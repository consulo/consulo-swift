/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.Parser;
import uk.ac.man.cs.llvm.bc.ParserListener;
import uk.ac.man.cs.llvm.bc.ParserResult;
import uk.ac.man.cs.llvm.bc.blocks.Block;

/**
 * Drives the {@link Parser} from {@code uk.ac.man.cs.llvm.bc} over a
 * {@code .swiftmodule} byte array — starting past the 4-byte sparkle
 * signature, reading opcodes at the current abbreviation width, and
 * dispatching to the registered {@link uk.ac.man.cs.llvm.bc.Operation}
 * for each. Listener callbacks fire on every block enter/exit and on
 * every record encountered.
 */
public final class SwiftBitstreamWalker {

    /** Drive the walker until end-of-stream or the top-level parser unwinds. */
    public static void walk(byte[] bytes, ParserListener listener) {
        if (!SwiftModuleSignature.isSwiftModule(bytes)) {
            throw new IllegalArgumentException(
                "Byte array does not begin with a .swiftmodule signature");
        }

        SwiftBitstream stream = new SwiftBitstream(bytes);
        long endOfStream = (long) bytes.length << 3;   // total bits
        long startOffset = (long) SwiftModuleSignature.LENGTH << 3;

        // Construct the initial parser positioned just past the signature.
        Parser parser = new Parser(stream, Block.ROOT, listener);
        parser = parser.offset(startOffset);

        while (parser != null && parser.offset() < endOfStream) {
            ParserResult result = parser.readId();
            long opcode = result.value();
            parser = result.parser();
            parser = parser.operation(opcode).apply(parser);
        }
    }
}
