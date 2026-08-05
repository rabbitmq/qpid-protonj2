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
package org.apache.qpid.protonj2.codec.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.buffer.ProtonBufferInputStream;
import org.apache.qpid.protonj2.codec.CodecTestSupport;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.apache.qpid.protonj2.codec.EncodingCodes;
import org.apache.qpid.protonj2.codec.StreamTypeDecoder;
import org.apache.qpid.protonj2.codec.TypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.ProtonDecoderFactory;
import org.apache.qpid.protonj2.codec.decoders.ProtonStreamDecoderFactory;
import org.apache.qpid.protonj2.codec.decoders.security.SaslMechanismsTypeDecoder;
import org.apache.qpid.protonj2.codec.encoders.ProtonEncoderFactory;
import org.apache.qpid.protonj2.codec.encoders.security.SaslMechanismsTypeEncoder;
import org.apache.qpid.protonj2.types.Symbol;
import org.apache.qpid.protonj2.types.security.SaslMechanisms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SaslMechanismsTypeCodecTest extends CodecTestSupport {

    @Override
    @BeforeEach
    public void setUp() {
        decoder = ProtonDecoderFactory.createSasl();
        decoderState = decoder.newDecoderState();

        encoder = ProtonEncoderFactory.createSasl();
        encoderState = encoder.newEncoderState();

        streamDecoder = ProtonStreamDecoderFactory.createSasl();
        streamDecoderState = streamDecoder.newDecoderState();
    }

    @Test
    public void testTypeClassReturnsCorrectType() throws IOException {
        assertEquals(SaslMechanisms.class, new SaslMechanismsTypeDecoder().getTypeClass());
        assertEquals(SaslMechanisms.class, new SaslMechanismsTypeEncoder().getTypeClass());
    }

    @Test
    public void testDescriptors() throws Exception {
        SaslMechanismsTypeDecoder decoder = new SaslMechanismsTypeDecoder();
        SaslMechanismsTypeEncoder encoder = new SaslMechanismsTypeEncoder();

        assertEquals(SaslMechanisms.DESCRIPTOR_CODE, decoder.getDescriptorCode());
        assertEquals(SaslMechanisms.DESCRIPTOR_CODE, encoder.getDescriptorCode());
        assertEquals(SaslMechanisms.DESCRIPTOR_SYMBOL, decoder.getDescriptorSymbol());
        assertEquals(SaslMechanisms.DESCRIPTOR_SYMBOL, encoder.getDescriptorSymbol());
    }

    @Test
    public void testEncodeDecodeType() throws Exception {
        doTestEncodeDecodeType(false);
    }

    @Test
    public void testEncodeDecodeTypeFromStream() throws Exception {
        doTestEncodeDecodeType(true);
    }

    private void doTestEncodeDecodeType(boolean fromStream) throws Exception {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Symbol[] mechanisms = new Symbol[] { Symbol.getSASLSymbol("ANONYMOUS"), Symbol.getSASLSymbol("EXTERNAL") };

        SaslMechanisms input = new SaslMechanisms();
        input.setSaslServerMechanisms(mechanisms);

        encoder.writeObject(buffer, encoderState, input);

        final SaslMechanisms result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = (SaslMechanisms) streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = (SaslMechanisms) decoder.readObject(buffer, decoderState);
        }

        assertArrayEquals(mechanisms, result.getSaslServerMechanisms());
    }

    @Test
    public void testSkipValue() throws IOException {
        doTestSkipValue(false);
    }

    @Test
    public void testSkipValueFromStream() throws IOException {
        doTestSkipValue(true);
    }

    private void doTestSkipValue(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        SaslMechanisms mechanisms = new SaslMechanisms();

        mechanisms.setSaslServerMechanisms(Symbol.getSASLSymbol("ANONYMOUS"));

        for (int i = 0; i < 10; ++i) {
            encoder.writeObject(buffer, encoderState, mechanisms);
        }

        mechanisms.setSaslServerMechanisms(Symbol.getSASLSymbol("ANONYMOUS"), Symbol.getSASLSymbol("EXTERNAL"));

        encoder.writeObject(buffer, encoderState, mechanisms);

        final InputStream stream;
        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        for (int i = 0; i < 10; ++i) {
            if (fromStream) {
                StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
                assertEquals(SaslMechanisms.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(stream, streamDecoderState);
            } else {
                TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
                assertEquals(SaslMechanisms.class, typeDecoder.getTypeClass());
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
        assertTrue(result instanceof SaslMechanisms);

        SaslMechanisms value = (SaslMechanisms) result;
        assertArrayEquals(new Symbol[] {Symbol.getSASLSymbol("ANONYMOUS"), Symbol.getSASLSymbol("EXTERNAL")}, value.getSaslServerMechanisms());

        final Symbol[] decodedMechanisms = value.getSaslServerMechanisms();

        for (Symbol decoded : decodedMechanisms) {
            assertSame(decoded, Symbol.getSASLSymbol(decoded.toString()));
        }
    }

    @Test
    public void testSkipValueWithInvalidMap32Type() throws IOException {
        doTestSkipValueWithInvalidMapType(EncodingCodes.MAP32, false);
    }

    @Test
    public void testSkipValueWithInvalidMap8Type() throws IOException {
        doTestSkipValueWithInvalidMapType(EncodingCodes.MAP8, false);
    }

    @Test
    public void testSkipValueWithInvalidMap32TypeFromStream() throws IOException {
        doTestSkipValueWithInvalidMapType(EncodingCodes.MAP32, true);
    }

    @Test
    public void testSkipValueWithInvalidMap8TypeFromStream() throws IOException {
        doTestSkipValueWithInvalidMapType(EncodingCodes.MAP8, true);
    }

    private void doTestSkipValueWithInvalidMapType(byte mapType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        if (mapType == EncodingCodes.MAP32) {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt((byte) 0);  // Size
            buffer.writeInt((byte) 0);  // Count
        } else {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 0);  // Size
            buffer.writeByte((byte) 0);  // Count
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(SaslMechanisms.class, typeDecoder.getTypeClass());

            try {
                typeDecoder.skipValue(stream, streamDecoderState);
                fail("Should not be able to skip type with invalid encoding");
            } catch (DecodeException ex) {}
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(SaslMechanisms.class, typeDecoder.getTypeClass());

            try {
                typeDecoder.skipValue(buffer, decoderState);
                fail("Should not be able to skip type with invalid encoding");
            } catch (DecodeException ex) {}
        }
    }

    @Test
    public void testDecodedWithInvalidMap32Type() throws IOException {
        doTestDecodeWithInvalidMapType(EncodingCodes.MAP32, false);
    }

    @Test
    public void testDecodeWithInvalidMap8Type() throws IOException {
        doTestDecodeWithInvalidMapType(EncodingCodes.MAP8, false);
    }

    @Test
    public void testDecodedWithInvalidMap32TypeFromStream() throws IOException {
        doTestDecodeWithInvalidMapType(EncodingCodes.MAP32, true);
    }

    @Test
    public void testDecodeWithInvalidMap8TypeFromStream() throws IOException {
        doTestDecodeWithInvalidMapType(EncodingCodes.MAP8, true);
    }

    private void doTestDecodeWithInvalidMapType(byte mapType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        if (mapType == EncodingCodes.MAP32) {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt((byte) 0);  // Size
            buffer.writeInt((byte) 0);  // Count
        } else {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 0);  // Size
            buffer.writeByte((byte) 0);  // Count
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            try {
                streamDecoder.readObject(stream, streamDecoderState);
                fail("Should not decode type with invalid encoding");
            } catch (DecodeException ex) {}
        } else {
            try {
                decoder.readObject(buffer, decoderState);
                fail("Should not decode type with invalid encoding");
            } catch (DecodeException ex) {}
        }
    }

    @Test
    public void testEncodeDecodeArray() throws IOException {
        doTestEncodeDecodeArray(false);
    }

    @Test
    public void testEncodeDecodeArrayFromStream() throws IOException {
        doTestEncodeDecodeArray(true);
    }

    public void doTestEncodeDecodeArray(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        SaslMechanisms[] array = new SaslMechanisms[3];

        array[0] = new SaslMechanisms();
        array[1] = new SaslMechanisms();
        array[2] = new SaslMechanisms();

        array[0].setSaslServerMechanisms(Symbol.getSASLSymbol("ANONYMOUS"), Symbol.getSASLSymbol("PLAIN"), Symbol.getSASLSymbol("EXTERNAL"));
        array[1].setSaslServerMechanisms(Symbol.getSASLSymbol("ANONYMOUS"), Symbol.getSASLSymbol("PLAIN"));
        array[2].setSaslServerMechanisms(Symbol.getSASLSymbol("ANONYMOUS"));

        encoder.writeObject(buffer, encoderState, array);

        final Object result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertTrue(result.getClass().isArray());
        assertEquals(SaslMechanisms.class, result.getClass().getComponentType());

        SaslMechanisms[] resultArray = (SaslMechanisms[]) result;

        for (int i = 0; i < resultArray.length; ++i) {
            assertNotNull(resultArray[i]);
            assertTrue(resultArray[i] instanceof SaslMechanisms);
            assertArrayEquals(array[i].getSaslServerMechanisms(), resultArray[i].getSaslServerMechanisms());
        }
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList0() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST0, false);
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList8() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST8, false);
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList32() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST32, false);
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList0FromStream() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST0, true);
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList8FromStream() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST8, true);
    }

    @Test
    public void testDecodeWithNotEnoughListEntriesList32FromStream() throws IOException {
        doTestDecodeWithNotEnoughListEntriesList32(EncodingCodes.LIST32, true);
    }

    private void doTestDecodeWithNotEnoughListEntriesList32(byte listType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        if (listType == EncodingCodes.LIST32) {
            buffer.writeByte(EncodingCodes.LIST32);
            buffer.writeInt((byte) 0);  // Size
            buffer.writeInt((byte) 0);  // Count
        } else if (listType == EncodingCodes.LIST8) {
            buffer.writeByte(EncodingCodes.LIST8);
            buffer.writeByte((byte) 0);  // Size
            buffer.writeByte((byte) 0);  // Count
        } else {
            buffer.writeByte(EncodingCodes.LIST0);
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            try {
                streamDecoder.readObject(stream, streamDecoderState);
                fail("Should not decode type with invalid min entries");
            } catch (DecodeException ex) {}
        } else {
            try {
                decoder.readObject(buffer, decoderState);
                fail("Should not decode type with invalid min entries");
            } catch (DecodeException ex) {}
        }
    }

    @Test
    public void testDecodeWithToManyListEntriesList8() throws IOException {
        doTestDecodeWithToManyListEntriesList32(EncodingCodes.LIST8, false);
    }

    @Test
    public void testDecodeWithToManyListEntriesList32() throws IOException {
        doTestDecodeWithToManyListEntriesList32(EncodingCodes.LIST32, false);
    }

    @Test
    public void testDecodeWithToManyListEntriesList8FromStream() throws IOException {
        doTestDecodeWithToManyListEntriesList32(EncodingCodes.LIST8, true);
    }

    @Test
    public void testDecodeWithToManyListEntriesList32FromStream() throws IOException {
        doTestDecodeWithToManyListEntriesList32(EncodingCodes.LIST32, true);
    }

    private void doTestDecodeWithToManyListEntriesList32(byte listType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        if (listType == EncodingCodes.LIST32) {
            buffer.writeByte(EncodingCodes.LIST32);
            buffer.writeInt((byte) 64);  // Size
            buffer.writeInt((byte) 8);  // Count
        } else if (listType == EncodingCodes.LIST8) {
            buffer.writeByte(EncodingCodes.LIST8);
            buffer.writeByte((byte) 64);  // Size
            buffer.writeByte((byte) 8);  // Count
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            try {
                streamDecoder.readObject(stream, streamDecoderState);
                fail("Should not decode type with invalid min entries");
            } catch (DecodeException ex) {}
        } else {
            try {
                decoder.readObject(buffer, decoderState);
                fail("Should not decode type with invalid min entries");
            } catch (DecodeException ex) {}
        }
    }

    @Test
    public void testDecodeFailsWhenArrayOfValuesSizeIsToLargeArray8() throws IOException {
        doTestDecodeFailsWhenArrayOfValuesSizeIsToLarge(EncodingCodes.ARRAY8, false);
    }

    @Test
    public void testDecodeFailsWhenArrayOfValuesSizeIsToLargeArray32() throws IOException {
        doTestDecodeFailsWhenArrayOfValuesSizeIsToLarge(EncodingCodes.ARRAY32, false);
    }

    private void doTestDecodeFailsWhenArrayOfValuesSizeIsToLarge(byte arrayType, boolean fromStream) throws IOException {
        ProtonBuffer buffer2 = ProtonBufferAllocator.defaultAllocator().allocate();

        if (arrayType == EncodingCodes.ARRAY32) {
            buffer2.writeByte(EncodingCodes.ARRAY32);
            buffer2.writeInt(13);  // Size
            buffer2.writeInt(2);   // Count
        } else {
            buffer2.writeByte(EncodingCodes.ARRAY8);
            buffer2.writeByte((byte) 10);  // Size
            buffer2.writeByte((byte) 2);  // Count
        }
        buffer2.writeByte((byte) 0); // Described Type Indicator
        buffer2.writeByte(EncodingCodes.SMALLULONG);
        buffer2.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        buffer2.writeByte(EncodingCodes.LIST8);
        buffer2.writeByte((byte) 1);  // Size
        buffer2.writeByte((byte) 0);  // Count
        buffer2.writeByte((byte) 1);  // Size
        buffer2.writeByte((byte) 0);  // Count

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer2);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(Object.class, typeDecoder.getTypeClass());
            assertThrows(DecodeException.class, () -> typeDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer2, decoderState);
            assertEquals(Object.class, typeDecoder.getTypeClass());
            assertThrows(DecodeException.class, () -> typeDecoder.readValue(buffer2, decoderState));
        }
    }

    @Test
    public void testDecodeFailsWhenArrayOfTypeWithList0EncodingsArray8() throws IOException {
        doTestDecodeFailsWhenArrayOfTypeWithList0Encodings(EncodingCodes.ARRAY8, false);
    }

    @Test
    public void testDecodeFailsWhenArrayOfTypeWithList0EncodingsArray32() throws IOException {
        doTestDecodeFailsWhenArrayOfTypeWithList0Encodings(EncodingCodes.ARRAY32, false);
    }

    @Test
    public void testDecodeFailsWhenArrayOfTypeWithList0EncodingsArray8FS() throws IOException {
        doTestDecodeFailsWhenArrayOfTypeWithList0Encodings(EncodingCodes.ARRAY8, true);
    }

    @Test
    public void testDecodeFailsWhenArrayOfTypeWithList0EncodingsArray32FS() throws IOException {
        doTestDecodeFailsWhenArrayOfTypeWithList0Encodings(EncodingCodes.ARRAY32, true);
    }

    private void doTestDecodeFailsWhenArrayOfTypeWithList0Encodings(byte arrayType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        if (arrayType == EncodingCodes.ARRAY32) {
            buffer.writeByte(EncodingCodes.ARRAY32);
            buffer.writeInt(8);  // Size
            buffer.writeInt(2);  // Count
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 5);  // Size
            buffer.writeByte((byte) 2);  // Count
        }
        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        buffer.writeByte(EncodingCodes.LIST0);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer.copy(true));
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(Object.class, typeDecoder.getTypeClass());
            assertThrows(DecodeException.class, () -> typeDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(Object.class, typeDecoder.getTypeClass());
            assertThrows(DecodeException.class, () -> typeDecoder.readValue(buffer, decoderState));
        }
    }

    @Test
    public void testDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType8() throws IOException {
        doTestDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType(EncodingCodes.LIST8, false);
    }

    @Test
    public void testDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType32() throws IOException {
        doTestDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType(EncodingCodes.LIST32, false);
    }

    @Test
    public void testDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType8FS() throws IOException {
        doTestDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType(EncodingCodes.LIST8, true);
    }

    @Test
    public void testDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType32FS() throws IOException {
        doTestDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType(EncodingCodes.LIST32, true);
    }

    private void doTestDecodeFailsFastWhenMechanismsArrayIsNotTheExpectedType(byte listType, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte((byte) 0); // Described Type Indicator
        buffer.writeByte(EncodingCodes.SMALLULONG);
        buffer.writeByte(SaslMechanisms.DESCRIPTOR_CODE.byteValue());
        if (listType == EncodingCodes.LIST32) {
            buffer.writeByte(EncodingCodes.LIST32);
            buffer.writeInt((byte) 24);  // Size
            buffer.writeInt((byte) 1);  // Count
        } else if (listType == EncodingCodes.LIST8) {
            buffer.writeByte(EncodingCodes.LIST8);
            buffer.writeByte((byte) 21);  // Size
            buffer.writeByte((byte) 1);  // Count
        }

        final UUID value = UUID.randomUUID();

        buffer.writeByte(EncodingCodes.ARRAY8);
        buffer.writeByte((byte) 18);
        buffer.writeByte((byte) 1);
        buffer.writeByte(EncodingCodes.UUID);
        buffer.writeLong(value.getMostSignificantBits());
        buffer.writeLong(value.getLeastSignificantBits());

        if (fromStream) {
            final InputStream stream = new ProtonBufferInputStream(buffer);
            assertThrows(DecodeException.class, () -> streamDecoder.readObject(stream, streamDecoderState));
            assertTrue(stream.available() > 0);
        } else {
            assertThrows(DecodeException.class, () -> decoder.readObject(buffer, decoderState));
            assertTrue(buffer.isReadable());
        }
    }
}
