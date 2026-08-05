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
package org.apache.qpid.protonj2.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SymbolTest {

    private final String LARGE_SYMBOL_VALUE = "Large String: " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog. " +
        "The quick brown fox jumps over the lazy dog.";


    @Test
    public void testGetSymbolWithNullString() {
        assertNull(Symbol.getSymbol((String) null));
    }

    @Test
    public void testGetSymbolWithNullBuffer() {
        assertNull(Symbol.getSymbol((ProtonBuffer) null));
    }

    @Test
    public void testGetSymbolWithEmptyString() {
        assertNotNull(Symbol.getSymbol(""));
        assertSame(Symbol.getSymbol(""), Symbol.getSymbol(""));
    }

    @Test
    public void testGetSymbolWithEmptyBuffer() {
        assertNotNull(Symbol.getSymbol(ProtonBufferAllocator.defaultAllocator().allocate(0)));
        assertSame(Symbol.getSymbol(ProtonBufferAllocator.defaultAllocator().allocate(0)),
                   Symbol.getSymbol(ProtonBufferAllocator.defaultAllocator().allocate(0)));
    }

    @Test
    public void testCompareTo() {
        String symbolString1 = "Symbol-1";
        String symbolString2 = "Symbol-2";
        String symbolString3 = "Symbol-3";

        Symbol symbol1 = Symbol.getSymbol(symbolString1);
        Symbol symbol2 = Symbol.getSymbol(symbolString2);
        Symbol symbol3 = Symbol.getSymbol(symbolString3);

        assertEquals(0, symbol1.compareTo(symbol1));
        assertEquals(0, symbol2.compareTo(symbol2));
        assertEquals(0, symbol3.compareTo(symbol3));

        assertTrue(symbol2.compareTo(symbol1) > 0);
        assertTrue(symbol3.compareTo(symbol1) > 0);
        assertTrue(symbol3.compareTo(symbol2) > 0);

        assertTrue(symbol1.compareTo(symbol2) < 0);
        assertTrue(symbol1.compareTo(symbol3) < 0);
        assertTrue(symbol2.compareTo(symbol3) < 0);
    }

    @Test
    public void testEquals() {
        String symbolString1 = "Symbol-1";
        String symbolString2 = "Symbol-2";
        String symbolString3 = "Symbol-3";

        Symbol symbol1 = Symbol.getSymbol(symbolString1);
        Symbol symbol2 = Symbol.getSymbol(symbolString2);
        Symbol symbol3 = Symbol.getSymbol(symbolString3);

        assertNotEquals(symbol1, symbol2);

        assertEquals(symbolString1, symbol1.toString());
        assertEquals(symbolString2, symbol2.toString());
        assertEquals(symbolString3, symbol3.toString());

        assertNotEquals(symbol1, symbol2);
        assertNotEquals(symbol2, symbol3);
        assertNotEquals(symbol3, symbol1);

        assertNotEquals(symbolString1, symbol1);
        assertNotEquals(symbolString2, symbol2);
        assertNotEquals(symbolString3, symbol3);
    }

    @Test
    public void testHashcode() {
        String symbolString1 = "Symbol-1";
        String symbolString2 = "Symbol-2";

        Symbol symbol1 = Symbol.getSymbol(symbolString1);
        Symbol symbol2 = Symbol.getSymbol(symbolString2);

        assertNotEquals(symbol1, symbol2);
        assertNotEquals(symbol1.hashCode(), symbol2.hashCode());

        assertEquals(symbol1.hashCode(), Symbol.getSymbol(symbolString1).hashCode());
        assertEquals(symbol2.hashCode(), Symbol.getSymbol(symbolString2).hashCode());
        assertEquals(symbol1.hashCode(), Symbol.getSASLSymbol(symbolString1).hashCode());
        assertEquals(symbol2.hashCode(), Symbol.getSASLSymbol(symbolString2).hashCode());
    }

    @Test
    public void testValueOf() {
        String symbolString1 = "Symbol-1";
        String symbolString2 = "Symbol-2";

        Symbol symbol1 = Symbol.valueOf(symbolString1);
        Symbol symbol2 = Symbol.getSymbol(symbolString2);

        assertNotEquals(symbol1, symbol2);

        assertEquals(symbolString1, symbol1.toString());
        assertEquals(symbolString2, symbol2.toString());
    }

    @Test
    public void testValueOfProducesSingleton() {
        String symbolString = "Symbol-String";

        Symbol symbol1 = Symbol.valueOf(symbolString);
        Symbol symbol2 = Symbol.getSymbol(symbolString);

        assertEquals(symbolString, symbol1.toString());
        assertEquals(symbolString, symbol2.toString());

        assertSame(symbol1, symbol2);
    }

    @Test
    public void testGetSymbol() {
        String symbolString1 = "Symbol-1";
        String symbolString2 = "Symbol-2";

        Symbol symbol1 = Symbol.getSymbol(symbolString1);
        Symbol symbol2 = Symbol.getSymbol(symbolString2);

        assertNotEquals(symbol1, symbol2);

        assertEquals(symbolString1, symbol1.toString());
        assertEquals(symbolString2, symbol2.toString());
    }

    @Test
    public void testGetSymbolProducesSingleton() {
        String symbolString = "Symbol-String";

        Symbol symbol1 = Symbol.getSymbol(symbolString);
        Symbol symbol2 = Symbol.getSymbol(symbolString);

        assertEquals(symbolString, symbol1.toString());
        assertEquals(symbolString, symbol2.toString());

        assertSame(symbol1, symbol2);
    }

    @Test
    public void testGetSymbolAndValueOfProduceSingleton() {
        String symbolString = "Symbol-String";

        Symbol symbol1 = Symbol.valueOf(symbolString);
        Symbol symbol2 = Symbol.getSymbol(symbolString);

        assertEquals(symbolString, symbol1.toString());
        assertEquals(symbolString, symbol2.toString());

        assertSame(symbol1, symbol2);
    }

    @Test
    public void testToStringProducesSingleton() {
        String symbolString = "Symbol-String";

        Symbol symbol1 = Symbol.getSymbol(symbolString);
        Symbol symbol2 = Symbol.getSymbol(symbolString);

        assertEquals(symbolString, symbol1.toString());
        assertEquals(symbolString, symbol2.toString());

        assertSame(symbol1, symbol2);
        assertSame(symbol1.toString(), symbol2.toString());
    }

    @Test
    public void testLargeSymbolNotCached() {
        Symbol symbol1 = Symbol.getSymbol(LARGE_SYMBOL_VALUE);
        Symbol symbol2 = Symbol.getSymbol(
            ProtonBufferAllocator.defaultAllocator().copy(LARGE_SYMBOL_VALUE.getBytes(StandardCharsets.US_ASCII)));

        assertNotSame(symbol1, symbol2);
        assertNotSame(symbol1.toString(), symbol2.toString());
    }

    @Test
    public void testLargeSASLSymbolNotCached() {
        Symbol symbol1 = Symbol.getSASLSymbol(LARGE_SYMBOL_VALUE);
        Symbol symbol2 = Symbol.getSASLSymbol(
            ProtonBufferAllocator.defaultAllocator().copy(LARGE_SYMBOL_VALUE.getBytes(StandardCharsets.US_ASCII)));

        assertNotSame(symbol1, symbol2);
        assertNotSame(symbol1.toString(), symbol2.toString());
    }

    @Test
    public void testGetSymbols() {
        String[] symbolStrings = new String[] { "one", "two", "three" };

        Symbol[] symbols1 = Symbols.getSymbols(symbolStrings);
        Symbol[] symbols2 = Symbols.getSymbols(symbolStrings);

        assertEquals(symbolStrings.length, symbols1.length);
        assertEquals(symbolStrings.length, symbols2.length);
        assertArrayEquals(symbols1, symbols2);

        for (int i = 0; i < symbolStrings.length; ++i) {
            assertSame(symbols1[i], symbols2[i]);
        }
    }

    @Test
    public void testGetSASLSymbols() {
        String[] symbolStrings = new String[] { "one", "two", "three" };

        Symbol[] symbols1 = Symbols.getSASLSymbols(symbolStrings);
        Symbol[] symbols2 = Symbols.getSASLSymbols(symbolStrings);

        assertEquals(symbolStrings.length, symbols1.length);
        assertEquals(symbolStrings.length, symbols2.length);
        assertArrayEquals(symbols1, symbols2);

        for (int i = 0; i < symbolStrings.length; ++i) {
            assertSame(symbols1[i], symbols2[i]);
        }
    }

    @Test
    @Disabled("Cache is static so running has side effects.")
    public void testGetSymbolsCachingMaxesOut() {
        boolean notSame = false;
        int attempts = 0;

        for (attempts = 0; attempts < Short.MAX_VALUE; ++attempts) {
            final Symbol symbol1 = Symbol.getSymbol(String.valueOf(attempts));
            final Symbol symbol2 = Symbol.getSymbol(String.valueOf(attempts));

            if (symbol1 != symbol2) {
                notSame = true;
                break;
            }
        }

        assertTrue(notSame);
        assertTrue(attempts > Byte.MAX_VALUE);
    }

    @Test
    @Disabled("Cache is static so running has side effects.")
    public void testGetSASLSymbolsCachingMaxesOut() {
        boolean notSame = false;
        int attempts = 0;

        for (attempts = 0; attempts < Short.MAX_VALUE; ++attempts) {
            final Symbol symbol1 = Symbol.getSASLSymbol(String.valueOf(attempts));
            final Symbol symbol2 = Symbol.getSASLSymbol(String.valueOf(attempts));

            if (symbol1 != symbol2) {
                notSame = true;
                break;
            }
        }

        assertTrue(notSame);
        assertTrue(attempts > Byte.MAX_VALUE);
    }

    @Test
    @Disabled("Cache is static so running has side effects.")
    public void testGetSymbolFromBufferCachingMaxesOut() {
        boolean notSame = false;
        int attempts = 0;

        for (attempts = 0; attempts < Short.MAX_VALUE; ++attempts) {
            final ProtonBuffer bytes =
                ProtonBufferAllocator.defaultAllocator().copy(String.valueOf(attempts).getBytes(StandardCharsets.US_ASCII));

            final Symbol symbol1 = Symbol.getSymbol(bytes);
            final Symbol symbol2 = Symbol.getSymbol(bytes);

            if (symbol1 != symbol2) {
                notSame = true;
                break;
            }
        }

        assertTrue(notSame);
        assertTrue(attempts > Byte.MAX_VALUE);
    }

    @Test
    @Disabled("Cache is static so running has side effects.")
    public void testGetSASLSymbolFromBufferCachingMaxesOut() {
        boolean notSame = false;
        int attempts = 0;

        for (attempts = 0; attempts < Short.MAX_VALUE; ++attempts) {
            final ProtonBuffer bytes =
                ProtonBufferAllocator.defaultAllocator().copy(String.valueOf(attempts).getBytes(StandardCharsets.US_ASCII));

            final Symbol symbol1 = Symbol.getSASLSymbol(bytes);
            final Symbol symbol2 = Symbol.getSASLSymbol(bytes);

            if (symbol1 != symbol2) {
                notSame = true;
                break;
            }
        }

        assertTrue(notSame);
        assertTrue(attempts > Byte.MAX_VALUE);
    }

    @Test
    public void testGetSymbolsWithNullOrEmptyArg() {
        assertNull(Symbols.getSymbols(null));
        assertNotNull(Symbols.getSymbols(new String[0]));
    }

    @Test
    public void testSymbolsArrayContains() {
        final Symbol[] symbols = new Symbol[] { Symbol.getSymbol("one"), Symbol.getSymbol("two"), Symbol.getSymbol("three") };

        assertThrows(NullPointerException.class, () -> Symbols.contains(symbols, (String)null));
        assertThrows(NullPointerException.class, () -> Symbols.contains(symbols, (Symbol)null));

        assertTrue(Symbols.contains(symbols, "one"));
        assertTrue(Symbols.contains(symbols, "three"));
        assertTrue(Symbols.contains(symbols, "two"));
        assertFalse(Symbols.contains(symbols, "four"));

        assertTrue(Symbols.contains(symbols, Symbol.getSymbol("one")));
        assertTrue(Symbols.contains(symbols, Symbol.getSymbol("two")));
        assertTrue(Symbols.contains(symbols, Symbol.getSymbol("three")));
        assertFalse(Symbols.contains(symbols, Symbol.getSymbol("four")));

        assertFalse(Symbols.contains(null, "four"));
        assertFalse(Symbols.contains(new Symbol[0], "four"));
    }

    @Test
    public void testSASLSymbolsArrayContains() {
        final Symbol[] symbols = new Symbol[] { Symbol.getSASLSymbol("one"), Symbol.getSASLSymbol("two"), Symbol.getSASLSymbol("three") };

        assertThrows(NullPointerException.class, () -> Symbols.contains(symbols, (String)null));
        assertThrows(NullPointerException.class, () -> Symbols.contains(symbols, (Symbol)null));

        assertTrue(Symbols.contains(symbols, "one"));
        assertTrue(Symbols.contains(symbols, "three"));
        assertTrue(Symbols.contains(symbols, "two"));
        assertFalse(Symbols.contains(symbols, "four"));

        assertTrue(Symbols.contains(symbols, Symbol.getSASLSymbol("one")));
        assertTrue(Symbols.contains(symbols, Symbol.getSASLSymbol("two")));
        assertTrue(Symbols.contains(symbols, Symbol.getSASLSymbol("three")));
        assertFalse(Symbols.contains(symbols, Symbol.getSASLSymbol("four")));

        assertFalse(Symbols.contains(null, "four"));
        assertFalse(Symbols.contains(new Symbol[0], "four"));
    }

    @Test
    public void testGetSymbolAndGetSASLSymbolUseDifferentCaches() {
        String symbolString = "Symbol-String";

        Symbol symbol1 = Symbol.getSymbol(symbolString);
        Symbol symbol2 = Symbol.getSymbol(symbolString);

        Symbol symbol3 = Symbol.getSASLSymbol(symbolString);
        Symbol symbol4 = Symbol.getSASLSymbol(symbolString);

        assertEquals(symbolString, symbol1.toString());
        assertEquals(symbolString, symbol2.toString());
        assertEquals(symbolString, symbol3.toString());
        assertEquals(symbolString, symbol4.toString());

        assertSame(symbol1, symbol2);
        assertSame(symbol3, symbol4);

        assertNotSame(symbol1, symbol3);
        assertNotSame(symbol2, symbol4);
    }
}
