/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.protonj2.client.impl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.ErrorCondition;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Session;
import org.apache.qpid.protonj2.client.Source;
import org.apache.qpid.protonj2.client.Target;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.apache.qpid.protonj2.client.exceptions.ClientIllegalStateException;
import org.apache.qpid.protonj2.client.exceptions.ClientOperationTimedOutException;
import org.apache.qpid.protonj2.client.exceptions.ClientResourceClosedException;
import org.apache.qpid.protonj2.client.futures.ClientFuture;
import org.apache.qpid.protonj2.client.util.FifoDeliveryQueue;
import org.apache.qpid.protonj2.engine.IncomingDelivery;
import org.apache.qpid.protonj2.types.messaging.Outcome;
import org.apache.qpid.protonj2.types.messaging.Released;
import org.apache.qpid.protonj2.types.transport.DeliveryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientReceiver implements Receiver {

    private static final Logger LOG = LoggerFactory.getLogger(ClientReceiver.class);

    private static final AtomicIntegerFieldUpdater<ClientReceiver> CLOSED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ClientReceiver.class, "closed");

    private final ClientFuture<Receiver> openFuture;
    private final ClientFuture<Receiver> closeFuture;
    private ClientFuture<Receiver> drainingFuture;

    private final ReceiverOptions options;
    private final ClientSession session;
    private final org.apache.qpid.protonj2.engine.Receiver protonReceiver;
    private final ScheduledExecutorService executor;
    private final String receiverId;
    private final FifoDeliveryQueue messageQueue;
    private volatile int closed;
    private boolean remoteRejectedOpen;
    private ClientException failureCause;

    private volatile Source remoteSource;
    private volatile Target remoteTarget;

    public ClientReceiver(ClientSession session, ReceiverOptions options, String receiverId, org.apache.qpid.protonj2.engine.Receiver receiver) {
        this.options = options;
        this.session = session;
        this.receiverId = receiverId;
        this.executor = session.getScheduler();
        this.openFuture = session.getFutureFactory().createFuture();
        this.closeFuture = session.getFutureFactory().createFuture();
        this.protonReceiver = receiver;

        // Ensure that the receiver can provide a link back to this object.
        protonReceiver.setLinkedResource(this);

        if (options.creditWindow() > 0) {
            protonReceiver.addCredit(options.creditWindow());
        }

        messageQueue = new FifoDeliveryQueue(options.creditWindow());
        messageQueue.start();
    }

    @Override
    public String address() throws ClientException {
        if (isDynamic()) {
            waitForOpenToComplete();
            return protonReceiver.getRemoteSource().getAddress();
        } else {
            return protonReceiver.getSource() != null ? protonReceiver.getSource().getAddress() : null;
        }
    }

    @Override
    public Source source() throws ClientException {
        waitForOpenToComplete();
        return remoteSource;
    }

    @Override
    public Target target() throws ClientException {
        waitForOpenToComplete();
        return remoteTarget;
    }

    @Override
    public Client client() {
        return session.client();
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Future<Receiver> openFuture() {
        return openFuture;
    }

    @Override
    public Delivery receive() throws ClientException {
        return receive(-1);
    }

    @Override
    public Delivery receive(long timeout) throws ClientException {
        checkClosed();
        try {
            ClientDelivery delivery = messageQueue.dequeue(timeout);
            if (delivery != null && options.autoAccept()) {
                delivery.disposition(org.apache.qpid.protonj2.client.DeliveryState.accepted(), options.autoSettle());
            } else {
                asyncReplenishCreditIfNeeded();
            }

            return delivery;
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new ClientException("Receive wait interrupted", e);
        }
    }

    @Override
    public Delivery tryReceive() throws ClientException {
        checkClosed();

        Delivery delivery = messageQueue.dequeueNoWait();
        if (delivery != null && options.autoAccept()) {
            delivery.disposition(org.apache.qpid.protonj2.client.DeliveryState.accepted(), options.autoSettle());
        } else {
            asyncReplenishCreditIfNeeded();
        }

        return delivery;
    }

    private void asyncReplenishCreditIfNeeded() {
        int creditWindow = options.creditWindow();
        if (creditWindow > 0) {
            executor.execute(() -> replenishCreditIfNeeded());
        }
    }

    private void replenishCreditIfNeeded() {
        int creditWindow = options.creditWindow();
        if (creditWindow > 0) {
            int currentCredit = protonReceiver.getCredit();
            if (currentCredit <= creditWindow * 0.5) {
                int potentialPrefetch = currentCredit + messageQueue.size();

                if (potentialPrefetch <= creditWindow * 0.7) {
                    int additionalCredit = creditWindow - potentialPrefetch;

                    LOG.trace("Consumer granting additional credit: {}", additionalCredit);
                    try {
                        protonReceiver.addCredit(additionalCredit);
                    } catch (Exception ex) {
                        LOG.debug("Error caught during credit top-up", ex);
                    }
                }
            }
        }
    }

    @Override
    public ClientFuture<Receiver> close() {
        return doCloseOrDetach(true, null);
    }

    @Override
    public ClientFuture<Receiver> close(ErrorCondition error) {
        Objects.requireNonNull(error, "Error Condition cannot be null");

        return doCloseOrDetach(true, error);
    }

    @Override
    public ClientFuture<Receiver> detach() {
        return doCloseOrDetach(false, null);
    }

    @Override
    public ClientFuture<Receiver> detach(ErrorCondition error) {
        Objects.requireNonNull(error, "Error Condition cannot be null");

        return doCloseOrDetach(false, error);
    }

    private ClientFuture<Receiver> doCloseOrDetach(boolean close, ErrorCondition error) {
        if (CLOSED_UPDATER.compareAndSet(this, 0, 1)) {
            executor.execute(() -> {
                if (protonReceiver.isLocallyOpen()) {
                    try {
                        messageQueue.stop();  // Ensure blocked receivers are all unblocked.

                        protonReceiver.setCondition(ClientErrorCondition.asProtonErrorCondition(error));

                        if (close) {
                            protonReceiver.close();
                        } else {
                            protonReceiver.detach();
                        }
                    } catch (Throwable ignore) {
                        closeFuture.complete(this);
                    }
                }
            });
        }
        return closeFuture;
    }

    @Override
    public long getQueueSize() {
        return messageQueue.size();
    }

    @Override
    public Receiver onMessage(Consumer<Delivery> handler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Receiver onMessage(Consumer<Delivery> handler, ExecutorService executor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Receiver addCredit(int credits) throws ClientException {
        checkClosed();
        ClientFuture<Receiver> creditAdded = session.getFutureFactory().createFuture();

        executor.execute(() -> {
            checkClosed(creditAdded);

            if (options.creditWindow() != 0) {
                creditAdded.failed(new ClientIllegalStateException("Cannot add credit when a credit window has been configured"));
            } else if (protonReceiver.isDraining()) {
                creditAdded.failed(new ClientIllegalStateException("Cannot add credit while a drain is pending"));
            } else {
                try {
                    protonReceiver.addCredit(credits);
                    creditAdded.complete(this);
                } catch (Exception ex) {
                    creditAdded.failed(ClientExceptionSupport.createNonFatalOrPassthrough(ex));
                }
            }
        });

        return session.request(creditAdded, options.requestTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Future<Receiver> drain() throws ClientException {
        checkClosed();
        final ClientFuture<Receiver> drainComplete = session.getFutureFactory().createFuture();

        executor.execute(() -> {
            checkClosed(drainComplete);

            if (protonReceiver.isDraining()) {
                drainComplete.failed(new ClientException("Already draining"));
                return;
            }

            if (protonReceiver.getCredit() == 0) {
                drainComplete.complete(this);
                return;
            }

            try {
                if (protonReceiver.drain()) {
                    drainingFuture = drainComplete;
                } else {
                    drainComplete.complete(this);
                }
            } catch (Exception ex) {
                drainComplete.failed(ClientExceptionSupport.createNonFatalOrPassthrough(ex));
            }
        });

        return drainComplete;
    }

    @Override
    public Map<String, Object> properties() throws ClientException {
        waitForOpenToComplete();
        return ClientConversionSupport.toStringKeyedMap(protonReceiver.getRemoteProperties());
    }

    @Override
    public String[] offeredCapabilities() throws ClientException {
        waitForOpenToComplete();
        return ClientConversionSupport.toStringArray(protonReceiver.getRemoteOfferedCapabilities());
    }

    @Override
    public String[] desiredCapabilities() throws ClientException {
        waitForOpenToComplete();
        return ClientConversionSupport.toStringArray(protonReceiver.getRemoteDesiredCapabilities());
    }

    //----- Internal API

    void disposition(IncomingDelivery delivery, DeliveryState state, boolean settled) throws ClientException {
        checkClosed();
        executor.execute(() -> {
            if (session.getTransactionContext().isInTransaction()) {
                delivery.disposition(session.getTransactionContext().enlistAcknowledgeInCurrentTransaction(this, (Outcome) state), true);
            } else {
                delivery.disposition(state, settled);
            }

            replenishCreditIfNeeded();
        });
    }

    ClientReceiver open() {
        protonReceiver.localOpenHandler(receiver -> handleLocalOpen(receiver))
                      .localCloseHandler(receiver -> handleLocalCloseOrDetach(receiver))
                      .localDetachHandler(receiver -> handleLocalCloseOrDetach(receiver))
                      .openHandler(receiver -> handleRemoteOpen(receiver))
                      .closeHandler(receiver -> handleRemoteCloseOrDetach(receiver))
                      .detachHandler(receiver -> handleRemoteCloseOrDetach(receiver))
                      .deliveryStateUpdatedHandler(delivery -> handleDeliveryRemotelyUpdated(delivery))
                      .deliveryReadHandler(delivery -> handleDeliveryReceived(delivery))
                      .creditStateUpdateHandler(receiver -> handleReceiverCreditUpdated(receiver))
                      .engineShutdownHandler(engine -> immediateLinkShutdown())
                      .open();

        return this;
    }

    void setFailureCause(ClientException failureCause) {
        this.failureCause = failureCause;
    }

    ClientException getFailureCause() {
        if (failureCause == null) {
            return session.getFailureCause();
        } else {
            return failureCause;
        }
    }

    String getId() {
        return receiverId;
    }

    boolean isClosed() {
        return closed > 0;
    }

    boolean isDynamic() {
        return protonReceiver.getSource() != null && protonReceiver.getSource().isDynamic();
    }

    //----- Handlers for proton receiver events

    private void handleLocalOpen(org.apache.qpid.protonj2.engine.Receiver receiver) {
        if (options.openTimeout() > 0) {
            executor.schedule(() -> {
                if (!openFuture.isDone()) {
                    if (failureCause == null) {
                        failureCause = new ClientOperationTimedOutException("Receiver open timed out waiting for remote to respond");
                    }

                    if (protonReceiver.isLocallyClosed()) {
                        // We didn't hear back from open and link was since closed so just fail
                        // the close as we don't want to doubly wait for something that can't come.
                        immediateLinkShutdown();
                    } else {
                        try {
                            receiver.close();
                        } catch (Throwable error) {
                            // Connection is responding to all engine failed errors
                        }
                    }
                }
            }, options.openTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    private void handleLocalCloseOrDetach(org.apache.qpid.protonj2.engine.Receiver receiver) {
        if (failureCause == null) {
            failureCause = session.getFailureCause();
        }

        // If not yet remotely closed we only wait for a remote close if the session isn't
        // already failed and we have successfully opened the receiver without a timeout.
        if (!session.isClosed() && failureCause == null && receiver.isRemotelyOpen()) {
            final long timeout = options.closeTimeout();

            if (timeout > 0) {
                session.scheduleRequestTimeout(closeFuture, timeout, () ->
                    new ClientOperationTimedOutException("receiver close timed out waiting for remote to respond"));
            }
        } else {
            immediateLinkShutdown();
        }
    }

    private void handleRemoteOpen(org.apache.qpid.protonj2.engine.Receiver receiver) {
        // Check for deferred close pending and hold completion if so
        if (receiver.getRemoteSource() != null) {
            remoteSource = new RemoteSource(receiver.getRemoteSource());

            if (receiver.getRemoteTarget() != null) {
                remoteTarget = new RemoteTarget(receiver.getRemoteTarget());
            }

            replenishCreditIfNeeded();

            openFuture.complete(this);
            LOG.trace("Receiver opened successfully: {}", receiverId);
        } else {
            LOG.debug("Receiver opened but remote signalled close is pending: {}", receiverId);
            remoteRejectedOpen = true;
        }
    }

    private void handleRemoteCloseOrDetach(org.apache.qpid.protonj2.engine.Receiver receiver) {
        if (receiver.isLocallyOpen()) {
            final ClientException error;

            if (receiver.getRemoteCondition() != null) {
                error = ClientErrorSupport.convertToNonFatalException(receiver.getRemoteCondition());
            } else {
                error = new ClientResourceClosedException("Receiver remotely closed without explanation");
            }

            if (failureCause == null) {
                failureCause = error;
            }

            try {
                if (receiver.isRemotelyDetached()) {
                    receiver.detach();
                } else {
                    receiver.close();
                }
            } catch (Throwable ignore) {
                LOG.trace("Error ignored from call to close receiver after remote close.", ignore);
            }
        } else {
            immediateLinkShutdown();
        }
    }

    private void handleDeliveryReceived(IncomingDelivery delivery) {
        LOG.trace("Delivery data was received: {}", delivery);

        if (delivery.getDefaultDeliveryState() != null) {
            delivery.setDefaultDeliveryState(Released.getInstance());
        }

        if (delivery.isAborted()) {
            delivery.settle();
            replenishCreditIfNeeded();
        } else if (!delivery.isPartial()) {
            LOG.trace("{} has incoming Message(s).", this);
            messageQueue.enqueue(new ClientDelivery(this, delivery));
        }
    }

    private void handleDeliveryRemotelyUpdated(IncomingDelivery delivery) {
        LOG.trace("Delivery was updated: {}", delivery);
        // TODO - event or other reaction
    }

    private void handleReceiverCreditUpdated(org.apache.qpid.protonj2.engine.Receiver receiver) {
        LOG.trace("Receiver credit update by remote: {}", receiver);

        if (drainingFuture != null) {
            if (receiver.getCredit() == 0) {
                drainingFuture.complete(this);
            }
        }
    }

    //----- Private implementation details

    private void waitForOpenToComplete() throws ClientException {
        if (!openFuture.isComplete() || openFuture.isFailed()) {
            try {
                openFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Thread.interrupted();
                if (failureCause != null) {
                    throw failureCause;
                } else {
                    throw ClientExceptionSupport.createNonFatalOrPassthrough(e.getCause());
                }
            }
        }
    }

    private void checkClosed(ClientFuture<?> request) {
        if (isClosed()) {
            ClientException error = null;

            if (getFailureCause() == null) {
                error = new ClientIllegalStateException("The Receiver is closed");
            } else {
                error = new ClientIllegalStateException("The Receiver was closed due to an unrecoverable error.");
                error.initCause(getFailureCause());
            }

            request.failed(error);
        }
    }

    private void checkClosed() throws ClientException {
        if (isClosed()) {
            ClientException error = null;

            if (getFailureCause() == null) {
                error = new ClientIllegalStateException("The Receiver is closed");
            } else {
                error = new ClientIllegalStateException("The Receiver was closed due to an unrecoverable error.");
                error.initCause(getFailureCause());
            }

            throw error;
        }
    }

    private void immediateLinkShutdown() {
        CLOSED_UPDATER.lazySet(this, 1);
        if (failureCause == null) {
            if (session.getFailureCause() != null) {
                failureCause = session.getFailureCause();
            } else if (session.getEngine().failureCause() != null) {
                failureCause = ClientExceptionSupport.createOrPassthroughFatal(session.getEngine().failureCause());
            }
        }

        try {
            if (protonReceiver.isRemotelyDetached()) {
                protonReceiver.detach();
            } else {
                protonReceiver.close();
            }
        } catch (Throwable ignore) {
        }

        if (failureCause != null) {
            openFuture.failed(failureCause);
            // Session is in failed state so an error from receiver close won't help user or the
            // remote closed the receiver with an error by omitting the inbound source
            if (remoteRejectedOpen || session.getFailureCause() != null) {
                closeFuture.complete(this);
            } else {
                closeFuture.failed(failureCause);
            }
        } else {
            openFuture.complete(this);
            closeFuture.complete(this);
        }
    }
}
