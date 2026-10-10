/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

module consulo.swift.bitcode {
    exports consulo.swift.bitcode.swiftmodule;

    // Vendored MIT-licensed llvm-bcj `bc/` package. Origin and license are
    // documented in the NOTICE file at the repository root.
    exports uk.ac.man.cs.llvm.bc;
    exports uk.ac.man.cs.llvm.bc.blocks;
    exports uk.ac.man.cs.llvm.bc.records;
    exports uk.ac.man.cs.llvm.util;
}
