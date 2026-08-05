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
package org.apache.qpid.protonj2.codec.primitives;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.buffer.ProtonBufferInputStream;
import org.apache.qpid.protonj2.codec.CodecTestSupport;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.apache.qpid.protonj2.codec.EncodingCodes;
import org.apache.qpid.protonj2.codec.StreamTypeDecoder;
import org.apache.qpid.protonj2.codec.TypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.PrimitiveArrayTypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.primitives.NullTypeDecoder;
import org.apache.qpid.protonj2.codec.encoders.primitives.NullTypeEncoder;
import org.junit.jupiter.api.Test;

public class NullTypeCodecTest extends CodecTestSupport {

    @Test
    public void testGetTypeCode() {
        assertEquals(EncodingCodes.NULL, new NullTypeDecoder().getTypeCode());
    }

    @Test
    public void testGetTypeClass() {
        assertEquals(Void.class, new NullTypeEncoder().getTypeClass());
        assertEquals(Void.class, new NullTypeDecoder().getTypeClass());
    }

    @Test
    public void testWriteOfArrayThrowsException() throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate(1).implicitGrowthLimit(1);

        try {
            new NullTypeEncoder().writeArray(buffer, encoderState, new Object[1]);
            fail("Null encoder cannot write array types");
        } catch (IllegalArgumentException iae) {}
    }

    @Test
    public void testWriteRawOfArrayThrowsException() throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate(1).implicitGrowthLimit(1);

        try {
            new NullTypeEncoder().writeRawArray(buffer, encoderState, new Object[1]);
            fail("Null encoder cannot write array types");
        } catch (IllegalArgumentException iae) {}
    }

    @Test
    public void testReadNullDoesNotTouchBuffer() throws IOException {
        testReadNullDoesNotTouchBuffer(false);
    }

    @Test
    public void testReadNullDoesNotTouchBufferFS() throws IOException {
        testReadNullDoesNotTouchBuffer(true);
    }

    private void testReadNullDoesNotTouchBuffer(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate(1).implicitGrowthLimit(1);

        buffer.writeByte(EncodingCodes.NULL);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            assertNull(streamDecoder.readObject(stream, streamDecoderState));
        } else {
            assertNull(decoder.readObject(buffer, decoderState));
        }
    }

    @Test
    public void testSkipNullDoesNotTouchBuffer() throws IOException {
        doTestSkipNullDoesNotTouchBuffer(false);
    }

    @Test
    public void testSkipNullDoesNotTouchStream() throws IOException {
        doTestSkipNullDoesNotTouchBuffer(true);
    }

    private void doTestSkipNullDoesNotTouchBuffer(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(EncodingCodes.NULL);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(Void.class, typeDecoder.getTypeClass());
            int index = buffer.getReadOffset();
            typeDecoder.skipValue(stream, streamDecoderState);
            assertEquals(index, buffer.getReadOffset());
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(Void.class, typeDecoder.getTypeClass());
            int index = buffer.getReadOffset();
            typeDecoder.skipValue(buffer, decoderState);
            assertEquals(index, buffer.getReadOffset());
        }
    }

    @Test
    public void testReadSeizeFromEncoding() throws IOException {
        doTestReadSeizeFromEncoding(false);
    }

    @Test
    public void testReadSeizeFromEncodingInStream() throws IOException {
        doTestReadSeizeFromEncoding(true);
    }

    private void doTestReadSeizeFromEncoding(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(EncodingCodes.NULL);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertTrue(typeDecoder.isNull());
            assertEquals(0, typeDecoder.readSize(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertTrue(typeDecoder.isNull());
            assertEquals(0, typeDecoder.readSize(buffer, decoderState));
        }
    }

    @Test
    public void testDefaultsDecodeFailsForAnyNonZeroSizedNullArray32() throws Exception {
        testDefaultsDecodeFailsForAnyNonZeroSizedNullArray(EncodingCodes.ARRAY32, false);
    }

    @Test
    public void testDefaultsDecodeFailsForAnyNonZeroSizedNullArray32FromStream() throws Exception {
        testDefaultsDecodeFailsForAnyNonZeroSizedNullArray(EncodingCodes.ARRAY32, true);
    }

    @Test
    public void testDefaultsDecodeFailsForAnyNonZeroSizedNullArray8() throws Exception {
        testDefaultsDecodeFailsForAnyNonZeroSizedNullArray(EncodingCodes.ARRAY8, false);
    }

    @Test
    public void testDefaultsDecodeFailsForAnyNonZeroSizedNullArray8FromStream() throws Exception {
        testDefaultsDecodeFailsForAnyNonZeroSizedNullArray(EncodingCodes.ARRAY8, true);
    }

    private void testDefaultsDecodeFailsForAnyNonZeroSizedNullArray(byte encodingCode, boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        if (encodingCode == EncodingCodes.ARRAY32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(5);  // Size
            buffer.writeInt(1);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 2);  // Size
            buffer.writeByte((byte) 1);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> arrayDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> arrayDecoder.readValue(buffer, decoderState));
        }
    }

    @Test
    public void testDecodeWorksForInConfiguredLimitsNullArray32() throws Exception {
        testDecodeWorksForInConfiguredLimitsNullArray(EncodingCodes.ARRAY32, false);
    }

    @Test
    public void testDecodeWorksForInConfiguredLimitsNullArray32FromStream() throws Exception {
        testDecodeWorksForInConfiguredLimitsNullArray(EncodingCodes.ARRAY32, true);
    }

    @Test
    public void testDecodeWorksForInConfiguredLimitsNullArray8() throws Exception {
        testDecodeWorksForInConfiguredLimitsNullArray(EncodingCodes.ARRAY8, false);
    }

    @Test
    public void testDecodeWorksForInConfiguredLimitsNullArray8FromStream() throws Exception {
        testDecodeWorksForInConfiguredLimitsNullArray(EncodingCodes.ARRAY8, true);
    }

    private void testDecodeWorksForInConfiguredLimitsNullArray(byte encodingCode ,boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        decoderState.setMaxZeroWidthArrayElements(20);
        streamDecoderState.setMaxZeroWidthArrayElements(20);

        if (encodingCode == EncodingCodes.ARRAY32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(5);  // Size
            buffer.writeInt(10);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 2);  // Size
            buffer.writeByte((byte) 10);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertDoesNotThrow(() -> arrayDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertDoesNotThrow(() -> arrayDecoder.readValue(buffer, decoderState));
        }
    }

    @Test
    public void testDecodeFailsForToLargeNullArray32() throws Exception {
        testDecodeFailsForToLargeForConfigurationNullArray(EncodingCodes.ARRAY32, false);
    }

    @Test
    public void testDecodeFailsForToLargeNullArray32FromStream() throws Exception {
        testDecodeFailsForToLargeForConfigurationNullArray(EncodingCodes.ARRAY32, true);
    }

    @Test
    public void testDecodeFailsForToLargeNullArray8() throws Exception {
        testDecodeFailsForToLargeForConfigurationNullArray(EncodingCodes.ARRAY8, false);
    }

    @Test
    public void testDecodeFailsForToLargeNullArray8FromStream() throws Exception {
        testDecodeFailsForToLargeForConfigurationNullArray(EncodingCodes.ARRAY8, true);
    }

    private void testDecodeFailsForToLargeForConfigurationNullArray(byte encodingCode ,boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        decoderState.setMaxZeroWidthArrayElements(9);
        streamDecoderState.setMaxZeroWidthArrayElements(9);

        if (encodingCode == EncodingCodes.ARRAY32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(5);  // Size
            buffer.writeInt(10);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 2);  // Size
            buffer.writeByte((byte) 10);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> arrayDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
            PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> arrayDecoder.readValue(buffer, decoderState));
        }
    }

    @Test
    public void testDecodeForArrayWithCountToLargeFailsArray32() throws Exception {
        doTestDecodeForArrayWithCountToLargeFails(EncodingCodes.ARRAY32);
    }

    @Test
    public void testDecodeForArrayWithCountToLargeFailsArray8() throws Exception {
        doTestDecodeForArrayWithCountToLargeFails(EncodingCodes.ARRAY8);
    }

    private void doTestDecodeForArrayWithCountToLargeFails(byte encodingCode) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        decoderState.setMaxZeroWidthArrayElements(20);
        streamDecoderState.setMaxZeroWidthArrayElements(20);

        if (encodingCode == EncodingCodes.ARRAY32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(5);  // Size
            buffer.writeInt(Integer.MAX_VALUE);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 2);  // Size
            buffer.writeByte(Byte.MAX_VALUE);  // Count
            buffer.writeByte(EncodingCodes.NULL);
        }

        TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
        assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
        PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
        assertThrows(DecodeException.class, () -> arrayDecoder.readValue(buffer, decoderState));
    }
}
