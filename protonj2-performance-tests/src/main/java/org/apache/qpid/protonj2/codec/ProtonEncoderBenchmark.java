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
package org.apache.qpid.protonj2.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.types.Symbol;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ProtonEncoderBenchmark {

    public static final int DEFAULT_BUFFER_SIZE = 16384;

    @State(Scope.Benchmark)
    public static class CodecState {

        protected ProtonBuffer buffer;
        protected Encoder encoder = CodecFactory.getDefaultEncoder();
        protected EncoderState encoderState = encoder.newEncoderState();
        protected Decoder decoder = CodecFactory.getDefaultDecoder();
        protected DecoderState decoderState = decoder.newDecoderState();

        protected final Map<Symbol, Object> testMap = new HashMap<>();
        protected final List<Symbol> testList = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() {
            buffer = ProtonBufferAllocator.defaultAllocator().allocate(DEFAULT_BUFFER_SIZE);

            testMap.put(Symbol.getSymbol("key-1"), "test-1");
            testMap.put(Symbol.getSymbol("key-2"), "test-2");
            testMap.put(Symbol.getSymbol("key-3"), "test-3");
            testMap.put(Symbol.getSymbol("key-4"), "test-4");
            testMap.put(Symbol.getSymbol("key-5"), "test-5");

            testList.add(Symbol.getSymbol("key-1"));
            testList.add(Symbol.getSymbol("key-2"));
            testList.add(Symbol.getSymbol("key-3"));
            testList.add(Symbol.getSymbol("key-4"));
            testList.add(Symbol.getSymbol("key-5"));
        }

        @Setup(Level.Iteration)
        public void reset() {
            buffer.clear();
        }
    }

    @Benchmark
    public void getTypeEncoderOfMap(CodecState codec, Blackhole bh) {
        bh.consume(codec.encoder.getTypeEncoder(Map.class));
    }

    @Benchmark
    public void encodeMapOfSymbolsAndStrings(CodecState codec, Blackhole bh) {
        codec.encoder.writeMap(codec.buffer, codec.encoderState, codec.testMap);
    }

    @Benchmark
    public void encodeListOfSymbols(CodecState codec, Blackhole bh) {
        codec.encoder.writeList(codec.buffer, codec.encoderState, codec.testList);
    }

    public static void main(String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(ProtonEncoderBenchmark.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .shouldDoGC(true)
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
