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
package org.messaginghub.amqperative.impl;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.engine.Engine;
import org.apache.qpid.protonj2.engine.exceptions.EngineStateException;
import org.messaginghub.amqperative.exceptions.ClientFailedException;
import org.messaginghub.amqperative.transport.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for events from a connection linked Transport and informs the connection
 * of the events.
 */
public class ClientTransportListener implements TransportListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientTransportListener.class);

    private final ClientConnection connection;
    private final Engine engine;

    public ClientTransportListener(ClientConnection connection) {
        this.connection = connection;
        this.engine = connection.getEngine();
    }

    @Override
    public void onData(ProtonBuffer incoming) {
        try {
            do {
                engine.ingest(incoming);
            } while (incoming.isReadable() && engine.isWritable());
            // TODO - How do we handle case of not all data read ?
        } catch (EngineStateException e) {
            LOG.warn("Caught problem during incoming data processing: {}", e.getMessage(), e);
            engine.engineFailed(ClientExceptionSupport.createOrPassthroughFatal(e));
        }
    }

    @Override
    public void onTransportClosed() {
        if (!connection.getScheduler().isShutdown()) {
            connection.getScheduler().execute(() -> {
                LOG.debug("Transport connection remotely closed");
                engine.engineFailed(new ClientFailedException("Transport connection remotely closed."));
            });
        }
    }

    @Override
    public void onTransportError(Throwable error) {
        if (!connection.getScheduler().isShutdown()) {
            connection.getScheduler().execute(() -> {
                LOG.info("Transport failed: {}", error.getMessage());
                engine.engineFailed(ClientExceptionSupport.createOrPassthroughFatal(error));
            });
        }
    }
}
