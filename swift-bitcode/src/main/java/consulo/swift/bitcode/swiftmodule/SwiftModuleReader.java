/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */

package consulo.swift.bitcode.swiftmodule;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for reading a {@code .swiftmodule} file.
 * <p>
 * The bitcode format is offset-based — records reference each other by bit
 * offset within the same stream — so the parser needs all bytes resident
 * to resolve cross-references. We minimise the damage from a hostile or
 * accidentally-huge input by:
 *
 * <ol>
 *   <li><b>Sniffing the 4-byte signature first</b> — a non-Swift file is
 *       rejected after exactly 4 bytes have been read, no matter how large
 *       the stream is.</li>
 *   <li><b>Capping the loaded byte count</b> — if the file exceeds the
 *       supplied limit, an {@link IOException} is thrown without ever
 *       allocating a buffer big enough to hold it. The default limit
 *       ({@link #DEFAULT_MAX_BYTES}) is 64 MiB, which fits every Apple
 *       framework {@code .swiftmodule} shipped to date with headroom.</li>
 * </ol>
 *
 * Callers that legitimately need to read a larger module can supply their
 * own cap through {@link #open(InputStream, int)}.
 */
public final class SwiftModuleReader {

    /**
     * Default cap on the size of a {@code .swiftmodule} loaded into memory.
     * 256 MiB — the linux Swift.swiftmodule alone is ~16 MiB and the Apple
     * macOS variant pushes ~30 MiB; SwiftUI/CoreData/Foundation .swiftmodule
     * bundles run up to ~60-80 MiB combined with their debug info. 256 MiB
     * leaves headroom for the biggest first-party modules without making
     * runaway-allocation attacks easy.
     *
     * <p>This is a HEAP allocation — the full file body is read into a
     * single {@code byte[]} because the parser does random-access JumpToBit
     * reads against {@code DECL_OFFSETS}. A streaming/memory-mapped
     * variant would let us drop this cap entirely; see TODO in
     * {@link SwiftBitstream} for the refactor sketch.
     */
    public static final int DEFAULT_MAX_BYTES = 256 * 1024 * 1024;

    private final byte[] bytes;

    private SwiftModuleReader(byte[] bytes) {
        this.bytes = bytes;
    }

    /** Validates and loads a {@code .swiftmodule} stream up to {@link #DEFAULT_MAX_BYTES}. */
    public static SwiftModuleReader open(InputStream in) throws IOException {
        return open(in, DEFAULT_MAX_BYTES);
    }

    /**
     * Validates and loads a {@code .swiftmodule} stream up to {@code maxBytes}.
     * <p>
     * Reads the 4-byte signature first; on mismatch throws an {@link IOException}
     * before reading any further bytes from the stream. If the stream is
     * non-empty after exactly {@code maxBytes} bytes have been consumed, the
     * read aborts with an {@link IOException} (does not allocate the bytes
     * beyond the cap).
     *
     * @param in       source stream (caller owns its lifecycle — not closed here)
     * @param maxBytes upper bound on bytes accepted; must be at least
     *                 {@link SwiftModuleSignature#LENGTH 4}
     */
    public static SwiftModuleReader open(InputStream in, int maxBytes) throws IOException {
        Objects.requireNonNull(in, "in");
        if (maxBytes < SwiftModuleSignature.LENGTH) {
            throw new IllegalArgumentException(
                "maxBytes must be at least " + SwiftModuleSignature.LENGTH + ", got " + maxBytes);
        }

        // Stage 1: 4-byte signature. Reject non-Swift content before allocating
        // a body buffer.
        byte[] header = readExactly(in, SwiftModuleSignature.LENGTH);
        if (!SwiftModuleSignature.isSwiftModule(header)) {
            throw new IOException("Not a Swift module file (bad signature)");
        }

        // Stage 2: read body bounded by maxBytes. We size-cap the buffer
        // before reading, so an attacker streaming TB of garbage can't make
        // us allocate proportionally — we just stop at the cap and raise.
        int bodyCap = maxBytes - SwiftModuleSignature.LENGTH;
        byte[] body = in.readNBytes(bodyCap);
        // If a 0-length read happens at the cap, the stream might have more —
        // a single follow-up read tells us whether to error.
        if (body.length == bodyCap) {
            int probe = in.read();
            if (probe != -1) {
                throw new IOException(
                    "Swift module exceeds maxBytes=" + maxBytes
                        + " (more bytes available beyond the cap)");
            }
        }

        byte[] all = new byte[SwiftModuleSignature.LENGTH + body.length];
        System.arraycopy(header, 0, all, 0, SwiftModuleSignature.LENGTH);
        System.arraycopy(body, 0, all, SwiftModuleSignature.LENGTH, body.length);
        return new SwiftModuleReader(all);
    }

    /** Convenience wrapper around {@link #open(InputStream)} for {@link Path}. */
    public static SwiftModuleReader open(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return open(in);
        }
    }

    /**
     * Stream-scan the CONTROL block of a {@code .swiftmodule} without
     * materializing the whole file. Returns a {@link SwiftModuleInfo} with
     * only the CONTROL-block fields populated (name, target, format
     * version, SDK info). The identifier table, decl/type/identifier
     * offsets, and ORDERED_TOP_LEVEL_DECLS are NOT extracted by this call —
     * for those use {@link #open(InputStream)}+{@link #readModuleInfo()}.
     * <p>
     * Use this in startup paths that need to enumerate every
     * {@code .swiftmodule} on an SDK search path: typical cost is a few
     * hundred bytes of I/O regardless of the module's actual size.
     */
    public static SwiftModuleInfo quickInfo(InputStream in) throws IOException {
        return StreamingSwiftModuleScanner.quickInfo(in);
    }

    /** Total length of the module's bytes. */
    public int getSize() {
        return bytes.length;
    }

    /**
     * Walks the bitstream and returns the control-block metadata
     * (module name, target triple, format version, SDK info, etc.).
     */
    public SwiftModuleInfo readModuleInfo() {
        return SwiftModuleInfoExtractor.extract(bytes);
    }

    /**
     * Returns the raw bytes. Callers must treat the array as read-only;
     * the block readers operate on it directly to avoid copying on every
     * parse.
     */
    public byte[] getBytes() {
        return bytes;
    }

    /** Read exactly {@code n} bytes or throw {@link EOFException}. */
    private static byte[] readExactly(InputStream in, int n) throws IOException {
        byte[] out = new byte[n];
        int total = 0;
        while (total < n) {
            int read = in.read(out, total, n - total);
            if (read < 0) {
                throw new EOFException(
                    "Unexpected end of stream after " + total + " of " + n + " bytes");
            }
            total += read;
        }
        return out;
    }
}
