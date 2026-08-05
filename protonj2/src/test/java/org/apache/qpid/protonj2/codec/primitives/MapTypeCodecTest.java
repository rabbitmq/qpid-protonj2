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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.buffer.ProtonBufferInputStream;
import org.apache.qpid.protonj2.codec.CodecTestSupport;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.apache.qpid.protonj2.codec.EncodingCodes;
import org.apache.qpid.protonj2.codec.StreamTypeDecoder;
import org.apache.qpid.protonj2.codec.TypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.PrimitiveArrayTypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.PrimitiveTypeDecoder;
import org.apache.qpid.protonj2.codec.decoders.ProtonScanningContext;
import org.apache.qpid.protonj2.codec.decoders.primitives.MapTypeDecoder;
import org.apache.qpid.protonj2.types.Binary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MapTypeCodecTest extends CodecTestSupport {

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

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            try {
                streamDecoder.readMap(stream, streamDecoderState);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}
        } else {
            try {
                decoder.readMap(buffer, decoderState);
                fail("Should not allow read of integer type as this type");
            } catch (DecodeException e) {}
        }
    }

    @Test
    public void testDecodeSmallSeriesOfMaps() throws IOException {
        doTestDecodeMapSeries(SMALL_SIZE, false);
    }

    @Test
    public void testDecodeLargeSeriesOfMaps() throws IOException {
        doTestDecodeMapSeries(LARGE_SIZE, false);
    }

    @Test
    public void testDecodeSmallSeriesOfMapsFromStream() throws IOException {
        doTestDecodeMapSeries(SMALL_SIZE, true);
    }

    @Test
    public void testDecodeLargeSeriesOfMapsFromStream() throws IOException {
        doTestDecodeMapSeries(LARGE_SIZE, true);
    }

    @SuppressWarnings("unchecked")
    private void doTestDecodeMapSeries(int size, boolean fromStream) throws IOException {
        String myBoolKey = "myBool";
        boolean myBool = true;
        String myByteKey = "myByte";
        byte myByte = 4;
        String myBytesKey = "myBytes";
        byte[] myBytes = myBytesKey.getBytes();
        String myCharKey = "myChar";
        char myChar = 'd';
        String myDoubleKey = "myDouble";
        double myDouble = 1234567890123456789.1234;
        String myFloatKey = "myFloat";
        float myFloat = 1.1F;
        String myIntKey = "myInt";
        int myInt = Integer.MAX_VALUE;
        String myLongKey = "myLong";
        long myLong = Long.MAX_VALUE;
        String myShortKey = "myShort";
        short myShort = 25;
        String myStringKey = "myString";
        String myString = myStringKey;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(myBoolKey, myBool);
        map.put(myByteKey, myByte);
        map.put(myBytesKey, new Binary(myBytes));
        map.put(myCharKey, myChar);
        map.put(myDoubleKey, myDouble);
        map.put(myFloatKey, myFloat);
        map.put(myIntKey, myInt);
        map.put(myLongKey, myLong);
        map.put(myShortKey, myShort);
        map.put(myStringKey, myString);

        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        for (int i = 0; i < size; ++i) {
            encoder.writeObject(buffer, encoderState, map);
        }

        final InputStream stream;
        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        for (int i = 0; i < size; ++i) {
            final Object result;
            if (fromStream) {
                result = streamDecoder.readObject(stream, streamDecoderState);
            } else {
                result = decoder.readObject(buffer, decoderState);
            }

            assertNotNull(result);
            assertTrue(result instanceof Map);

            Map<String, Object> resultMap = (Map<String, Object>) result;

            assertEquals(map.size(), resultMap.size());
        }
    }

    @Test
    public void testArrayOfMApsOfStringToUUIDs() throws IOException {
        testArrayOfMApsOfStringToUUIDs(false);
    }

    @Test
    public void testArrayOfMApsOfStringToUUIDsFS() throws IOException {
        testArrayOfMApsOfStringToUUIDs(true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void testArrayOfMApsOfStringToUUIDs(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, UUID>[] source = new LinkedHashMap[2];
        for (int i = 0; i < source.length; ++i) {
            source[i] = new LinkedHashMap<>();
            source[i].put("1", UUID.randomUUID());
            source[i].put("2", UUID.randomUUID());
            source[i].put("3", UUID.randomUUID());
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

        Map[] map = (Map[]) result;
        assertEquals(source.length, map.length);

        for (int i = 0; i < map.length; ++i) {
            assertEquals(source[i], map[i]);
        }
    }

    @Test
    public void testMapOfArraysOfUUIDsIndexedByString() throws IOException {
        testMapOfArraysOfUUIDsIndexedByString(false);
    }

    @Test
    public void testMapOfArraysOfUUIDsIndexedByStringFS() throws IOException {
        testMapOfArraysOfUUIDsIndexedByString(true);
    }

    @SuppressWarnings({ "unchecked" })
    private void testMapOfArraysOfUUIDsIndexedByString(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        UUID[] element1 = new UUID[] { UUID.randomUUID() };
        UUID[] element2 = new UUID[] { UUID.randomUUID(), UUID.randomUUID() };
        UUID[] element3 = new UUID[] { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };

        UUID[][] expected = new UUID[][] { element1, element2, element3 };

        Map<String, UUID[]> source = new LinkedHashMap<>();
        source.put("1", element1);
        source.put("2", element2);
        source.put("3", element3);

        encoder.writeMap(buffer, encoderState, source);

        final Object result;
        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            result = streamDecoder.readObject(stream, streamDecoderState);
        } else {
            result = decoder.readObject(buffer, decoderState);
        }

        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, UUID[]> map = (Map<String, UUID[]>) result;
        assertEquals(source.size(), map.size());

        for (int i = 1; i <= map.size(); ++i) {
            Object entry = map.get(Integer.toString(i));
            assertNotNull(entry);
            assertTrue(entry.getClass().isArray());
            UUID[] uuids = (UUID[]) entry;
            assertEquals(i, uuids.length);
            assertArrayEquals(expected[i - 1], uuids);
        }
    }

    @Test
    public void testSizeToLargeValidationMAP32() throws IOException {
        dotestSizeToLargeValidation(EncodingCodes.MAP32, true);
    }

    @Test
    public void testSizeToLargeValidationMAP8() throws IOException {
        dotestSizeToLargeValidation(EncodingCodes.MAP8, true);
    }

    private void dotestSizeToLargeValidation(byte encodingCode, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(encodingCode);
        if (encodingCode == EncodingCodes.MAP32) {
            buffer.writeInt(Integer.MAX_VALUE);
            buffer.writeInt(4);
        } else {
            buffer.writeByte(Byte.MAX_VALUE);
            buffer.writeByte((byte) 4);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.UTF_8));
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 5);
        buffer.writeBytes("value".getBytes(StandardCharsets.UTF_8));

        final InputStream stream;
        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        if (fromStream) {
            StreamTypeDecoder<?> typeDecoder = streamDecoder.peekNextTypeDecoder(stream, streamDecoderState);
            assertEquals(Map.class, typeDecoder.getTypeClass());
            assertTrue(typeDecoder instanceof PrimitiveTypeDecoder);
            assertEquals(((PrimitiveTypeDecoder<?>) typeDecoder).getTypeCode(), encodingCode & 0xFF);
        } else {
            TypeDecoder<?> typeDecoder = decoder.peekNextTypeDecoder(buffer, decoderState);
            assertEquals(Map.class, typeDecoder.getTypeClass());
            assertTrue(typeDecoder instanceof PrimitiveTypeDecoder);
            assertEquals(((PrimitiveTypeDecoder<?>) typeDecoder).getTypeCode(), encodingCode & 0xFF);
        }

        if (fromStream) {
            try {
                streamDecoder.readObject(stream, streamDecoderState);
                fail("should throw an IllegalArgumentException");
            } catch (IllegalArgumentException iae) {}
        } else {
            try {
                decoder.readObject(buffer, decoderState);
                fail("should throw an IllegalArgumentException");
            } catch (IllegalArgumentException iae) {}
        }
    }

    @Test
    public void testOddElementCountDetectedMAP32() throws IOException {
        doTestOddElementCountDetected(EncodingCodes.MAP32, false);
    }

    @Test
    public void testOddElementCountDetectedMAP8() throws IOException {
        doTestOddElementCountDetected(EncodingCodes.MAP8, false);
    }

    @Test
    public void testOddElementCountDetectedMAP32FS() throws IOException {
        doTestOddElementCountDetected(EncodingCodes.MAP32, true);
    }

    @Test
    public void testOddElementCountDetectedMAP8FS() throws IOException {
        doTestOddElementCountDetected(EncodingCodes.MAP8, true);
    }

    private void doTestOddElementCountDetected(byte encodingCode, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(encodingCode);
        if (encodingCode == EncodingCodes.MAP32) {
            buffer.writeInt(17);
            buffer.writeInt(1);
        } else {
            buffer.writeByte((byte) 14);
            buffer.writeByte((byte) 1);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.UTF_8));
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 5);
        buffer.writeBytes("value".getBytes(StandardCharsets.UTF_8));

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            try {
                streamDecoder.readObject(stream, streamDecoderState);
                fail("should throw an IllegalArgumentException");
            } catch (IllegalArgumentException iae) {}
        } else {
            try {
                decoder.readObject(buffer, decoderState);
                fail("should throw an IllegalArgumentException");
            } catch (IllegalArgumentException iae) {}
        }
    }

    @Test
    public void testSkipValue() throws IOException {
        doTestSkipValue(false);
    }

    @Test
    public void testSkipValueFromStream() throws IOException {
        doTestSkipValue(true);
    }

    @SuppressWarnings("unchecked")
    public void doTestSkipValue(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, UUID> skip = new HashMap<>();
        for (int i = 0; i < 10; ++i) {
            skip.put(UUID.randomUUID().toString(), UUID.randomUUID());
        }

        for (int i = 0; i < 10; ++i) {
            encoder.writeMap(buffer, encoderState, skip);
        }

        Map<String, UUID> expected = new LinkedHashMap<>();
        expected.put(UUID.randomUUID().toString(), UUID.randomUUID());

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
                assertEquals(Map.class, typeDecoder.getTypeClass());
                typeDecoder.skipValue(stream, streamDecoderState);
            } else {
                TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
                assertEquals(Map.class, typeDecoder.getTypeClass());
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
        assertTrue(result instanceof Map);

        Map<String, UUID> value = (Map<String, UUID>) result;
        assertEquals(expected, value);
    }

    @Test
    public void testEncodeMapWithUnknownEntryValueType() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("unknown", new MyUnknownTestType());

        doTestEncodeMapWithUnknownEntryValueTypeTestImpl(map);
    }

    @Test
    public void testEncodeSubMapWithUnknownEntryValueType() throws Exception {
        Map<String, Object> subMap = new HashMap<>();
        subMap.put("unknown", new MyUnknownTestType());

        Map<String, Object> map = new HashMap<>();
        map.put("submap", subMap);

        doTestEncodeMapWithUnknownEntryValueTypeTestImpl(map);
    }

    private void doTestEncodeMapWithUnknownEntryValueTypeTestImpl(Map<String, Object> map) {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        try {
            encoder.writeMap(buffer, encoderState, map);
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), containsString("Cannot find encoder for type"));
            assertThat(iae.getMessage(), containsString(MyUnknownTestType.class.getSimpleName()));
        }
    }

    @Test
    public void testEncodeMapWithUnknownEntryKeyType() throws Exception {
        Map<Object, String> map = new HashMap<>();
        map.put(new MyUnknownTestType(), "unknown");

        doTestEncodeMapWithUnknownEntryKeyTypeTestImpl(map);
    }

    @Test
    public void testEncodeSubMapWithUnknownEntryKeyType() throws Exception {
        Map<Object, String> subMap = new HashMap<>();
        subMap.put(new MyUnknownTestType(), "unknown");

        Map<String, Object> map = new HashMap<>();
        map.put("submap", subMap);

        doTestEncodeMapWithUnknownEntryKeyTypeTestImpl(map);
    }

    @Test
    public void testStreamSkipOfMapEncodingHandlesIOException() throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, UUID> skip = new HashMap<>();
        for (int i = 0; i < 10; ++i) {
            skip.put(UUID.randomUUID().toString(), UUID.randomUUID());
        }

        encoder.writeMap(buffer, encoderState, skip);

        InputStream stream = new ProtonBufferInputStream(buffer);
        StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
        assertEquals(Map.class, typeDecoder.getTypeClass());

        stream = Mockito.spy(stream);

        Mockito.when(stream.skip(Mockito.anyLong())).thenThrow(EOFException.class);

        try {
            typeDecoder.skipValue(stream, streamDecoderState);
            fail("Expected an exception on skip when it throws.");
        } catch (DecodeException dex) {}
    }

    private void doTestEncodeMapWithUnknownEntryKeyTypeTestImpl(Map<?, ?> map) {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        try {
            encoder.writeMap(buffer, encoderState, map);
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), containsString("Cannot find encoder for type"));
            assertThat(iae.getMessage(), containsString(MyUnknownTestType.class.getSimpleName()));
        }
    }

    private static class MyUnknownTestType {

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

        buffer.writeByte(EncodingCodes.MAP8);
        buffer.writeByte((byte) 8);
        buffer.writeByte(EncodingCodes.MAP32);
        buffer.writeInt(16);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(8, typeDecoder.readSize(stream, streamDecoderState));
            typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertEquals(16, typeDecoder.readSize(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(8, typeDecoder.readSize(buffer, decoderState));
            typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertEquals(16, typeDecoder.readSize(buffer, decoderState));
        }
    }

    @Test
    public void testScanEncodedMapForSpecificKey() throws IOException {
        doTestScanEncodedMapForSpecificKey(false);
    }

    @Test
    public void testScanEncodedMapForSpecificKeyFromStream() throws IOException {
        doTestScanEncodedMapForSpecificKey(true);
    }

    private void doTestScanEncodedMapForSpecificKey(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, Object> propertiesMap = new LinkedHashMap<>();

        propertiesMap.put("key-1", "1");
        propertiesMap.put("key-2", "2");
        propertiesMap.put("key-3", "3");
        propertiesMap.put("key-4", "4");
        propertiesMap.put("key-5", "5");
        propertiesMap.put("key-6", "6");
        propertiesMap.put("key-7", "7");
        propertiesMap.put("key-8", "8");

        final Collection<String> searchDomain = new ArrayList<>();
        searchDomain.add("key-2");

        encoder.writeObject(buffer, encoderState, propertiesMap);

        final InputStream stream;

        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        final AtomicBoolean matchFound = new AtomicBoolean();
        final ProtonScanningContext<String> context = ProtonScanningContext.createStringScanContext(searchDomain);

        final MapTypeDecoder result;
        if (fromStream) {
            result = (MapTypeDecoder) streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertNotNull(result);
            result.scanKeys(stream, streamDecoderState, context, (k, v) -> {
                assertEquals("2", v);
                matchFound.set(true);
            });
            assertEquals(0, stream.available());
        } else {
            result = (MapTypeDecoder) decoder.readNextTypeDecoder(buffer, decoderState);
            assertNotNull(result);
            result.scanKeys(buffer, decoderState, context, (k, v) -> {
                assertEquals("2", v);
                matchFound.set(true);
            });
            assertFalse(buffer.isReadable());
        }

        assertTrue(matchFound.get());
    }

    @Test
    public void testScanEncodedApplicationPropertiesForSpecificKeyFromStreamRejectsStreamWihtoutMarkSupport() throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, Object> propertiesMap = new LinkedHashMap<>();

        propertiesMap.put("key-1", "1");
        propertiesMap.put("key-2", "2");
        propertiesMap.put("key-3", "3");
        propertiesMap.put("key-4", "4");
        propertiesMap.put("key-5", "5");
        propertiesMap.put("key-6", "6");
        propertiesMap.put("key-7", "7");
        propertiesMap.put("key-8", "8");

        final Collection<String> searchDomain = new ArrayList<>();
        searchDomain.add("key-2");

        encoder.writeObject(buffer, encoderState, propertiesMap);

        final InputStream stream = Mockito.spy(new ProtonBufferInputStream(buffer));

        Mockito.when(stream.markSupported()).thenReturn(false);

        final AtomicBoolean matchFound = new AtomicBoolean();
        final ProtonScanningContext<String> context = ProtonScanningContext.createStringScanContext(searchDomain);

        final MapTypeDecoder result = (MapTypeDecoder) streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
        assertNotNull(result);
        try {
            result.scanKeys(stream, streamDecoderState, context, (k, v) -> {
                matchFound.set(true);
            });
            fail("Should fail if stream says it cannot support marking");
        } catch (UnsupportedOperationException ex) {
            // Expected
        }

        assertFalse(matchFound.get());
    }

    @Test
    public void testDecodingOfDeeplyNestedMapOfMapsFromBuffer() throws Exception {
        doTestDecodingOfDeeplyNestedMapOfMaps(false, 10);
    }

    @Test
    public void testDecodingOfDeeplyNestedMapOfMapsFromStream() throws Exception {
        doTestDecodingOfDeeplyNestedMapOfMaps(true, 10);
    }

    private void doTestDecodingOfDeeplyNestedMapOfMaps(boolean fromStream, int depthLimit) throws IOException {
        final Map<String, Object> toEncode = new HashMap<>();

        Map<String, Object> current = toEncode;

        // Encodes one more than the max depth value set
        for (int i = 0; i < depthLimit; ++i) {
            final Map<String, Object> next = new HashMap<>();

            current.put(String.valueOf(i), next);
            current = next;
        }

        final ProtonBuffer buffer1 = ProtonBufferAllocator.defaultAllocator().allocate();

        encoder.writeMap(buffer1, encoderState, toEncode);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer1);
            streamDecoderState.setDepthLimit(depthLimit);

            assertThrows(DecodeException.class, () -> streamDecoder.readMap(stream, streamDecoderState));
        } else {
            decoderState.setDepthLimit(depthLimit);

            assertThrows(DecodeException.class, () -> decoder.readMap(buffer1, decoderState));
        }

        // Encode up to the limit instead which should work
        toEncode.clear();
        current = toEncode;
        streamDecoderState.reset();
        decoderState.reset();

        for (int i = 0; i < depthLimit; ++i) {
            final Map<String, Object> next = new HashMap<>();

            current.put(String.valueOf(i), next);
            current = next;
        }

        final ProtonBuffer buffer2 = ProtonBufferAllocator.defaultAllocator().allocate();

        encoder.writeMap(buffer2, encoderState, toEncode);

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer2);
            streamDecoderState.setDepthLimit(depthLimit + 1);

            assertTrue(streamDecoder.readMap(stream, streamDecoderState) instanceof Map);
        } else {
            decoderState.setDepthLimit(depthLimit + 1);

            assertTrue(decoder.readMap(buffer2, decoderState) instanceof Map);
        }
    }

    @Test
    public void testScanEncodedMapForSpecificKeyWithNoMatchConsumesEncoding() throws IOException {
        doTestScanEncodedMapForSpecificKeyWithNoMatchConsumesEncoding(false);
    }

    @Test
    public void testScanEncodedMapForSpecificKeyWithNoMatchConsumesEncodingFromStream() throws IOException {
        doTestScanEncodedMapForSpecificKeyWithNoMatchConsumesEncoding(true);
    }

    private void doTestScanEncodedMapForSpecificKeyWithNoMatchConsumesEncoding(boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        Map<String, Object> propertiesMap = new LinkedHashMap<>();

        propertiesMap.put("key-1", "1");
        propertiesMap.put("key-2", "2");
        propertiesMap.put("key-3", "3");
        propertiesMap.put("key-4", "4");
        propertiesMap.put("key-5", "5");
        propertiesMap.put("key-6", "6");
        propertiesMap.put("key-7", "7");
        propertiesMap.put("key-8", "8");

        final Collection<String> searchDomain = new ArrayList<>();
        searchDomain.add("key-99");

        encoder.writeObject(buffer, encoderState, propertiesMap);

        final InputStream stream;

        if (fromStream) {
            stream = new ProtonBufferInputStream(buffer);
        } else {
            stream = null;
        }

        final AtomicBoolean matchFound = new AtomicBoolean();
        final ProtonScanningContext<String> context = ProtonScanningContext.createStringScanContext(searchDomain);

        final MapTypeDecoder result;
        if (fromStream) {
            result = (MapTypeDecoder) streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertNotNull(result);
            result.scanKeys(stream, streamDecoderState, context, (k, v) -> {
                matchFound.set(true);
            });
            assertEquals(0, stream.available());
        } else {
            result = (MapTypeDecoder) decoder.readNextTypeDecoder(buffer, decoderState);
            assertNotNull(result);
            result.scanKeys(buffer, decoderState, context, (k, v) -> {
                matchFound.set(true);
            });
            assertFalse(buffer.isReadable());
        }

        assertFalse(matchFound.get());
    }

    @Test
    public void testCountGreaterThanAvailableDataCausesExceptionMap32() throws IOException {
        doTestCountGreaterThanAvailableDataCausesExceptionMap(EncodingCodes.MAP32, false);
    }

    @Test
    public void testCountGreaterThanAvailableDataCausesExceptionMap8() throws IOException {
        doTestCountGreaterThanAvailableDataCausesExceptionMap(EncodingCodes.MAP8, false);
    }

    @Test
    public void testCountGreaterThanAvailableDataCausesExceptionMap32FS() throws IOException {
        doTestCountGreaterThanAvailableDataCausesExceptionMap(EncodingCodes.MAP32, true);
    }

    @Test
    public void testCountGreaterThanAvailableDataCausesExceptionMap8FS() throws IOException {
        doTestCountGreaterThanAvailableDataCausesExceptionMap(EncodingCodes.MAP8, true);
    }

    private void doTestCountGreaterThanAvailableDataCausesExceptionMap(byte encodingCode, boolean fromStream) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        buffer.writeByte(encodingCode);
        if (encodingCode == EncodingCodes.MAP32) {
            buffer.writeInt(4);   // Size
            buffer.writeInt(16);  // Count
        } else {
            buffer.writeByte((byte) 1);  // Size
            buffer.writeByte((byte) 16); // Count
        }

        if (fromStream) {
            InputStream stream = new ProtonBufferInputStream(buffer);
            StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
            assertTrue(typeDecoder instanceof MapTypeDecoder);
            MapTypeDecoder mapDecoder = (MapTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> mapDecoder.readValue(stream, streamDecoderState));
        } else {
            TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
            assertTrue(typeDecoder instanceof MapTypeDecoder);
            MapTypeDecoder mapDecoder = (MapTypeDecoder) typeDecoder;
            assertThrows(DecodeException.class, () -> mapDecoder.readValue(buffer, decoderState));
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
            buffer.writeByte(EncodingCodes.MAP32);
        } else {
            buffer.writeByte(EncodingCodes.ARRAY8);
            buffer.writeByte((byte) 2);  // Size
            buffer.writeByte(Byte.MAX_VALUE);  // Count
            buffer.writeByte(EncodingCodes.MAP8);
        }

        TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
        assertTrue(typeDecoder instanceof PrimitiveArrayTypeDecoder);
        PrimitiveArrayTypeDecoder arrayDecoder = (PrimitiveArrayTypeDecoder) typeDecoder;
        assertThrows(DecodeException.class, () -> arrayDecoder.readValue(buffer, decoderState));
    }

    @Test
    public void testStreamDecodeFailsWhenEncodedLengthExceedsConfigurationMap() throws IOException {
        doTestStreamDecodeFailsWhenEncodedLengthExceedsConfiguration(true);
    }

    @Test
    public void testStreamDecodeFailsWhenEncodedLengthExceedsConfigurationMap32() throws IOException {
        doTestStreamDecodeFailsWhenEncodedLengthExceedsConfiguration(false);
    }

    private void doTestStreamDecodeFailsWhenEncodedLengthExceedsConfiguration(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[256];

        streamDecoderState.setMaxMapSize(24);

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 136);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(139);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        InputStream stream = new ProtonBufferInputStream(buffer);
        StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
        assertThrows(DecodeException.class, () -> typeDecoder.readValue(stream, streamDecoderState));
    }

    @Test
    public void testStreamSkipValueFailsWhenEncodedLengthExceedsConfigurationMap8() throws IOException {
        doTestStreamSkipValueFailsWhenEncodedLengthExceedsConfiguration(true);
    }

    @Test
    public void testStreamSkipValueFailsWhenEncodedLengthExceedsConfigurationMap32() throws IOException {
        doTestStreamSkipValueFailsWhenEncodedLengthExceedsConfiguration(false);
    }

    private void doTestStreamSkipValueFailsWhenEncodedLengthExceedsConfiguration(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[127];

        streamDecoderState.setMaxMapSize(24);

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 136);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(139);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        InputStream stream = new ProtonBufferInputStream(buffer);
        StreamTypeDecoder<?> typeDecoder = streamDecoder.readNextTypeDecoder(stream, streamDecoderState);
        assertThrows(DecodeException.class, () -> typeDecoder.skipValue(stream, streamDecoderState));
    }

    @Test
    public void testReadValueFailsWhenEncodedLengthExceedsAvailableMap8() throws IOException {
        doTestReadValueFailsWhenEncodedLengthExceedsAvailable(true);
    }

    @Test
    public void testReadValueFailsWhenEncodedLengthExceedsAvailableMap32() throws IOException {
        doTestReadValueFailsWhenEncodedLengthExceedsAvailable(false);
    }

    private void doTestReadValueFailsWhenEncodedLengthExceedsAvailable(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[127];

        streamDecoderState.setMaxMapSize(24);

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 137);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(143);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
        assertThrows(DecodeException.class, () -> typeDecoder.readValue(buffer, decoderState));
    }

    @Test
    public void testSkipValueFailsWhenEncodedLengthExceedsAvailableMap8() throws IOException {
        doTestSkipValueFailsWhenEncodedLengthExceedsAvailable(true);
    }

    @Test
    public void testSkipValueFailsWhenEncodedLengthExceedsAvailableMap32() throws IOException {
        doTestSkipValueFailsWhenEncodedLengthExceedsAvailable(false);
    }

    private void doTestSkipValueFailsWhenEncodedLengthExceedsAvailable(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[127];

        streamDecoderState.setMaxMapSize(24);

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 137);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(143);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        TypeDecoder<?> typeDecoder = decoder.readNextTypeDecoder(buffer, decoderState);
        assertThrows(DecodeException.class, () -> typeDecoder.skipValue(buffer, decoderState));
    }

    @Test
    public void testStreamScanKeyFailsWhenEncodedLengthExceedsAvailableMap8() throws IOException {
        doTestStreamScanKeysFailsWhenEncodedLengthExceedsAvailable(true);
    }

    @Test
    public void testStreamScanKeysFailsWhenEncodedLengthExceedsAvailableMap32() throws IOException {
        doTestStreamScanKeysFailsWhenEncodedLengthExceedsAvailable(false);
    }

    private void doTestStreamScanKeysFailsWhenEncodedLengthExceedsAvailable(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[127];

        streamDecoderState.setMaxMapSize(24);

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 137);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(143);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        final InputStream stream = new ProtonBufferInputStream(buffer);
        final Collection<String> searchDomain = new ArrayList<>();
        final ProtonScanningContext<String> context = ProtonScanningContext.createStringScanContext(searchDomain);
        final MapTypeDecoder typeDecoder = (MapTypeDecoder) streamDecoder.readNextTypeDecoder(stream, streamDecoderState);

        assertThrows(DecodeException.class, () -> typeDecoder.scanKeys(stream, streamDecoderState, context, (k, v) -> {}));
    }

    @Test
    public void testScanKeyFailsWhenEncodedLengthExceedsAvailableMap8() throws IOException {
        doTestScanKeysFailsWhenEncodedLengthExceedsAvailable(true);
    }

    @Test
    public void testScanKeysFailsWhenEncodedLengthExceedsAvailableMap32() throws IOException {
        doTestScanKeysFailsWhenEncodedLengthExceedsAvailable(false);
    }

    private void doTestScanKeysFailsWhenEncodedLengthExceedsAvailable(boolean smallEncoding) throws IOException {
        ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().allocate();

        final byte[] payload = new byte[127];

        if (smallEncoding) {
            buffer.writeByte(EncodingCodes.MAP8);
            buffer.writeByte((byte) 137);
            buffer.writeByte((byte) 2);
        } else {
            buffer.writeByte(EncodingCodes.MAP32);
            buffer.writeInt(143);
            buffer.writeInt(2);
        }
        buffer.writeByte(EncodingCodes.STR8);
        buffer.writeByte((byte) 4);
        buffer.writeBytes("test".getBytes(StandardCharsets.US_ASCII));
        buffer.writeByte(EncodingCodes.VBIN8);
        buffer.writeByte((byte) 127);
        buffer.writeBytes(payload);

        final Collection<String> searchDomain = new ArrayList<>();
        final ProtonScanningContext<String> context = ProtonScanningContext.createStringScanContext(searchDomain);
        final MapTypeDecoder typeDecoder = (MapTypeDecoder) decoder.readNextTypeDecoder(buffer, decoderState);

        assertThrows(DecodeException.class, () -> typeDecoder.scanKeys(buffer, decoderState, context, (k, v) -> {}));
    }
}
