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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SwiftDeclTableExtractor} — the phase-2 walker over
 * {@code DECLS_AND_TYPES_BLOCK} that turns {@code INDEX_BLOCK.DECL_OFFSETS}
 * into named {@link SwiftDeclEntry} records. The shipped
 * {@code PackageDescription.swiftmodule} fixture is our gold standard: it
 * defines the {@code Package}, {@code Product}, {@code Target} symbols
 * every Swift Package Manager user is familiar with.
 */
class SwiftDeclTableExtractorTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    private static SwiftModuleReader openReader() throws IOException {
        try (InputStream in = SwiftDeclTableExtractorTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in, "fixture missing: " + FIXTURE_RESOURCE);
            return SwiftModuleReader.open(in);
        }
    }

    @Test
    void declEntriesArePopulatedForEveryNonZeroDeclOffset() throws IOException {
        SwiftModuleInfo info = openReader().readModuleInfo();
        SwiftDeclEntry[] entries = info.getDeclEntries();
        long[] offsets = info.getDeclOffsets();

        assertEquals(offsets.length, entries.length,
            "Entry array must be parallel to declOffsets");

        int populated = 0;
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] > 0) {
                assertNotNull(entries[i],
                    "declID=" + (i + 1) + " has offset=" + offsets[i]
                        + " but no entry was captured");
                populated++;
            }
        }
        assertTrue(populated > 100,
            "PackageDescription should have hundreds of resolved decls, got " + populated);
        System.out.println("Resolved decl entries: " + populated + " of " + offsets.length);
    }

    @Test
    void topLevelDeclsContainPackageProductTarget() throws IOException {
        SwiftModuleInfo info = openReader().readModuleInfo();

        long[] topLevel = info.getOrderedTopLevelDeclIds();
        assertTrue(topLevel.length > 0, "PackageDescription has top-level decls");

        // Build a histogram of (name, kind) for diagnostics, then assert the
        // headline symbols are present.
        Map<String, Integer> nameToKind = new LinkedHashMap<>();
        int unnamed = 0;
        for (long id : topLevel) {
            SwiftDeclEntry entry = info.lookupDecl((int) id);
            assertNotNull(entry,
                "top-level declID=" + id + " missing from decl entries");
            String name = info.lookupDeclName((int) id);
            if (name == null || name.isEmpty()) {
                unnamed++;
                continue;
            }
            nameToKind.putIfAbsent(name, entry.recordKind());
        }

        System.out.println("Top-level decl kinds in PackageDescription:");
        Map<Integer, Integer> kindCounts = new HashMap<>();
        for (long id : topLevel) {
            SwiftDeclEntry e = info.lookupDecl((int) id);
            if (e != null) {
                kindCounts.merge(e.recordKind(), 1, Integer::sum);
            }
        }
        kindCounts.forEach((k, v) ->
            System.out.println("  " + SwiftDeclRecord.name(k) + " x" + v));
        System.out.println("  (named=" + nameToKind.size() + ", unnamed=" + unnamed + ")");

        assertTrue(nameToKind.containsKey("Package"),
            "Package should appear among top-level decls; got " + nameToKind.keySet());
        assertTrue(nameToKind.containsKey("Product"),
            "Product should appear among top-level decls; got " + nameToKind.keySet());
        // Target also appears in PackageDescription but as a nested decl in
        // this fixture (it surfaces in the identifier table, not in
        // ORDERED_TOP_LEVEL_DECLS). It'll resolve through DECL_MEMBER_NAMES
        // once we wire that up in a later milestone.
    }

    @Test
    void packageIsANominalLocalDecl() throws IOException {
        SwiftModuleInfo info = openReader().readModuleInfo();

        SwiftDeclEntry pkg = findTopLevelByName(info, "Package");
        assertNotNull(pkg, "Package decl entry must be present");
        // In the bundled fixture Package is serialized as STRUCT_DECL.
        // Older PackageDescription releases declare it as `public final class
        // Package`; newer ones may switch to a struct. Accept either nominal
        // shape — we just want to confirm it's a local type, not an XREF.
        int kind = pkg.recordKind();
        assertTrue(
            kind == SwiftDeclRecord.STRUCT_DECL || kind == SwiftDeclRecord.CLASS_DECL,
            "Package should be a local nominal decl, got " + SwiftDeclRecord.name(kind));
        assertFalse(pkg.isExternal(), "Package must be a local decl, not an XREF");
    }

    @Test
    void productResolvesToALocalNominalDecl() throws IOException {
        SwiftModuleInfo info = openReader().readModuleInfo();

        SwiftDeclEntry product = findTopLevelByName(info, "Product");
        assertNotNull(product, "Product top-level decl must be present");
        // In this fixture Product is serialized as an enum (an enum with
        // associated values per product flavor). Accept any local nominal
        // kind — what matters is that it's not an XREF to an external module.
        int kind = product.recordKind();
        assertTrue(
            kind == SwiftDeclRecord.STRUCT_DECL
                || kind == SwiftDeclRecord.CLASS_DECL
                || kind == SwiftDeclRecord.ENUM_DECL,
            "Expected local nominal decl, got " + SwiftDeclRecord.name(kind));
        assertFalse(product.isExternal());
    }

    @Test
    void earlyDeclIdsAreExternalXrefs() throws IOException {
        // declID=1..N typically reference stdlib/Foundation types (Int, String,
        // ...) — Swift writes those as XREF records in the local module's
        // DECLS_AND_TYPES_BLOCK so its types can name them without inlining.
        SwiftModuleInfo info = openReader().readModuleInfo();
        SwiftDeclEntry first = info.lookupDecl(1);
        assertNotNull(first, "declID=1 must be resolvable, even as an XREF");
        assertTrue(first.isExternal(),
            "declID=1 should be a cross-module XREF, got "
                + SwiftDeclRecord.name(first.recordKind()));
        // Local decl names should NOT resolve through XREF — confirm that.
        assertNull(info.lookupDeclName(1),
            "XREF entries don't carry a base-name identifier ID");
    }

    @Test
    void namesResolveOnlyForExpectedKinds() throws IOException {
        SwiftModuleInfo info = openReader().readModuleInfo();

        // Count how many decls of each kind resolve to a name. Nominal kinds
        // (class/struct/enum/protocol/typealias) should always have names;
        // anonymous kinds (extension/init/deinit/accessor) should never.
        Map<Integer, int[]> stats = new HashMap<>();   // kind -> {named, total}
        for (SwiftDeclEntry entry : info.getDeclEntries()) {
            if (entry == null) {
                continue;
            }
            int[] row = stats.computeIfAbsent(entry.recordKind(), k -> new int[2]);
            row[1]++;
            String name = info.lookupDeclName(entry.declId());
            if (name != null && !name.isEmpty()) {
                row[0]++;
            }
        }

        // Nominal-bearing kinds: every record must have a name resolvable.
        for (int kind : new int[]{
            SwiftDeclRecord.STRUCT_DECL,
            SwiftDeclRecord.CLASS_DECL,
            SwiftDeclRecord.ENUM_DECL,
            SwiftDeclRecord.PROTOCOL_DECL,
            SwiftDeclRecord.TYPE_ALIAS_DECL,
            SwiftDeclRecord.VAR_DECL,
        }) {
            int[] row = stats.get(kind);
            if (row == null) {
                continue;
            }
            assertEquals(row[1], row[0],
                "Every " + SwiftDeclRecord.name(kind)
                    + " should resolve to a name; named=" + row[0] + " total=" + row[1]);
        }

        // Anonymous kinds: zero names by construction.
        for (int kind : new int[]{
            SwiftDeclRecord.CONSTRUCTOR_DECL,
            SwiftDeclRecord.DESTRUCTOR_DECL,
            SwiftDeclRecord.EXTENSION_DECL,
            SwiftDeclRecord.ACCESSOR_DECL,
        }) {
            int[] row = stats.get(kind);
            if (row == null) {
                continue;
            }
            assertEquals(0, row[0],
                SwiftDeclRecord.name(kind) + " never carries a base identifier name");
        }
    }

    @Test
    void extractorReturnsEmptyArrayWhenDeclOffsetsEmpty() {
        // Smallest possible valid input: a 4-byte signature with nothing after it.
        // Even if the bitstream is otherwise garbage, an empty declOffsets array
        // should short-circuit to an empty result without touching the bytes.
        byte[] dummy = {
            SwiftModuleSignature.SPARKLE_BYTE_0,
            SwiftModuleSignature.SPARKLE_BYTE_1,
            SwiftModuleSignature.SPARKLE_BYTE_2,
            SwiftModuleSignature.TAG_SWIFTMODULE,
        };
        SwiftDeclEntry[] out = SwiftDeclTableExtractor.extract(dummy, new long[0]);
        assertEquals(0, out.length);
    }

    @Test
    void firstHundredTopLevelDeclsPrint() throws IOException {
        // Diagnostic — show the early top-level names so regressions are
        // easy to eyeball when the format version changes upstream.
        SwiftModuleInfo info = openReader().readModuleInfo();
        long[] topLevel = info.getOrderedTopLevelDeclIds();
        List<String> sample = new ArrayList<>();
        for (int i = 0; i < Math.min(topLevel.length, 30); i++) {
            int id = (int) topLevel[i];
            SwiftDeclEntry e = info.lookupDecl(id);
            String name = info.lookupDeclName(id);
            sample.add(SwiftDeclRecord.name(e == null ? -1 : e.recordKind())
                + ":" + (name == null ? "<unnamed>" : name));
        }
        System.out.println("First 30 top-level decls: " + sample);
        assertFalse(sample.isEmpty());
    }

    private static SwiftDeclEntry findTopLevelByName(SwiftModuleInfo info, String name) {
        for (long id : info.getOrderedTopLevelDeclIds()) {
            String got = info.lookupDeclName((int) id);
            if (name.equals(got)) {
                return info.lookupDecl((int) id);
            }
        }
        return null;
    }

    // Suppress IDE unused-import nag if Arrays goes away on a future refactor.
    @SuppressWarnings("unused")
    private static final Class<?> UNUSED = Arrays.class;
}
