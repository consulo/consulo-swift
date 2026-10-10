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

/**
 * Magic-number constants for the Swift binary serialization format.
 * <p>
 * A {@code .swiftmodule} file begins with a four-byte signature
 * &mdash; the UTF-8 encoding of U+2728 SPARKLES ({@code ✨}) followed
 * by a single byte tag. Swift's source-info and documentation companion
 * files use distinct signatures, all built on the same sparkle prefix.
 */
public final class SwiftModuleSignature {
    /** First three bytes: UTF-8 encoding of '✨' (U+2728). */
    public static final byte SPARKLE_BYTE_0 = (byte) 0xE2;
    public static final byte SPARKLE_BYTE_1 = (byte) 0x9C;
    public static final byte SPARKLE_BYTE_2 = (byte) 0xA8;

    /** Fourth byte tags the file flavor. */
    public static final byte TAG_SWIFTMODULE     = (byte) 0x0E;
    public static final byte TAG_SWIFTDOC        = (byte) 0x07;
    public static final byte TAG_SWIFTSOURCEINFO = (byte) 0x08;

    /** Length of the signature prefix in bytes. */
    public static final int LENGTH = 4;

    private SwiftModuleSignature() {
    }

    /**
     * Returns true iff {@code bytes} begins with a valid signature for the
     * given tag (e.g. {@link #TAG_SWIFTMODULE}).
     */
    public static boolean matches(byte[] bytes, byte tag) {
        return bytes != null
            && bytes.length >= LENGTH
            && bytes[0] == SPARKLE_BYTE_0
            && bytes[1] == SPARKLE_BYTE_1
            && bytes[2] == SPARKLE_BYTE_2
            && bytes[3] == tag;
    }

    /** Convenience for {@code .swiftmodule} files specifically. */
    public static boolean isSwiftModule(byte[] bytes) {
        return matches(bytes, TAG_SWIFTMODULE);
    }
}
