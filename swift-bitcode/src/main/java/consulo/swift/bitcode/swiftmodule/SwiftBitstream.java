/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.Bitstream;

/**
 * Trivial subclass that exposes the protected {@link Bitstream#Bitstream(byte[])}
 * constructor so we can wrap an already-loaded {@code .swiftmodule} byte array
 * without writing it to disk first.
 */
final class SwiftBitstream extends Bitstream {
    SwiftBitstream(byte[] bytes) {
        super(bytes);
    }
}
