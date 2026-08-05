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
package org.apache.qpid.protonj2.codec.decoders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.buffer.ProtonBufferInputStream;
import org.apache.qpid.protonj2.codec.CodecTestSupport;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.junit.jupiter.api.Test;

/**
 * Tests that decoders throw expected exception for specific bad encodings.
 */
public class ProtonDecodersErrorTest extends CodecTestSupport {

    // Application Properties Tests (Descriptor 0x74)

    @Test
    public void testApplicationPropertiesHasUnevenCountFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x03, 0x01, 0x40, 0x40, // size , count , elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "Map encoded number of elements 1 is not an even number.");
    }

    @Test
    public void testApplicationPropertiesHasUnevenCountFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x03, 0x01, 0x40, 0x40, // size , count , elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "Map encoded number of elements 1 is not an even number.");
    }

    @Test
    public void testApplicationPropertiesSizeGreaterThanAvailableBytesFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x05, 0x04, 0x40, 0x40, // size, count, elements...
                                     0x40 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "Map encoded size is specified to be greater than the amount of data available s:(5) r:(4)");
    }

    @Test
    public void testApplicationPropertiesSizeGreaterThanAvailableBytesFS() throws IOException {
        streamDecoderState.setMaxMapSize(4);

        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x05, 0x04, 0x40, 0x40, // size, count, elements...
                                     0x40 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "Map encoded size is specified to be greater than the configured maximum map size allowed s:(5) c:(4)");
    }

    @Test
    public void testApplicationPropertiesCountGreaterThanAvailableBytesFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x04, 0x04, 0x40, 0x40, // size, count, elements...
                                     0x40 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "Map encoded count is specified to be greater than the reported encoded size s:(4) c:(4)");
    }

    @Test
    public void testApplicationPropertiesCountGreaterThanAvailableBytesFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x04, 0x04, 0x40, 0x40, // size, count, elements...
                                     0x40 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "Map encoded count is specified to be greater than the reported encoded size s:(4) c:(4)");
    }

    @Test
    public void testApplicationPropertiesSizeGreaterThanDecodedResultFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x74, (byte) 0xc1, // Described-type, ulong type, header descriptor, map8.
                                     0x06, 0x02, // size , count , elements...
                                     (byte) 0xa1, 0x01, 0x41, 0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "Map decoding did not read the expected amount of bytes: 6");
    }

    // Header Section Tests (Descriptor: 0x70)

    @Test
    public void testHeaderIndicatesCountLargerThanElementsInHeaderProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x70, (byte) 0xC0, // Described-type, ulong type, header descriptor, list8.
                                     0x07, 0x06, 0x40, 0x00, // size (7), count (6), elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 5 but got 6");
    }

    @Test
    public void testHeaderIndicatesCountLargerThanElementsInHeaderProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x70, (byte) 0xC0,
                                     0x07, 0x06, 0x40, 0x00,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 5 but got 6");
    }

    // Properties Section Tests (Descriptor: 0x73)

    @Test
    public void testPropertiesIndicatesCountLargerThanElementsInPropertiesProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x73, (byte) 0xC0, // Described-type, ulong type, properties descriptor, list8.
                                     0x0F, 0x0E, 0x40, 0x00, // size (15), count (14), elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 13 but got 14");
    }

    @Test
    public void testPropertiesIndicatesCountLargerThanElementsInPropertiesProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x73, (byte) 0xC0,
                                     0x0F, 0x0E, 0x40, 0x00,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 13 but got 14");
    }

    // Received Outcome Tests (Descriptor: 0x23)

    @Test
    public void testReceivedWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x23, 0x45}; // list0 encoding

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 2 but got 0");
    }

    @Test
    public void testReceivedWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x23, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 2 but got 0");
    }

    @Test
    public void testReceivedIndicatesCountLargerThanElementsInReceivedProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x23, (byte) 0xC0, // Described-type, ulong type, received descriptor, list8.
                                     0x05, 0x03, 0x43, 0x44, // size (5), count (3), elements...
                                     0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    @Test
    public void testReceivedIndicatesCountLargerThanElementsInReceivedProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x23, (byte) 0xC0,
                                     0x05, 0x03, 0x43, 0x44,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    // Rejected Outcome Tests (Descriptor: 0x25)

    @Test
    public void testRejectedIndicatesCountLargerThanElementsInRejectedProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x25, (byte) 0xC0, // Described-type, ulong type, rejected descriptor, list8.
                                     0x03, 0x03, 0x40, 0x00 }; // size (3), count (3), elements...

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 3");
    }

    @Test
    public void testRejectedIndicatesCountLargerThanElementsInRejectedProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x25, (byte) 0xC0,
                                     0x03, 0x03, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 3");
    }

    // Modified Outcome Tests (Descriptor: 0x27)

    @Test
    public void testModifiedIndicatesCountLargerThanElementsInModifiedProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x27, (byte) 0xC0, // Described-type, ulong type, modified descriptor, list8.
                                     0x05, 0x04, 0x42, 0x42, // size (5), count (4), elements...
                                     0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    @Test
    public void testModifiedIndicatesCountLargerThanElementsInModifiedProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x27, (byte) 0xC0,
                                     0x05, 0x04, 0x42, 0x42,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    // Source Terminus Tests (Descriptor: 0x28)

    @Test
    public void testSourceIndicatesCountLargerThanElementsInSourceProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x28, (byte) 0xC0, // Described-type, ulong type, source descriptor, list8.
                                     0x0D, 0x0C, 0x40, 0x00, // size (13), count (12), elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    @Test
    public void testSourceIndicatesCountLargerThanElementsInSourceProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x28, (byte) 0xC0,
                                     0x0D, 0x0C, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    // Target Terminus Tests (Descriptor: 0x29)

    @Test
    public void testTargetIndicatesCountLargerThanElementsInTargetProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x29, (byte) 0xC0, // Described-type, ulong type, target descriptor, list8.
                                     0x09, 0x08, 0x40, 0x00, // size (3), count (8), elements...
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 7 but got 8");
    }

    @Test
    public void testTargetIndicatesCountLargerThanElementsInTargetProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x29, (byte) 0xC0,
                                     0x09, 0x08, 0x40, 0x00,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 7 but got 8");
    }

    // Begin Performative Tests (Descriptor: 0x10)

    @Test
    public void testOpenOmitsContainerFB() throws IOException {
        // Provide the bytes for Open, but omit the mandatory container-id to provoke a decode error.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), container-id (null).

        doTestDecoderThrowsExpectedException(bytes, false,
            "The container-id field cannot be omitted from the Open");
    }

    @Test
    public void testOpenOmitsContainerFS() throws IOException {
        // Provide the bytes for Open, but omit the mandatory container-id to provoke a decode error.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), container-id (null).

        doTestDecoderThrowsExpectedException(bytes, true,
            "The container-id field cannot be omitted from the Open");
    }

    @Test
    public void testOpenWithList0EncodingFailsValidationFB() throws Exception {
        // Provide the bytes for Open, but indicate a body of List0
        byte[] bytes = new byte[] { 0x00, 0x53, 0x10, 0x45}; // Described-type, ulong type, open descriptor, list0.

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testOpenWithList0EncodingFailsValidationFS() throws Exception {
        // Provide the bytes for Open, but indicate a body of List0
        byte[] bytes = new byte[] { 0x00, 0x53, 0x10, 0x45}; // Described-type, ulong type, open descriptor, list0.

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testOpenIndicatesSizeLargerThanNeededFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list5.
                                     0x05, 0x01, (byte) 0xa1, 0x01, 0x41 }; // size (5), count (1), container-id (A).

        doTestDecoderThrowsExpectedException(bytes, false,
            "List encoded size is specified to be greater than the amount of data available s:(5) r:(4)");
    }

    @Test
    public void testOpenIndicatesSizeLargerThanNeededFs() throws IOException {
        streamDecoderState.setMaxListSize(4);

        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list5.
                                     0x05, 0x01, (byte) 0xa1, 0x01, 0x41 }; // size (5), count (1), container-id (A).

        doTestDecoderThrowsExpectedException(bytes, true,
            "List encoded size is specified to be greater than the configured maximum List size allowed s:(5) c:(4)");
    }

    @Test
    public void testOpenIndicatesCountLargerThanAvailableFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list5.
                                     0x04, 0x05, (byte) 0xa1, 0x01, 0x41 }; // size (4), count (5), container-id (A).

        doTestDecoderThrowsExpectedException(bytes, false,
            "List encoded count is specified to be greater than the reported encoded size s:(4) c:(5)");
    }

    @Test
    public void testOpenIndicatesCountLargerThanAvailableFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x04, 0x05, (byte) 0xa1, 0x01, 0x41 }; // size (4), count (5), container-id (A).

        doTestDecoderThrowsExpectedException(bytes, true,
            "List encoded count is specified to be greater than the reported encoded size s:(4) c:(5)");
    }

    @Test
    public void testOpenIndicatesCountLargerThanElementsInOpenProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x0C, 0x0B, (byte) 0xa1, 0x01, 0x41, // size (12), count (11), container-id (A).
                                     0x00, 0x00, 0x00, 0x00, 0x00,
                                     0x00, 0x00, 0x00 }; // Pad to allow for list count validation

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 10 but got 11");
    }

    @Test
    public void testOpenIndicatesCountLargerThanElementsInOpenProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x0C, 0x0B, (byte) 0xa1, 0x01, 0x41, // size (12), count (11), container-id (A).
                                     0x00, 0x00, 0x00, 0x00, 0x00,
                                     0x00, 0x00, 0x00 }; // Pad to allow for list count validation

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 10 but got 11");
    }

    @Test
    public void testOpenDecodeDoesNotUseIndicatedSizeBytesFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x10, (byte) 0xC0, // Described-type, ulong type, open descriptor, list8.
                                     0x05, 0x01, (byte) 0xa1, 0x01, 0x41, 0x00 }; // size (5), count (1), container-id (A).

        doTestDecoderThrowsExpectedException(bytes, false,
            "List decoding did not read the expected amount of bytes: 5");
    }

    // Begin Performative Tests (Descriptor: 0x11)

    @Test
    public void testBeginWithList0EncodingFailsValidationFB() throws Exception {
        // Provide the bytes for Begin, but indicate a body of List0
        byte[] bytes = new byte[] { 0x00, 0x53, 0x11, 0x45}; // Described-type, ulong type, begin descriptor, list0.

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 4 but got 0");
    }

    @Test
    public void testBeginWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x11, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 4 but got 0");
    }

    @Test
    public void testBeginIndicatesCountLargerThanElementsInBeginProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x11, (byte) 0xC0, // Described-type, ulong type, begin descriptor, list8.
                                     0x0A, 0x09, 0x40, 0x43, 0x43, 0x43, // size (10), count (9), fields...
                                     0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 8 but got 9");
    }

    @Test
    public void testBeginIndicatesCountLargerThanElementsInBeginProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x11, (byte) 0xC0,
                                     0x0A, 0x09, 0x40, 0x43, 0x43, 0x43,
                                     0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 8 but got 9");
    }

    // Attach Performative Tests (Descriptor: 0x12)

    @Test
    public void testAttachOmitsNameFB() throws IOException {
        // Provide the bytes for Attach, but omit the mandatory name field.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x12, (byte) 0xC0, // Described-type, ulong type, attach descriptor, list8.
                                     0x04, 0x03, 0x40, 0x40, 0x40 }; // size (3), count (1), name (null).

        doTestDecoderThrowsExpectedException(bytes, false,
            "The name field cannot be omitted from the Attach");
    }

    @Test
    public void testAttachOmitsNameFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x12, (byte) 0xC0,
                                     0x04, 0x03, 0x40, 0x40, 0x40 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "The name field cannot be omitted from the Attach");
    }

    @Test
    public void testAttachIndicatesCountLargerThanElementsInAttachProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x12, (byte) 0xC0,
                                     0x0F, 0x0F, (byte) 0xa1, 0x01, 0x41, // size (16), count (15), name (A)
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 14 but got 15");
    }

    @Test
    public void testAttachIndicatesCountLargerThanElementsInAttachProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x12, (byte) 0xC0,
                                     0x0F, 0x0F, (byte) 0xa1, 0x01, 0x41,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 14 but got 15");
    }

    // Flow Performative Tests (Descriptor: 0x13)

    @Test
    public void testFlowWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x13, 0x45};

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 4 but got 0");
    }

    @Test
    public void testFlowWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x13, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 4 but got 0");
    }

    @Test
    public void testFlowIndicatesCountLargerThanElementsInFlowProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x13, (byte) 0xC0,
                                     0x0D, 0x0C, 0x43, 0x43, 0x43, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    @Test
    public void testFlowIndicatesCountLargerThanElementsInFlowProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x13, (byte) 0xC0,
                                     0x0D, 0x0C, 0x43, 0x43, 0x43, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    // Transfer Performative Tests (Descriptor: 0x14)

    @Test
    public void testTransferWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x14, 0x45};

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testTransferWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x14, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testTransferIndicatesCountLargerThanElementsInTransferProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x14, (byte) 0xC0,
                                     0x0E, 0x0C, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    @Test
    public void testTransferIndicatesCountLargerThanElementsInTransferProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x14, (byte) 0xC0,
                                     0x0E, 0x0C, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 11 but got 12");
    }

    // Disposition Performative Tests (Descriptor: 0x15)

    @Test
    public void testDispositionWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x15, 0x45};

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 2 but got 0");
    }

    @Test
    public void testDispositionWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x15, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 2 but got 0");
    }

    @Test
    public void testDispositionIndicatesCountLargerThanElementsInDispositionProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x15, (byte) 0xC0,
                                     0x08, 0x07, 0x41, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 6 but got 7");
    }

    @Test
    public void testDispositionIndicatesCountLargerThanElementsInDispositionProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x15, (byte) 0xC0,
                                     0x08, 0x07, 0x41, 0x43,
                                     0x00, 0x00, 0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 6 but got 7");
    }

    // Detach Performative Tests (Descriptor: 0x16)

    @Test
    public void testDetachWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x16, 0x45};

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testDetachWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x16, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testDetachIndicatesCountLargerThanElementsInDetachProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x16, (byte) 0xC0,
                                     0x05, 0x04, 0x43,
                                     0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    @Test
    public void testDetachIndicatesCountLargerThanElementsInDetachProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x16, (byte) 0xC0,
                                     0x05, 0x04, 0x43,
                                     0x00, 0x00, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    // End Performative Tests (Descriptor: 0x17)

    @Test
    public void testEndIndicatesCountLargerThanElementsInEndProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x17, (byte) 0xC0,
                                     0x03, 0x02, 0x40,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testEndIndicatesCountLargerThanElementsInEndProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x17, (byte) 0xC0,
                                     0x03, 0x02, 0x40,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // Close Performative Tests (Descriptor: 0x18)

    @Test
    public void testCloseIndicatesCountLargerThanElementsInCloseProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x18, (byte) 0xC0,
                                     0x03, 0x02, 0x40,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testCloseIndicatesCountLargerThanElementsInCloseProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x18, (byte) 0xC0,
                                     0x03, 0x02, 0x40,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // Coordinator Performative Tests (Descriptor: 0x30)

    @Test
    public void testCoordinatorIndicatesCountLargerThanElementsInCoordinatorProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x30, (byte) 0xC0, // Described-type, ulong type, coordinator descriptor, list8.
                                     0x03, 0x02, 0x40, 0x00 }; // size (3), count (2), elements...

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testCoordinatorIndicatesCountLargerThanElementsInCoordinatorProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x30, (byte) 0xC0,
                                     0x03, 0x02, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // Declare Performative Tests (Descriptor: 0x31)

    @Test
    public void testDeclareIndicatesCountLargerThanElementsInDeclareProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x31, (byte) 0xC0, // Described-type, ulong type, declare descriptor, list8.
                                     0x03, 0x02, 0x40, 0x00 }; // size (3), count (2), elements...

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testDeclareIndicatesCountLargerThanElementsInDeclareProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x31, (byte) 0xC0,
                                     0x03, 0x02, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // Discharge Performative Tests (Descriptor: 0x32)

    @Test
    public void testDischargeOmitsTxnIdFB() throws IOException {
        // Provide bytes for Discharge, but omit the mandatory txn-id binary payload.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x32, (byte) 0xC0, // Described-type, ulong type, discharge descriptor, list8.
                                     0x04, 0x02, 0x40, 0x42, 0x00 }; // size (4), count (2), txn-id (null), fail (false).

        doTestDecoderThrowsExpectedException(bytes, false,
            "The txn-id field cannot be omitted from the Discharge");
    }

    @Test
    public void testDischargeOmitsTxnIdFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x32, (byte) 0xC0,
                                     0x04, 0x02, 0x40, 0x42, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "The txn-id field cannot be omitted from the Discharge");
    }

    @Test
    public void testDischargeWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x32, 0x45}; // list0 encoding

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testDischargeWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x32, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testDischargeIndicatesCountLargerThanElementsInDischargeProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x32, (byte) 0xC0,
                                     0x06, 0x03, (byte) 0xa0, 0x01, 0x00, 0x42, // size (6), count (3), txn-id, fail (false)
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    @Test
    public void testDischargeIndicatesCountLargerThanElementsInDischargeProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x32, (byte) 0xC0,
                                     0x06, 0x03, (byte) 0xa0, 0x01, 0x00, 0x42,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    // Declared Performative Tests (Descriptor: 0x33)

    @Test
    public void testDeclaredOmitsTxnIdFB() throws IOException {
        // Provide bytes for Declared, but omit the mandatory txn-id binary payload.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x33, (byte) 0xC0, // Described-type, ulong type, declared descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), txn-id (null).

        doTestDecoderThrowsExpectedException(bytes, false,
            "The txn-id field cannot be omitted from the Declared");
    }

    @Test
    public void testDeclaredOmitsTxnIdFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x33, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "The txn-id field cannot be omitted from the Declared");
    }

    @Test
    public void testDeclaredIndicatesCountLargerThanElementsInDeclaredProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x33, (byte) 0xC0,
                                     0x05, 0x02, (byte) 0xa0, 0x01, 0x00, // size (5), count (2), txn-id (binary)
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testDeclaredIndicatesCountLargerThanElementsInDeclaredProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x33, (byte) 0xC0,
                                     0x06, 0x02, (byte) 0xa0, 0x01, 0x00,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // TransactionalState Performative Tests (Descriptor: 0x34)

    @Test
    public void testTransactionalStateOmitsTxnIdFB() throws IOException {
        // Provide bytes for TransactionalState, but omit the mandatory txn-id binary payload.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x34, (byte) 0xC0, // Described-type, ulong type, transactional-state descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), txn-id (null).

        doTestDecoderThrowsExpectedException(bytes, false,
            "The txn-id field cannot be omitted from the TransactionalState");
    }

    @Test
    public void testTransactionalStateOmitsTxnIdFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x34, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "The txn-id field cannot be omitted from the TransactionalState");
    }

    @Test
    public void testTransactionalStateWithList0EncodingFailsValidationFB() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x34, 0x45};

        doTestDecoderThrowsExpectedException(bytes, false,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testTransactionalStateWithList0EncodingFailsValidationFS() throws Exception {
        byte[] bytes = new byte[] { 0x00, 0x53, 0x34, 0x45};

        doTestDecoderThrowsExpectedException(bytes, true,
            "Not enough list elements indicated in the encoded count, expected 1 but got 0");
    }

    @Test
    public void testTransactionalStateIndicatesCountLargerThanElementsInTransactionalStateProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x34, (byte) 0xC0,
                                     0x06, 0x03, (byte) 0xa0, 0x01, 0x00, 0x40, // size (7), count (3), txn-id, outcome (null)
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    @Test
    public void testTransactionalStateIndicatesCountLargerThanElementsInTransactionalStateProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x34, (byte) 0xC0,
                                     0x06, 0x03, (byte) 0xa0, 0x01, 0x00, 0x40,
                                     0x00 };

        doTestDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    protected void doTestDecoderThrowsExpectedException(byte[] encoding, boolean fromStream, String errorMessage) throws IOException {
        final ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().copy(encoding);
        final InputStream stream = new ProtonBufferInputStream(buffer.copy(true));

        try {
            if (fromStream) {
                streamDecoder.readObject(stream, streamDecoderState);
            } else {
                decoder.readObject(buffer, decoderState);
            }

            fail("Invalid encodings should throw a decode exception");
        } catch (DecodeException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }

    // SaslMechanisms Performative Tests (Descriptor: 0x40)

    @Test
    public void testSaslMechanismsOmitsMechanismsFB() throws IOException {
        // Provide bytes for SaslMechanisms, but omit the mandatory mechanisms array.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x40, (byte) 0xC0, // Described-type, ulong type, sasl-mechanisms descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), mechanisms (null).

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "The mechanisms field cannot be omitted from the SaslMechanisms");
    }

    @Test
    public void testSaslMechanismsOmitsMechanismsFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x40, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "The mechanisms field cannot be omitted from the SaslMechanisms");
    }

    @Test
    public void testSaslMechanismsIndicatesCountLargerThanElementsInSaslMechanismsProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x40, (byte) 0xC0,
                                     0x05, 0x02, (byte) 0xE0, 0x01, 0x00, // size (5), count (2), mechanisms (empty array)
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testSaslMechanismsIndicatesCountLargerThanElementsInSaslMechanismsProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x40, (byte) 0xC0,
                                     0x05, 0x02, (byte) 0xE0, 0x01, 0x00,
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // SaslInit Performative Tests (Descriptor: 0x41)

    @Test
    public void testSaslInitOmitsMechanismFB() throws IOException {
        // Provide bytes for SaslInit, but omit the mandatory mechanism string.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x41, (byte) 0xC0, // Described-type, ulong type, sasl-init descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), mechanism (null).

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "The mechanism field cannot be omitted from the SaslInit");
    }

    @Test
    public void testSaslInitOmitsMechanismFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x41, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "The mechanism field cannot be omitted from the SaslInit");
    }

    @Test
    public void testSaslInitIndicatesCountLargerThanElementsInSaslInitProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x41, (byte) 0xC0,
                                     0x05, 0x04, (byte) 0xa3, 0x01, 0x41, // size (5), count (4), mechanism (A)
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    @Test
    public void testSaslInitIndicatesCountLargerThanElementsInSaslInitProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x41, (byte) 0xC0,
                                     0x06, 0x04, (byte) 0xa3, 0x01, 0x41,
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 3 but got 4");
    }

    // SaslChallenge Performative Tests (Descriptor: 0x42)

    @Test
    public void testSaslChallengeOmitsChallengeFB() throws IOException {
        // Provide bytes for SaslChallenge, but omit the mandatory challenge binary payload.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x42, (byte) 0xC0, // Described-type, ulong type, sasl-challenge descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), challenge (null).

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "The challenge field cannot be omitted from the SaslChallenge");
    }

    @Test
    public void testSaslChallengeOmitsChallengeFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x42, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "The challenge field cannot be omitted from the SaslChallenge");
    }

    @Test
    public void testSaslChallengeIndicatesCountLargerThanElementsInSaslChallengeProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x42, (byte) 0xC0,
                                     0x05, 0x02, (byte) 0xa0, 0x01, 0x00, // size (5), count (2), challenge (binary)
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testSaslChallengeIndicatesCountLargerThanElementsInSaslChallengeProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x42, (byte) 0xC0,
                                     0x05, 0x02, (byte) 0xa0, 0x01, 0x00,
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // SaslResponse Performative Tests (Descriptor: 0x43)

    @Test
    public void testSaslResponseOmitsResponseFB() throws IOException {
        // Provide bytes for SaslResponse, but omit the mandatory response binary payload.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x43, (byte) 0xC0, // Described-type, ulong type, sasl-response descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), response (null).

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "The response field cannot be omitted from the SaslResponse");
    }

    @Test
    public void testSaslResponseOmitsResponseFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x43, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "The response field cannot be omitted from the SaslResponse");
    }

    @Test
    public void testSaslResponseIndicatesCountLargerThanElementsInSaslResponseProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x43, (byte) 0xC0,
                                     0x06, 0x02, (byte) 0xa0, 0x01, 0x00, // size (6), count (2), response (binary)
                                     0x00, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    @Test
    public void testSaslResponseIndicatesCountLargerThanElementsInSaslResponseProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x43, (byte) 0xC0,
                                     0x06, 0x02, (byte) 0xa0, 0x01, 0x00,
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 1 but got 2");
    }

    // SaslOutcome Performative Tests (Descriptor: 0x44)

    @Test
    public void testSaslOutcomeOmitsCodeFB() throws IOException {
        // Provide bytes for SaslOutcome, but omit the mandatory outcome code.
        byte[] bytes = new byte[] {  0x00, 0x53, 0x44, (byte) 0xC0, // Described-type, ulong type, sasl-outcome descriptor, list8.
                                     0x03, 0x01, 0x40, 0x00 }; // size (3), count (1), code (null).

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "The code field cannot be omitted from the SaslOutcome");
    }

    @Test
    public void testSaslOutcomeOmitsCodeFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x44, (byte) 0xC0,
                                     0x03, 0x01, 0x40, 0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "The code field cannot be omitted from the SaslOutcome");
    }

    @Test
    public void testSaslOutcomeIndicatesCountLargerThanElementsInSaslOutcomeProperFB() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x44, (byte) 0xC0,
                                     0x04, 0x03, 0x54, 0x00, // size (4), count (3), code (ubyte 0)
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, false,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    @Test
    public void testSaslOutcomeIndicatesCountLargerThanElementsInSaslOutcomeProperFS() throws IOException {
        byte[] bytes = new byte[] {  0x00, 0x53, 0x44, (byte) 0xC0,
                                     0x05, 0x03, 0x54, 0x00,
                                     0x00 };

        doTestSaslDecoderThrowsExpectedException(bytes, true,
            "To many elements indicated in the encoded count, maximum 2 but got 3");
    }

    protected void doTestSaslDecoderThrowsExpectedException(byte[] encoding, boolean fromStream, String errorMessage) throws IOException {
        final ProtonBuffer buffer = ProtonBufferAllocator.defaultAllocator().copy(encoding);
        final InputStream stream = new ProtonBufferInputStream(buffer.copy(true));

        try {
            if (fromStream) {
                saslStreamDecoder.readObject(stream, streamDecoderState);
            } else {
                saslDecoder.readObject(buffer, decoderState);
            }

            fail("Invalid encodings should throw a decode exception");
        } catch (DecodeException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }
}
