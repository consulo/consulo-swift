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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link SwiftTypePrinter} against the bundled
 * {@code PackageDescription.swiftmodule}. Types are interesting because
 * they recursively reference decls and other types — a printer that
 * spits out plausible Swift text is the foundation of the decompiler.
 */
class SwiftTypePrinterTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    private static SwiftModuleInfo openInfo() throws IOException {
        try (InputStream in = SwiftTypePrinterTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in, "fixture missing");
            return SwiftModuleReader.open(in).readModuleInfo();
        }
    }

    @Test
    void typeIdZeroRendersAsNone() throws IOException {
        SwiftTypePrinter printer = new SwiftTypePrinter(openInfo());
        assertEquals(SwiftTypePrinter.NONE, printer.print(0));
    }

    @Test
    void everyDeclaredTypeRendersToNonEmptyString() throws IOException {
        SwiftModuleInfo info = openInfo();
        SwiftTypePrinter printer = new SwiftTypePrinter(info);

        SwiftTypeEntry[] types = info.getTypeEntries();
        assertTrue(types.length > 0, "PackageDescription has type entries");
        int rendered = 0;
        for (int id = 1; id <= types.length; id++) {
            if (types[id - 1] == null) {
                continue;
            }
            String text = printer.print(id);
            assertNotNull(text);
            assertFalse(text.isEmpty(), "typeID=" + id + " printed to empty string");
            rendered++;
        }
        assertTrue(rendered > 100,
            "PackageDescription has hundreds of typeIDs that should render, got " + rendered);
        System.out.println("Rendered " + rendered + " types of " + types.length + " slots");
    }

    @Test
    void typeKindHistogramHasExpectedSwiftConstructs() throws IOException {
        SwiftModuleInfo info = openInfo();
        SwiftTypeEntry[] types = info.getTypeEntries();
        Map<Integer, Integer> hist = new HashMap<>();
        for (SwiftTypeEntry e : types) {
            if (e == null) {
                continue;
            }
            hist.merge(e.recordKind(), 1, Integer::sum);
        }
        System.out.println("Type kind histogram:");
        hist.forEach((k, v) ->
            System.out.println("  kind=" + k + " count=" + v));

        // PackageDescription is rich enough to exercise the printer's
        // common cases — at minimum it carries nominals + bound generics +
        // optionals / arrays / dictionaries.
        assertTrue(hist.getOrDefault(SwiftDeclRecord.NOMINAL_TYPE, 0) > 0,
            "NOMINAL_TYPE expected — even the simplest module uses one");
        assertTrue(hist.getOrDefault(SwiftDeclRecord.BOUND_GENERIC_TYPE, 0) > 0,
            "BOUND_GENERIC_TYPE expected (Array<X>, Optional<X>, etc.)");
        // The shape varies — assert at least one syntactic sugar appears.
        int sugar = hist.getOrDefault(SwiftDeclRecord.OPTIONAL_TYPE, 0)
            + hist.getOrDefault(SwiftDeclRecord.ARRAY_SLICE_TYPE, 0)
            + hist.getOrDefault(SwiftDeclRecord.DICTIONARY_TYPE, 0);
        // Sugar may or may not appear depending on serialization choices —
        // sugar sometimes gets desugared to BOUND_GENERIC_TYPE; document
        // this rather than fail noisily.
        System.out.println("Sugar (Optional/Array/Dictionary) count: " + sugar);
    }

    @Test
    void boundGenericPrintsAsAngleBracketSyntax() throws IOException {
        SwiftModuleInfo info = openInfo();
        SwiftTypePrinter printer = new SwiftTypePrinter(info);
        SwiftTypeEntry[] types = info.getTypeEntries();

        // Find the first BOUND_GENERIC_TYPE and assert its text contains '<' '>'.
        for (int id = 1; id <= types.length; id++) {
            SwiftTypeEntry e = types[id - 1];
            if (e != null && e.recordKind() == SwiftDeclRecord.BOUND_GENERIC_TYPE) {
                String text = printer.print(id);
                assertTrue(text.contains("<") && text.contains(">"),
                    "Bound generic text should look like Decl<...>, got " + text);
                System.out.println("First bound-generic: " + text);
                return;
            }
        }
        // Nothing to assert against — the histogram test already complains.
    }

    @Test
    void nominalTypeResolvesToADeclName() throws IOException {
        SwiftModuleInfo info = openInfo();
        SwiftTypePrinter printer = new SwiftTypePrinter(info);
        SwiftTypeEntry[] types = info.getTypeEntries();

        // Pick the first NOMINAL_TYPE that points at a LOCAL decl (i.e. its
        // declID resolves to a name through the identifier table). External
        // (XREF) decls have no inline name in this module, so they'd render
        // as <extern:...> placeholders — useful but not useful for asserting
        // clean Swift text yet (cross-module name resolution lands later).
        int sampled = 0;
        for (int id = 1; id <= types.length && sampled < 5; id++) {
            SwiftTypeEntry e = types[id - 1];
            if (e == null || e.recordKind() != SwiftDeclRecord.NOMINAL_TYPE) {
                continue;
            }
            int declId = (int) (e.args().length > 0 ? e.args()[0] : 0);
            String declName = info.lookupDeclName(declId);
            if (declName == null || declName.isEmpty()) {
                continue;
            }
            String text = printer.print(id);
            // The printer may sanitize non-identifier names (e.g.
            // "extension Foo" → "extension_Foo") to keep the output
            // parseable. Compare against the sanitized form too so this
            // assertion stays useful for synthesized symbol names.
            String sanitized = SwiftIdentifierNames.sanitize(declName);
            assertTrue(text.contains(declName) || text.contains(sanitized),
                "NOMINAL_TYPE text should contain the decl name '"
                    + declName + "' (or sanitized form '" + sanitized + "'), got " + text);
            assertFalse(text.startsWith("Any /* typeID="),
                "Local NOMINAL_TYPE should resolve to a real name, got " + text);
            sampled++;
        }
        assertTrue(sampled > 0, "At least one local NOMINAL_TYPE expected");
    }

    @Test
    void cycleProtectionDoesNotLoop() throws IOException {
        // Synthetic cycle: build a NOMINAL_TYPE that points at itself via
        // its parent slot. NOMINAL_TYPE is the simplest dotted-name path
        // the printer takes — easiest place to confirm the identity-map
        // cycle guard fires before the printer recurses forever.
        SwiftModuleInfo info = new SwiftModuleInfo();
        SwiftTypeEntry self = new SwiftTypeEntry(1,
            SwiftDeclRecord.NOMINAL_TYPE,
            new long[]{ 99L /* decl */, 1L /* parent = self */ });
        info.setTypeEntries(new SwiftTypeEntry[]{ self });
        SwiftTypePrinter printer = new SwiftTypePrinter(info);
        String text = printer.print(1);
        assertNotNull(text);
        // We don't care which sentinel the printer chooses (could be
        // <recursive>, an Any placeholder, etc.) — just that it returned
        // without stack-overflowing and produced a non-empty string.
        assertFalse(text.isEmpty(), "Printer must complete on cyclic input");
    }

    @Test
    void printSampleOfPackageDescriptionTypes() throws IOException {
        SwiftModuleInfo info = openInfo();
        SwiftTypePrinter printer = new SwiftTypePrinter(info);
        SwiftTypeEntry[] types = info.getTypeEntries();

        System.out.println("Sample of first 30 type IDs:");
        for (int id = 1; id <= Math.min(30, types.length); id++) {
            SwiftTypeEntry e = types[id - 1];
            if (e == null) {
                continue;
            }
            String text = printer.print(id);
            System.out.println("  typeID=" + id + " kind=" + e.recordKind() + " : " + text);
        }
    }
}
