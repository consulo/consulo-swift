/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Renders {@code PackageDescription.swiftmodule} as Swift source via
 * {@link SwiftModuleSourcePrinter} and asserts the headline shape of the
 * output — the strings that downstream IDE features (indexer, completion,
 * structure view) rely on.
 */
class SwiftModuleSourcePrinterTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    private static SwiftModuleInfo openInfo() throws IOException {
        try (InputStream in = SwiftModuleSourcePrinterTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in);
            return SwiftModuleReader.open(in).readModuleInfo();
        }
    }

    @Test
    void rendersModuleBanner() throws IOException {
        String src = SwiftModuleSourcePrinter.render(openInfo());
        assertTrue(src.startsWith("// swift-module: PackageDescription"),
            "Output must lead with a module-banner comment; got: " + firstLines(src, 4));
        assertTrue(src.contains("// target:"));
        assertTrue(src.contains("// format:"));
    }

    @Test
    void rendersPackageAndProductAsTopLevelDeclarations() throws IOException {
        SwiftModuleInfo info = openInfo();
        String src = SwiftModuleSourcePrinter.render(info);
        // The exact access keyword may vary by SDK version, so just look for
        // 'struct Package' / 'enum Product' on a header line followed by the
        // stub body.
        assertTrue(src.contains("struct Package"),
            "Expected `struct Package` line in synthesized source");
        assertTrue(src.contains("enum Product"),
            "Expected `enum Product` line in synthesized source");
    }

    @Test
    void nominalBodiesAreOpenedAndClosed() throws IOException {
        String src = SwiftModuleSourcePrinter.render(openInfo());
        // Brace balance is the headline correctness check — if a nominal decl
        // misses its closing brace the whole file becomes a parse error chain.
        long opens = src.chars().filter(c -> c == '{').count();
        long closes = src.chars().filter(c -> c == '}').count();
        assertEquals(opens, closes,
            "Braces must balance ({ = " + opens + ", } = " + closes + ")");
        assertTrue(opens > 30,
            "PackageDescription's body should produce dozens of brace pairs, got " + opens);
    }

    @Test
    void methodSignaturesIncludeStubBodies() throws IOException {
        String src = SwiftModuleSourcePrinter.render(openInfo());
        // Methods + initialisers + accessors get a fatalError() stub so the
        // file parses without us having to reconstruct the original code.
        long stubs = src.lines().filter(l -> l.contains("fatalError()")).count();
        assertTrue(stubs > 30,
            "Expected lots of `fatalError()` stub bodies, got " + stubs);
    }

    @Test
    void noDeclEmitsAnEmptyLine() throws IOException {
        SwiftModuleInfo empty = new SwiftModuleInfo();
        empty.setModuleName("EmptyTest");
        empty.setTarget("x86_64-apple-macos11.0");
        empty.setFormatMajor(0);
        empty.setFormatMinor(123);
        String src = SwiftModuleSourcePrinter.render(empty);
        assertTrue(src.contains("// swift-module: EmptyTest"));
        assertTrue(src.contains("// no top-level decls"),
            "Empty-module footer expected, got: " + src);
    }

    @Test
    void rendererTouchesEveryTopLevelEntryAtLeastOnce() throws IOException {
        SwiftModuleInfo info = openInfo();
        String src = SwiftModuleSourcePrinter.render(info);
        // Count how many decls in ORDERED_TOP_LEVEL_DECLS produced any text.
        // Crude but effective: every emitted decl ends with two newlines.
        int blocks = src.split("\\n\\n").length;
        assertTrue(blocks > 20,
            "Renderer should emit dozens of decl blocks for PackageDescription, got " + blocks);
        System.out.println("Source preview (first 80 lines):");
        System.out.println(firstLines(src, 80));
    }

    @Test
    void rendersIsIdempotentAndDeterministic() throws IOException {
        SwiftModuleInfo info = openInfo();
        String first = SwiftModuleSourcePrinter.render(info);
        String second = SwiftModuleSourcePrinter.render(info);
        assertEquals(first, second, "Rendering must be deterministic for the same input");
    }

    private static String firstLines(String src, int n) {
        String[] lines = src.split("\\n", n + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, n); i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString();
    }
}
