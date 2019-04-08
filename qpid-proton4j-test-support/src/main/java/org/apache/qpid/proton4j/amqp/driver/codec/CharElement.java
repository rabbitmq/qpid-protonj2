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
package org.apache.qpid.proton4j.amqp.driver.codec;

import org.apache.qpid.proton4j.buffer.ProtonBuffer;

class CharElement extends AtomicElement<Integer> {

    private final int value;

    CharElement(Element<?> parent, Element<?> prev, int i) {
        super(parent, prev);
        value = i;
    }

    @Override
    public int size() {
        return isElementOfArray() ? 4 : 5;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public Data.DataType getDataType() {
        return Data.DataType.CHAR;
    }

    @Override
    public int encode(ProtonBuffer buffer) {
        final int size = size();
        if (size <= buffer.getWritableBytes()) {
            if (size == 5) {
                buffer.writeByte((byte) 0x73);
            }
            buffer.writeInt(value);
        }
        return 0;
    }
}
