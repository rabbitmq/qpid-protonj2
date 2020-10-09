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
package org.apache.qpid.protonj2.codec.decoders.primitives;

import java.io.InputStream;
import java.util.UUID;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.apache.qpid.protonj2.codec.DecoderState;
import org.apache.qpid.protonj2.codec.EncodingCodes;
import org.apache.qpid.protonj2.codec.StreamDecoderState;
import org.apache.qpid.protonj2.codec.decoders.AbstractPrimitiveTypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.ProtonStreamUtils;

/**
 * Decoder of AMQP UUID values from a byte stream
 */
public final class UUIDTypeDecoder extends AbstractPrimitiveTypeDecoder<UUID> {

    private static final int BYTES = Long.BYTES * 2;

    @Override
    public Class<UUID> getTypeClass() {
        return UUID.class;
    }

    @Override
    public int getTypeCode() {
        return EncodingCodes.UUID & 0xff;
    }

    @Override
    public UUID readValue(ProtonBuffer buffer, DecoderState state) throws DecodeException {
        long msb = buffer.readLong();
        long lsb = buffer.readLong();

        return new UUID(msb, lsb);
    }

    @Override
    public UUID readValue(InputStream stream, StreamDecoderState state) throws DecodeException {
        long msb = ProtonStreamUtils.readLong(stream);
        long lsb = ProtonStreamUtils.readLong(stream);

        return new UUID(msb, lsb);
    }

    @Override
    public void skipValue(ProtonBuffer buffer, DecoderState state) throws DecodeException {
        buffer.skipBytes(BYTES);
    }

    @Override
    public void skipValue(InputStream stream, StreamDecoderState state) throws DecodeException {
        ProtonStreamUtils.skipBytes(stream, BYTES);
    }
}
