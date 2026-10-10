/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link SwiftIdentifierTable} on hand-crafted byte
 * arrays — isolates the string-table layer from the rest of the bitcode
 * reader so a regression here is unambiguous.
 */
class SwiftIdentifierTableTest {

    @Test
    void emptyTableHasNoIdentifiers() {
        SwiftIdentifierTable t = SwiftIdentifierTable.empty();
        assertEquals(0, t.blobSize());
        assertTrue(t.allIdentifiers().isEmpty());
        assertNull(t.lookup(0));
        assertNull(t.lookup(1));
        assertNull(t.lookup(100));
    }

    @Test
    void lookupAtZeroOffsetReturnsNullSentinel() {
        // The Swift convention: identifier ID 0 means "no identifier".
        // The corresponding byte offset 0 must yield null even if data exists.
        byte[] blob = bytes("hello\0world\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        assertNull(t.lookup(0), "offset 0 is reserved sentinel");
    }

    @Test
    void lookupReadsUntilNullTerminator() {
        byte[] blob = bytes("\0Package\0Product\0Target\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);

        // 'Package' starts at byte 1.
        assertEquals("Package", t.lookup(1));
        // 'Product' starts at byte 9.
        assertEquals("Product", t.lookup(9));
        // 'Target' starts at byte 17.
        assertEquals("Target", t.lookup(17));
    }

    @Test
    void lookupOutOfRangeReturnsNull() {
        byte[] blob = bytes("\0Foo\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);

        assertNull(t.lookup(-1));
        assertNull(t.lookup(blob.length));      // past last byte
        assertNull(t.lookup(blob.length + 100));
    }

    @Test
    void allIdentifiersSplitsOnNullBytes() {
        byte[] blob = bytes("\0first\0second\0third\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        assertEquals(List.of("first", "second", "third"), t.allIdentifiers());
    }

    @Test
    void allIdentifiersHandlesNoLeadingSentinel() {
        byte[] blob = bytes("alpha\0beta\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        assertEquals(List.of("alpha", "beta"), t.allIdentifiers());
    }

    @Test
    void allIdentifiersSkipsEmptySegments() {
        // Adjacent NULs produce empty segments which we drop.
        byte[] blob = bytes("\0a\0\0b\0\0\0c\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        assertEquals(List.of("a", "b", "c"), t.allIdentifiers());
    }

    @Test
    void utf8MultiByteIdentifiersDecodeCorrectly() {
        // Swift identifiers can include arbitrary Unicode.
        String mathName = "café";        // 5 UTF-8 bytes: 0x63 0x61 0x66 0xC3 0xA9
        String emojiName = "🦄";         // 4 UTF-8 bytes
        byte[] body = ("\0" + mathName + "\0" + emojiName + "\0").getBytes(StandardCharsets.UTF_8);
        SwiftIdentifierTable t = new SwiftIdentifierTable(body);

        assertEquals(mathName, t.lookup(1));
        assertEquals(emojiName, t.lookup(1 + mathName.getBytes(StandardCharsets.UTF_8).length + 1));
        assertEquals(List.of(mathName, emojiName), t.allIdentifiers());
    }

    @Test
    void lookupByIdResolvesViaOffsetsTable() {
        byte[] blob = bytes("\0Package\0Product\0Target\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);

        // ID-to-offset table mirrors what INDEX_BLOCK's IDENTIFIER_OFFSETS gives us:
        // identifier ID 1 lives at offset 1, ID 2 at 9, ID 3 at 17.
        long[] offsets = { 1, 9, 17 };

        assertEquals("Package", t.lookupById(offsets, 1));
        assertEquals("Product", t.lookupById(offsets, 2));
        assertEquals("Target",  t.lookupById(offsets, 3));
    }

    @Test
    void lookupByIdRejectsZeroAndOutOfRangeIds() {
        byte[] blob = bytes("\0Foo\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        long[] offsets = { 1 };

        // ID 0 is the no-identifier sentinel.
        assertNull(t.lookupById(offsets, 0));
        // ID beyond the offsets array.
        assertNull(t.lookupById(offsets, 2));
        // Null offsets table is tolerated.
        assertNull(t.lookupById(null, 1));
    }

    @Test
    void lookupByIdRejectsNegativeIds() {
        byte[] blob = bytes("\0Foo\0");
        SwiftIdentifierTable t = new SwiftIdentifierTable(blob);
        long[] offsets = { 1 };
        assertNull(t.lookupById(offsets, -1));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
