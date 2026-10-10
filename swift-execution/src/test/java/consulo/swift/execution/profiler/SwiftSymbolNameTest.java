/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.swift.execution.profiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Symbols are what {@code swift-demangle --compact --simplified} printed for the demo package.
 */
public class SwiftSymbolNameTest {
    @Test
    public void testMemberFunction() {
        assertEquals(new SwiftSymbolName("Point", "moved", true), SwiftSymbolName.parse("Point.moved(by:)"));
    }

    @Test
    public void testTopLevelFunction() {
        assertEquals(new SwiftSymbolName(null, "greet", true), SwiftSymbolName.parse("greet()"));
    }

    @Test
    public void testStaticAndGenerics() {
        assertEquals(new SwiftSymbolName("Main", "main", true), SwiftSymbolName.parse("static Main.main()"));
        assertEquals(new SwiftSymbolName("Box", "area", true), SwiftSymbolName.parse("Box<A>.area()"));
        assertEquals(new SwiftSymbolName("Box", "map", true), SwiftSymbolName.parse("generic specialization <Swift.Int> of Box<A>.map<B>(_:)"));
    }

    @Test
    public void testClosureNavigatesToItsFunction() {
        assertEquals(new SwiftSymbolName(null, "greet", true), SwiftSymbolName.parse("closure #1 in greet()"));
    }

    @Test
    public void testInitializerAndAccessorNavigateToType() {
        assertEquals(new SwiftSymbolName("Point", null, false), SwiftSymbolName.parse("Point.init(x:y:)"));
        assertEquals(new SwiftSymbolName("Point", "x", false), SwiftSymbolName.parse("Point.x.getter"));
    }

    @Test
    public void testNotSwift() {
        assertNull(SwiftSymbolName.parse("init()"));
        assertNull(SwiftSymbolName.parse(""));
    }
}
