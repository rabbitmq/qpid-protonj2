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
package org.apache.qpid.proton4j.engine;

import java.util.Map;

import org.apache.qpid.proton4j.amqp.Symbol;
import org.apache.qpid.proton4j.amqp.UnsignedLong;
import org.apache.qpid.proton4j.amqp.messaging.Source;
import org.apache.qpid.proton4j.amqp.messaging.Target;
import org.apache.qpid.proton4j.amqp.transport.ErrorCondition;
import org.apache.qpid.proton4j.amqp.transport.Role;
import org.apache.qpid.proton4j.engine.impl.ProtonSession;

/**
 * Base API for {@link Sender} and {@link Receiver} links.
 */
public interface Link<T extends Link<T>> {

    /**
     * Open this end of the link
     */
    public void open();

    /**
     * Close this end of the link
     */
    public void close();

    /**
     * Detach this end of the link.
     */
    public void detach();

    /**
     * Sets an application defined context value that will be carried with this {@link Connection} until
     * cleared by the application.
     *
     * @param context
     *      The context to associate with this connection.
     */
    void setContext(Object context);

    /**
     * @return the currently configured context that is associated with this {@link Connection}
     */
    Object getContext();

    /**
     * Sets or updates a named application defined context value that will be carried with this
     * {@link Connection} until cleared by the application.
     *
     * @param key
     *      The key used to identify the given context entry.
     * @param value
     *      The context value to assigned to the given key, or null to clear.
     */
    void setContextEntry(String key, Object value);

    /**
     * @return the context entry assigned to the given key or null of none assigned.
     */
    Object getContextEntry(String key);

    /**
     * @return the local link state
     */
    public LinkState getLocalState();

    /**
     * @return the local endpoint error, or null if there is none
     */
    public ErrorCondition getLocalCondition();

    /**
     * Sets the local {@link ErrorCondition} to be applied to a {@link Link} close.
     *
     * @param condition
     *      The error condition to convey to the remote peer on link close or detach.
     */
    public void setLocalCondition(ErrorCondition condition);

    /**
     * @return the {@link Role} that this end of the link is performing.
     */
    Role getRole();

    /**
     * @return the parent {@link Session} of the Receiver.
     */
    Session getSession();

    /**
     * @return the link name that is assigned to this link.
     */
    String getName();

    /**
     * Sets the {@link Source} to assign to the local end of this {@link Link}.
     *
     * @param source
     *      The {@link Source} that will be set on the local end of this link.
     */
    void setSource(Source source);

    /**
     * @return the {@link Source} for the local end of this link.
     */
    Source getSource();

    /**
     * Sets the {@link Target} to assign to the local end of this {@link Link}.
     *
     * @param source
     *      The {@link Target} that will be set on the local end of this link.
     */
    void setTarget(Target target);

    /**
     * @return the {@link Target} for the local end of this link.
     */
    Target getTarget();

    /**
     * Gets the local link properties.
     *
     * @see #setProperties(Map)
     */
    Map<Symbol, Object> getProperties();

    /**
     * Sets the local {@link Link} properties, to be conveyed to the peer via the Attach frame when
     * opening the local end of the link.
     *
     * Must be called during link setup, i.e. before calling the {@link #open()} method.
     *
     * @param properties
     *          the properties map to send, or null for none.
     */
    void setProperties(Map<Symbol, Object> properties);

    /**
     * Sets the local link offered capabilities, to be conveyed to the peer via the Attach frame
     * when attaching the link to the session.
     *
     * Must be called during link setup, i.e. before calling the {@link #open()} method.
     *
     * @param offeredCapabilities
     *          the offered capabilities array to send, or null for none.
     */
    public void setOfferedCapabilities(Symbol[] offeredCapabilities);

    /**
     * Gets the local link offered capabilities.
     *
     * @return the offered capabilities array, or null if none was set.
     *
     * @see #setOfferedCapabilities(Symbol[])
     */
    Symbol[] getOfferedCapabilities();

    /**
     * Sets the local link desired capabilities, to be conveyed to the peer via the Attach frame
     * when attaching the link to the session.
     *
     * Must be called during link setup, i.e. before calling the {@link #open()} method.
     *
     * @param desiredCapabilities
     *          the desired capabilities array to send, or null for none.
     */
    public void setDesiredCapabilities(Symbol[] desiredCapabilities);

    /**
     * Gets the local link desired capabilities.
     *
     * @return the desired capabilities array, or null if none was set.
     *
     * @see #setDesiredCapabilities(Symbol[])
     */
    Symbol[] getDesiredCapabilities();

    /**
     * Sets the local link max message size, to be conveyed to the peer via the Attach frame
     * when attaching the link to the session. Null or 0 means no limit.
     *
     * Must be called during link setup, i.e. before calling the {@link #open()} method.
     *
     * @param maxMessageSize
     *            the local max message size value, or null to clear. 0 also means no limit.
     */
    void setMaxMessageSize(UnsignedLong maxMessageSize);

    /**
     * Gets the local link max message size.
     *
     * @return the local max message size, or null if none was set. 0 also means no limit.
     *
     * @see #setMaxMessageSize(UnsignedLong)
     */
    UnsignedLong getMaxMessageSize();

    //----- View of the state of the link at the remote

    /**
     * @return the {@link Source} for the remote end of this link.
     */
    Source getRemoteSource();

    /**
     * @return the {@link Target} for the remote end of this link.
     */
    Target getRemoteTarget();

    /**
     * Gets the remote link offered capabilities, as conveyed from the peer via the Attach frame
     * when attaching the link to the session.
     *
     * @return the offered capabilities array conveyed by the peer, or null if there was none.
     */
    Symbol[] getRemoteOfferedCapabilities();

    /**
     * Gets the remote link desired capabilities, as conveyed from the peer via the Attach frame
     * when attaching the link to the session.
     *
     * @return the desired capabilities array conveyed by the peer, or null if there was none.
     */
    Symbol[] getRemoteDesiredCapabilities();

    /**
     * Gets the remote link properties, as conveyed from the peer via the Attach frame
     * when attaching the link to the session.
     *
     * @return the properties Map conveyed by the peer, or null if there was none.
     */
    Map<Symbol, Object> getRemoteProperties();

    /**
     * Gets the remote link max message size, as conveyed from the peer via the Attach frame
     * when attaching the link to the session.
     *
     * @return the remote max message size conveyed by the peer, or null if none was set. 0 also means no limit.
     */
    UnsignedLong getRemoteMaxMessageSize();

    /**
     * @return the remote link state (as last communicated)
     */
    public LinkState getRemoteState();

    /**
     * @return the remote endpoint error, or null if there is none
     */
    public ErrorCondition getRemoteCondition();

    //----- Remote events for AMQP Link resources

    /**
     * Sets a {@link EventHandler} for when an AMQP Begin frame is received from the remote peer for this
     * {@link Link} which would have been locally opened previously.
     *
     * Typically used by clients, servers rely on {@link ProtonSession#senderOpenEventHandler(EventHandler)} and
     * {@link ProtonSession#receiverOpenEventHandler(EventHandler)}.
     *
     * @param remoteOpenHandler
     *      The {@link EventHandler} to notify when this link is remotely opened.
     *
     * @return the link for chaining.
     */
    T openHandler(EventHandler<AsyncEvent<T>> remoteOpenHandler);

    /**
     * Sets a {@link EventHandler} for when an AMQP Detach frame is received from the remote peer for this
     * {@link Link} which would have been locally opened previously, the Detach from would have been marked
     * as not having been closed.
     *
     * @param remoteDetachHandler
     *      The {@link EventHandler} to notify when this link is remotely closed.
     *
     * @return the link for chaining.
     */
    T detachHandler(EventHandler<AsyncEvent<T>> remoteDetachHandler);

    /**
     * Sets a {@link EventHandler} for when an AMQP Detach frame is received from the remote peer for this
     * {@link Link} which would have been locally opened previously, the detach would have been marked as
     * having been closed.
     *
     * @param remoteCloseHandler
     *      The {@link EventHandler} to notify when this link is remotely closed.
     *
     * @return the link for chaining.
     */
    T closeHandler(EventHandler<AsyncEvent<T>> remoteCloseHandler);

}
