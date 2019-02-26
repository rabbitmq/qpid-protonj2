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
package org.apache.qpid.proton4j.amqp.transport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.qpid.proton4j.amqp.Symbol;
import org.apache.qpid.proton4j.amqp.UnsignedLong;

public final class ErrorCondition {

    public static final UnsignedLong DESCRIPTOR_CODE = UnsignedLong.valueOf(0x000000000000001dL);
    public static final Symbol DESCRIPTOR_SYMBOL = Symbol.valueOf("amqp:error:list");

    private Symbol condition;
    private String description;
    private Map<Object, Object> info;

    public ErrorCondition() {
    }

    public ErrorCondition(Symbol condition, String description) {
        this.condition = condition;
        this.description = description;
    }

    public Symbol getCondition() {
        return condition;
    }

    public ErrorCondition setCondition(Symbol condition) {
        if (condition == null) {
            throw new NullPointerException("the condition field is mandatory");
        }

        this.condition = condition;

        return this;
    }

    public String getDescription() {
        return description;
    }

    public ErrorCondition setDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<Object, Object> getInfo() {
        return info;
    }

    public ErrorCondition setInfo(Map<Object, Object> info) {
        this.info = info;
        return this;
    }

    public ErrorCondition clear() {
        condition = null;
        description = null;
        info = null;

        return this;
    }

    public boolean isEmpty() {
        return condition == null;
    }

    public ErrorCondition copy() {
        ErrorCondition copy = new ErrorCondition();

        copy.setCondition(condition);
        copy.setDescription(description);
        copy.setInfo(info == null ? null : new LinkedHashMap<>(info));

        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ErrorCondition that = (ErrorCondition) o;

        if (condition != null ? !condition.equals(that.condition) : that.condition != null) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (info != null ? !info.equals(that.info) : that.info != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = condition != null ? condition.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Error{" +
               "condition=" + condition +
               ", description='" + description + '\'' +
               ", info=" + info +
               '}';
    }
}
