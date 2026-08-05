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

package org.apache.qpid.protonj2.engine.util;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
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
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class UnsettledMapBenchmark {

    // ---------- Read-mostly scenarios (no mutation) ----------

    @State(Scope.Benchmark)
    public static class ReadState {

        /**
         * Tests with larger backlog or loads can show points where time to
         * scan becomes a problem.
         */
        @Param({ "2048" })
        public int elementCount;

        /**
         * Internal loop count. JMH can normalize scoring if you choose to
         * add @OperationsPerInvocation(PROBES) (constant), but we keep it param-driven.
         */
        @Param({ "1024" })
        public int probes;

        public UnsettledMap<DeliveryType> map;
        public DeliveryType[] deliveries;
        public int[] keys;

        public int hotKey;      // readOffset fast-path when using first element
        public int midKey;      // tends to trigger in-bucket search
        public int farKey;      // last element; in multi-bucket case forces scans

        @Setup(Level.Trial)
        public void setup() {
            map = new UnsettledMap<>(DeliveryType::getDeliveryId);
            keys = new int[elementCount];
            deliveries = new DeliveryType[elementCount];
            for (int i = 0; i < elementCount; ++i) {
                deliveries[i] = new DeliveryType(i);
                map.put(i, deliveries[i]); // primitive put
                keys[i] = i;
            }

            hotKey = 0;
            midKey = elementCount >>> 1;
            farKey = elementCount - 1;
        }
    }

    /** get(): fast-path hit (bucket checks deliveryIds[readOffset] first) */
    @Benchmark
    public void get_hot_first(ReadState s, Blackhole bh) {
        for (int i = 0; i < s.probes; ++i) {
            bh.consume(s.map.get(s.hotKey));
        }
    }

    /** get(): in-bucket search path (linear or binary depending on span) */
    @Benchmark
    public void get_mid_in_bucket(ReadState s, Blackhole bh) {
        for (int i = 0; i < s.probes; ++i) {
            bh.consume(s.map.get(s.midKey));
        }
    }

    /** get(): in multi-bucket case, forces cross-bucket scan starting from tail */
    @Benchmark
    public void get_far_cross_bucket(ReadState s, Blackhole bh) {
        for (int i = 0; i < s.probes; ++i) {
            bh.consume(s.map.get(s.farKey));
        }
    }


    @Benchmark
    public void get_sequential_access(ReadState state, Blackhole bh) {
        for (int i = 0; i < state.elementCount; i++) {
            bh.consume(state.map.get(i));
        }
    }

    @Benchmark
    public void get_reverse_sequential(ReadState state, Blackhole bh) {
        for (int i = state.elementCount - 1; i >= 0; i--) {
            bh.consume(state.map.get(i));
        }
    }

    /** containsKey(): same scanning mechanics as get(), but boolean return */
    @Benchmark
    public void containsKey_hot_first(ReadState s, Blackhole bh) {
        for (int i = 0; i < s.probes; ++i) {
            bh.consume(s.map.containsKey(s.hotKey));
        }
    }

    @Benchmark
    public void containsKey_far_cross_bucket(ReadState s, Blackhole bh) {
        for (int i = 0; i < s.probes; ++i) {
            bh.consume(s.map.containsKey(s.farKey));
        }
    }

    @Benchmark
    public void containsValue_hit_end(ReadState state, Blackhole bh) {
        final int[] keys = state.keys;
        DeliveryType target = state.map.get(keys[keys.length - 1]);

        bh.consume(state.map.containsValue(target));
    }

    /** forEach(): full traversal over active buckets and [readOffset..writeOffset) */
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS) // traversal is larger-grain than single gets
    public void forEach_all(ReadState s, Blackhole bh) {
        s.map.forEach(bh::consume);
    }


    @Benchmark
    public void forEach_range_cross_bucket(ReadState state, Blackhole bh) {
        final int[] keys = state.keys;

        // Range spanning multiple buckets (smaller slice)
        int start = keys[keys.length / 3];
        int end = keys[(keys.length / 3) + (keys.length / 4)];

        state.map.forEach(start, end, d -> bh.consume(d));
    }

    // ---------- Mutating scenarios, without per-invocation setup ----------
    // We keep each benchmark invocation "fresh" by rotating through a pool of prebuilt maps.

    @State(Scope.Thread)
    public static class MutatePoolState {

        @Param({ "2048" })
        public int elementCount;

        /**
         * Pool size: each invocation consumes a different prebuilt map.
         * Keep small to constrain memory/time but large enough for steady-state.
         */
        @Param({ "16" })
        public int poolSize;

        public UnsettledMap<DeliveryType>[] pool;
        public int cursor;

        // Range chosen to span multiple full buckets (default bucket size = 256).
        // Remove buckets 1..(n-2) to encourage bucket recycling.
        public int first;
        public int last;

        @Setup(Level.Iteration)
        @SuppressWarnings("unchecked")
        public void setupPool() {
            pool = (UnsettledMap<DeliveryType>[]) new UnsettledMap<?>[poolSize];
            cursor = 0;

            // Remove a middle band: [256 .. elementCount-257] (inclusive)
            // which is aligned to bucket boundaries for the default bucket size.
            first = 256;
            last = elementCount - 257;

            for (int p = 0; p < poolSize; ++p) {
                pool[p] = buildFilledMap(elementCount);
            }
        }

        private static UnsettledMap<DeliveryType> buildFilledMap(int count) {
            final UnsettledMap<DeliveryType> map = new UnsettledMap<>(DeliveryType::getDeliveryId);
            for (int i = 0; i < count; ++i) {
                // Allocation is part of many real call-paths, but if you want
                // map-only costs you can preallocate and reuse objects.
                map.put(i, new DeliveryType(i));
            }
            return map;
        }

        public UnsettledMap<DeliveryType> nextMapAndReplace() {
            final int idx = cursor++;
            final int slot = idx % poolSize;
            final UnsettledMap<DeliveryType> m = pool[slot];
            // Replace consumed map with a fresh one for the next time we hit this slot.
            pool[slot] = buildFilledMap(elementCount);
            return m;
        }
    }

    /** removeEach(): ranged remove that spans buckets, exercises removeRange/recycle paths */
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void removeEach_ranged_bucket_spanning(MutatePoolState s, Blackhole bh) {
        final UnsettledMap<DeliveryType> m = s.nextMapAndReplace();
        m.removeEach(s.first, s.last, bh::consume);
    }

    // ---------- Put-focused scenario ----------

    @State(Scope.Thread)
    public static class PutState {

        @Param({ "2048" })
        public int elementCount;

        public UnsettledMap<DeliveryType> map;
        public DeliveryType[] deliveries;

        @Setup(Level.Iteration)
        public void setup() {
            deliveries = new DeliveryType[elementCount];
            for (int i = 0; i < elementCount; ++i) {
                deliveries[i] = new DeliveryType(i);
            }
            map = new UnsettledMap<>(DeliveryType::getDeliveryId);
        }

        @Setup(Level.Invocation)
        public void clear() {
            // NOTE: If you want to avoid Level.Invocation entirely, use the same pool trick
            // as MutatePoolState. Kept here because "clear + fill" is larger-grain.
            map.clear();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void put_sequential_fill(PutState s) {
        for (int i = 0; i < s.elementCount; ++i) {
            s.map.put(i, s.deliveries[i]);
        }
    }

    // ---------- Remove-focused scenario ----------

    @State(Scope.Thread)
    public static class RemoveState {

        @Param({ "2048" })
        public int elementCount;

        public UnsettledMap<DeliveryType> map;

        @Setup(Level.Iteration)
        public void setup() {
            map = new UnsettledMap<>(DeliveryType::getDeliveryId);

            for (int i = 0; i < elementCount; ++i) {
                map.put(i, new DeliveryType(i));
            }
        }
    }

    @Benchmark
    public void removeFromTail(RemoveState s, Blackhole bh) {
        // remove oldest entries in order
        for (int i = 0; i < s.elementCount; ++i) {
            bh.consume(s.map.remove(i));
        }
    }

    @Benchmark
    public void removeFromHead(RemoveState s, Blackhole bh) {
        for (int i = s.elementCount - 1; i >= 0; --i) {
            bh.consume(s.map.remove(i));
        }
    }

    @Benchmark
    public void removeMiddleConstant(RemoveState s, Blackhole bh) {
        final int key = s.elementCount / 2;

        for (int i = 0; i < s.elementCount / 4; ++i) {
            bh.consume(s.map.remove(key));
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void removeWithCompactionPressure(RemoveState s, Blackhole bh) {
        int start = s.elementCount / 4;
        int end = (s.elementCount * 3) / 4;

        for (int i = start; i < end; ++i) {
            bh.consume(s.map.remove(i));
        }
    }

    @Benchmark
    public void removeAlternatingEnds(RemoveState s, Blackhole bh) {
        int low = 0;
        int high = s.elementCount - 1;

        for (int i = 0; i < s.elementCount / 2; ++i) {
            bh.consume(s.map.remove(low++));
            bh.consume(s.map.remove(high--));
        }
    }

    // ---------- Lightweight value type ----------

    public static final class DeliveryType {

        private final int deliveryId;

        public DeliveryType(int deliveryId) {
            this.deliveryId = deliveryId;
        }

        public int getDeliveryId() {
            return deliveryId;
        }

        @Override
        public int hashCode() {
            return deliveryId;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof DeliveryType) && ((DeliveryType) o).deliveryId == deliveryId;
        }

        @Override
        public String toString() {
            return "DeliveryType: { " + deliveryId + " }";
        }
    }

    // ---------- Runner (matches your existing style) ----------

    public static void main(String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(UnsettledMapBenchmark.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .shouldDoGC(true)
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
