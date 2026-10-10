/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 University of Manchester
 * Copyright (c) 2026 consulo.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Modifications by consulo.io (2026): implemented get() for the BCBlob
 * encoding (originally a stub throwing UnsupportedOperationException).
 * Swift .swiftmodule files use blobs extensively (module name, target
 * triple, identifier table, etc.), so blob support is required.
 */
package uk.ac.man.cs.llvm.bc.records;

import uk.ac.man.cs.llvm.bc.Parser;
import uk.ac.man.cs.llvm.bc.ParserResult;

public final class UserRecordBinaryOperand extends UserRecordOperand {

    public UserRecordBinaryOperand() {
        super();
    }

    /**
     * Reads an LLVM bitcode {@code Blob} operand:
     * <pre>
     *   numbytes : VBR(6)
     *   pad      : align 32
     *   bytes    : numbytes * 8 bits
     *   pad      : align 32
     * </pre>
     * Returns the blob content as a {@code long[]} of byte values (each
     * entry in the range 0..255).
     */
    @Override
    protected ParserResult get(Parser parser) {
        ParserResult result = parser.readVBR(6);
        int count = (int) result.value();
        parser = result.parser().align(32);

        long[] bytes = new long[count];
        for (int i = 0; i < count; i++) {
            result = parser.read(8);
            bytes[i] = result.value() & 0xFFL;
            parser = result.parser();
        }
        parser = parser.align(32);
        return new ParserResult(parser, bytes);
    }

    @Override
    public String toString() {
        return "blob";
    }
}
