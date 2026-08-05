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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.apache.qpid.protonj2.codec.decoders.primitives.DoubleTypeDecoder;
import org.apache.qpid.protonj2.codec.encoders.primitives.DoubleTypeEncoder;
import org.junit.jupiter.api.Test;

public class DoubleTypeCodecTest extends CodecTestSupport {

    @Test
    public void testDecoderThrowsWhenAskedToReadWrongTypeAsThisType() throws Exception {
        testDecoderThrowsWhenAskedToReadWrongTypeAsThisType(false);
    }

    @Test
    public void testDecoderThrowsWhenAskedToReadWrongTypeAsThisTypeFS() throws Exception {
        testDecoderThrowsWhenAskedToReadWrongTypeAsThisType(true);
    }

    private void testDecoderThrowsWhenAskedToReadWrongTypeAsThisType(boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(EncodingCodes.UINT);
        buffer.writeByte(EncodingCodes.UINT);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);

            try {
                streamDecoder.readDouble(stream, streamDecoderState);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}

            try {
                streamDecoder.readDouble(stream, streamDecoderState, 0.0);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}
        } else {
            try {
                decoder.readDouble(buffer, decoderState);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}

            try {
                decoder.readDouble(buffer, decoderState, 0.0);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}
        }
    }

    @Test
    public void testReadPrimitiveTypeFromEncodingCode() throws IOException {
        testReadPrimitiveTypeFromEncodingCode(false);
    }

    @Test
    public void testReadPrimitiveTypeFromEncodingCodeFS() throws IOException {
        testReadPrimitiveTypeFromEncodingCode(true);
    }

    private void testReadPrimitiveTypeFromEncodingCode(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(EncodingCodes.DOUBLE);
        buffer.writeDouble(42.0);
        buffer.writeByte(EncodingCodes.DOUBLE);
        buffer.writeDouble(43.0);
        buffer.writeByte(EncodingCodes.NULL);
        buffer.writeByte(EncodingCodes.NULL);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);

            assertEquals(42.0, streamDecoder.readDouble(stream, streamDecoderState).shortValue(), 0.0);
            assertEquals(43.0, streamDecoder.readDouble(stream, streamDecoderState, 42.0), 0.0);
            assertNull(streamDecoder.readDouble(stream, streamDecoderState));
            assertEquals(43.0, streamDecoder.readDouble(stream, streamDecoderState, 43.0), 0.0);
        } else {
            assertEquals(42.0, decoder.readDouble(buffer, decoderState).shortValue(), 0.0);
            assertEquals(43.0, decoder.readDouble(buffer, decoderState, 42.0), 0.0);
            assertNull(decoder.readDouble(buffer, decoderState));
            assertEquals(43.0, decoder.readDouble(buffer, decoderState, 43.0), 0.0);
        }
    }

    @Test
    public void testGetTypeCode() {
        assertEquals(EncodingCodes.DOUBLE, (byte) new DoubleTypeDecoder().getTypeCode());
    }

    @Test
    public void testGetTypeClass() {
        assertEquals(Double.class, new DoubleTypeEncoder().getTypeClass());
        assertEquals(Double.class, new DoubleTypeDecoder().getTypeClass());
    }

    @Test
    public void testReadDoubleFromEncodingCode() throws IOException {
        testReadDoubleFromEncodingCode(false);
    }

    @Test
    public void testReadDoubleFromEncodingCodeFS() throws IOException {
        testReadDoubleFromEncodingCode(true);
    }

    private void testReadDoubleFromEncodingCode(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(EncodingCodes.DOUBLE);
        buffer.writeDouble(42);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            assertEquals(42, streamDecoder.readDouble(stream, streamDecoderState).intValue());
        } else {
            assertEquals(42, decoder.readDouble(buffer, decoderState).intValue());
        }
    }

    @Test
    public void testSkipValue() throws IOException {
        testSkipValue(false);
    }

    @Test
    public void testSkipValueFS() throws IOException {
        testSkipValue(true);
    }

    private void testSkipValue(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        for (int i = 0; i < 10; ++i) {
            encoder.writeDouble(buffer, encoderState, Double.MAX_VALUE);
            encoder.writeDouble(buffer, encoderState, 16.1);
        }

        double expected = 42;

        encoder.writeObject(buffer, encoderState, expected);

        final InputStream stream;
        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        for (int i = 0; i < 10; ++i) {
            if (fromStream) {
                StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
                assertEquals(Double.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(stream, streamDecoderState);
                typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
                assertEquals(Double.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(stream, streamDecoderState);
            } else {
                TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
                assertEquals(Double.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(buffer, decoderState);
                typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
                assertEquals(Double.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(buffer, decoderState);
            }
        }

        final Object result;
        if (fromStream) {
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertNotNull(result);
        assertTrue(result instanceof Double);

        Double value = (Double) result;
        assertEquals(expected, value.doubleValue(), 0.1f);
    }

    @Test
    public void testArrayOfObjects() throws IOException {
        testArrayOfObjects(false);
    }

    @Test
    public void testArrayOfObjectsFS() throws IOException {
        testArrayOfObjects(true);
    }

    private void testArrayOfObjects(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final int size = 10;

        Double[] source = new Double[size];
        for (int i = 0; i < size; ++i) {
            source[i] = Double.valueOf((char) i);
        }

        encoder.writeArray(buffer, encoderState, source);

        final Object result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertTrue(result.getClass().getComponentType().isPrimitive());

        double[] array = (double[]) result;
        assertEquals(size, array.length);

        for (int i = 0; i < size; ++i) {
            assertEquals(source[i], array[i]);
        }
    }

    @Test
    public void testZeroSizedArrayOfObjects() throws IOException {
        testZeroSizedArrayOfObjects(false);
    }

    @Test
    public void testZeroSizedArrayOfObjectsFS() throws IOException {
        testZeroSizedArrayOfObjects(true);
    }

    private void testZeroSizedArrayOfObjects(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Double[] source = new Double[0];

        encoder.writeArray(buffer, encoderState, source);

        final Object result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertTrue(result.getClass().getComponentType().isPrimitive());

        double[] array = (double[]) result;
        assertEquals(source.length, array.length);
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

        buffer.writeByte(EncodingCodes.DOUBLE);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(8, typeDecoder.readSize(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(8, typeDecoder.readSize(buffer, decoderState));
        }
    }

    @Test
    public void testEncodeAndDecodeArrayOfPrimitivesAsUnregisteredType() throws IOException {
        doTestEncodeAndDecodeArrayOfPrimitivesAsUnregisteredType(false);
    }

    @Test
    public void testEncodeAndDecodeArrayOfPrimitivesAsUnregisteredTypeFS() throws IOException {
        doTestEncodeAndDecodeArrayOfPrimitivesAsUnregisteredType(true);
    }

    private void doTestEncodeAndDecodeArrayOfPrimitivesAsUnregisteredType(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        double[] values = new double[] { 0.11, 0.22, 1.11, 1.22 };

        encoder.writeObject(buffer, encoderState, values);

        final Object result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertTrue(result.getClass().isArray());
        assertTrue(result.getClass().getComponentType().isPrimitive());

        double[] resultArray = (double[]) result;

        assertArrayEquals(values, resultArray);
    }

    @Test
    public void testDefaultsDecodeFailsForPrimitiveArrayIfCountIsToLargeArray32() throws Exception {
        testDefaultsDecodeFailsForPrimitiveArrayWhenCountIsToLarge(true, false);
    }

    @Test
    public void testDefaultsDecodeFailsForPrimitiveArrayIfCountIsToLargeArray32FromStream() throws Exception {
        testDefaultsDecodeFailsForPrimitiveArrayWhenCountIsToLarge(true, true);
    }

    @Test
    public void testDefaultsDecodeFailsForPrimitiveArrayIfCountIsToLargeArray8() throws Exception {
        testDefaultsDecodeFailsForPrimitiveArrayWhenCountIsToLarge(false, false);
    }

    @Test
    public void testDefaultsDecodeFailsForPrimitiveArrayIfCountIsToLargeArray8FromStream() throws Exception {
        testDefaultsDecodeFailsForPrimitiveArrayWhenCountIsToLarge(false, true);
    }

    private void testDefaultsDecodeFailsForPrimitiveArrayWhenCountIsToLarge(boolean array32, boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        streamDecoderState.setMaxArraySize(13);

        if (array32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(13);  // Size
            buffer.writeInt(15);  // Count
            buffer.writeByte(EncodingCodes.DOUBLE);
            buffer.writeLong((byte) 1);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 10);  // Size
            buffer.writeByte((byte) 14);  // Count
            buffer.writeByte(EncodingCodes.DOUBLE);
            buffer.writeLong((byte) 1);
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
}
