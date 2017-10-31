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
package org.apache.qpid.proton4j.codec.decoders.transport;

import java.io.IOException;

import org.apache.qpid.proton4j.amqp.Symbol;
import org.apache.qpid.proton4j.amqp.UnsignedByte;
import org.apache.qpid.proton4j.amqp.UnsignedLong;
import org.apache.qpid.proton4j.amqp.messaging.Source;
import org.apache.qpid.proton4j.amqp.messaging.Target;
import org.apache.qpid.proton4j.amqp.transport.Attach;
import org.apache.qpid.proton4j.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton4j.amqp.transport.Role;
import org.apache.qpid.proton4j.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton4j.buffer.ProtonBuffer;
import org.apache.qpid.proton4j.codec.DecoderState;
import org.apache.qpid.proton4j.codec.DescribedTypeDecoder;
import org.apache.qpid.proton4j.codec.TypeDecoder;
import org.apache.qpid.proton4j.codec.decoders.primitives.ListTypeDecoder;

/**
 * Decoder of AMQP Attach type values from a byte stream.
 */
public class AttachTypeDecoder implements DescribedTypeDecoder<Attach> {

    @Override
    public Class<Attach> getTypeClass() {
        return Attach.class;
    }

    @Override
    public UnsignedLong getDescriptorCode() {
        return Attach.DESCRIPTOR_CODE;
    }

    @Override
    public Symbol getDescriptorSymbol() {
        return Attach.DESCRIPTOR_SYMBOL;
    }

    @Override
    public Attach readValue(ProtonBuffer buffer, DecoderState state) throws IOException {

        TypeDecoder<?> decoder = state.getDecoder().readNextTypeDecoder(buffer, state);

        if (!(decoder instanceof ListTypeDecoder)) {
            throw new IOException("Expected List type indicator but got decoder for type: " + decoder.getTypeClass().getName());
        }

        ListTypeDecoder listDecoder = (ListTypeDecoder) decoder;
        Attach attach = new Attach();

        @SuppressWarnings("unused")
        int size = listDecoder.readSize(buffer);
        int count = listDecoder.readCount(buffer);

        for (int index = 0; index < count; ++index) {
            switch (index) {
                case 0:
                    attach.setName(state.getDecoder().readString(buffer, state));
                    break;
                case 1:
                    attach.setHandle(state.getDecoder().readUnsignedInteger(buffer, state));
                    break;
                case 2:
                    Boolean role = state.getDecoder().readBoolean(buffer, state);
                    attach.setRole(Boolean.TRUE.equals(role) ? Role.RECEIVER : Role.SENDER);
                    break;
                case 3:
                    UnsignedByte sndSettleMode = state.getDecoder().readUnsignedByte(buffer, state);
                    attach.setSndSettleMode(sndSettleMode == null ? SenderSettleMode.MIXED : SenderSettleMode.values()[sndSettleMode.intValue()]);
                    break;
                case 4:
                    UnsignedByte rcvSettleMode = state.getDecoder().readUnsignedByte(buffer, state);
                    attach.setRcvSettleMode(rcvSettleMode == null ? ReceiverSettleMode.FIRST : ReceiverSettleMode.values()[rcvSettleMode.intValue()]);
                    break;
                case 5:
                    attach.setSource(state.getDecoder().readObject(buffer, state, Source.class));
                    break;
                case 6:
                    attach.setTarget(state.getDecoder().readObject(buffer, state, Target.class));
                    break;
                case 7:
                    attach.setUnsettled(state.getDecoder().readMap(buffer, state));
                    break;
                case 8:
                    attach.setIncompleteUnsettled(Boolean.TRUE.equals(state.getDecoder().readBoolean(buffer, state)));
                    break;
                case 9:
                    attach.setInitialDeliveryCount(state.getDecoder().readUnsignedInteger(buffer, state));
                    break;
                case 10:
                    attach.setMaxMessageSize(state.getDecoder().readUnsignedLong(buffer, state));
                    break;
                case 11:
                    attach.setOfferedCapabilities(state.getDecoder().readMultiple(buffer, state, Symbol.class));
                    break;
                case 12:
                    attach.setDesiredCapabilities(state.getDecoder().readMultiple(buffer, state, Symbol.class));
                    break;
                case 13:
                    attach.setProperties(state.getDecoder().readMap(buffer, state));
                    break;
                default:
                    throw new IllegalStateException("To many entries in Attach encoding");
            }
        }

        return attach;
    }

    @Override
    public void skipValue(ProtonBuffer buffer, DecoderState state) throws IOException {
        TypeDecoder<?> decoder = state.getDecoder().readNextTypeDecoder(buffer, state);

        if (!(decoder instanceof ListTypeDecoder)) {
            throw new IOException("Expected List type indicator but got decoder for type: " + decoder.getTypeClass().getName());
        }

        decoder.skipValue(buffer, state);
    }
}
