/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The opaque contents of the Swift {@code IDENTIFIER_DATA_BLOCK} — every
 * identifier string referenced anywhere in the module is concatenated into
 * one blob of NUL-separated UTF-8 byte sequences.
 * <p>
 * Other blocks reference identifiers by their <em>byte offset</em> into
 * this blob (1-based; offset 0 is a reserved "no identifier" sentinel).
 * {@link #lookup(int)} resolves an offset to the string starting there.
 */
public final class SwiftIdentifierTable {
    private static final SwiftIdentifierTable EMPTY =
        new SwiftIdentifierTable(new byte[0]);

    private final byte[] blob;

    public SwiftIdentifierTable(byte[] blob) {
        this.blob = blob;
    }

    public static SwiftIdentifierTable empty() {
        return EMPTY;
    }

    /** Raw blob length in bytes. */
    public int blobSize() {
        return blob.length;
    }

    /**
     * Returns the UTF-8 string whose byte offset into the blob is
     * {@code offset}, reading until the next NUL byte or end of blob.
     * Returns {@code null} if the offset is out of range or zero
     * (the reserved sentinel).
     */
    public String lookup(int offset) {
        if (offset <= 0 || offset >= blob.length) {
            return null;
        }
        int end = offset;
        while (end < blob.length && blob[end] != 0) {
            end++;
        }
        return new String(blob, offset, end - offset, StandardCharsets.UTF_8);
    }

    /**
     * Resolve a Swift identifier ID to its string. {@code identifierOffsets[i]}
     * is the byte offset into this blob for identifier ID {@code i+1}
     * ({@code 0} is the reserved "no identifier" sentinel).
     */
    public String lookupById(long[] identifierOffsets, int id) {
        if (id <= 0 || identifierOffsets == null || id - 1 >= identifierOffsets.length) {
            return null;
        }
        long off = identifierOffsets[id - 1];
        return lookup((int) off);
    }

    /**
     * Returns every identifier in the table, in storage order. Useful for
     * tests and the "list public symbols" view; production code that maps
     * specific offsets to strings should use {@link #lookup(int)} directly.
     */
    public List<String> allIdentifiers() {
        if (blob.length == 0) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        int i = 0;
        // Some Swift toolchains begin the table with a sentinel NUL — skip it.
        if (blob[0] == 0) {
            i = 1;
        }
        while (i < blob.length) {
            int end = i;
            while (end < blob.length && blob[end] != 0) {
                end++;
            }
            if (end > i) {
                out.add(new String(blob, i, end - i, StandardCharsets.UTF_8));
            }
            i = end + 1;
        }
        return out;
    }
}
