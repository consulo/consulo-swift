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
 * Modifications by consulo.io (2026): trim the assembled operand array to the
 * number of values actually produced. Upstream sized {@code ops} to
 * {@code operands.size() - 1} up front, which leaves zero-filled tail slots
 * for variable-length operands (BCArray, BCBlob) that produce fewer values
 * than the operand count — e.g. an empty MODULE_NAME blob would emit
 * {@code [0]} instead of {@code []}.
 */
package uk.ac.man.cs.llvm.bc.records;

import java.util.Arrays;
import java.util.List;
import uk.ac.man.cs.llvm.bc.Operation;
import uk.ac.man.cs.llvm.bc.Parser;
import uk.ac.man.cs.llvm.bc.ParserResult;

public final class UserRecordBuilder implements Operation {

    private final List<UserRecordOperand> operands;

    public UserRecordBuilder(List<UserRecordOperand> operands) {
        this.operands = operands;
    }

    @Override
    public Parser apply(Parser parser) {
        ParserResult result = operands.get(0).get(parser);
        long id = result.value();

        long[] ops = new long[operands.size() - 1];

        int idx = 0;
        for (int i = 0; i < operands.size() - 1; i++) {
            result = operands.get(i + 1).get(result.parser());
            long[] values = result.values();
            if (idx + values.length > ops.length) {
                ops = Arrays.copyOf(ops, idx + values.length);
            }
            System.arraycopy(values, 0, ops, idx, values.length);
            idx += values.length;
        }

        // Trim trailing slots that no operand actually filled. This matters
        // for variable-length operands (BCArray, BCBlob) which may produce
        // zero values for an empty payload.
        if (idx != ops.length) {
            ops = Arrays.copyOf(ops, idx);
        }

        result.parser().record(id, ops);

        return result.parser();
    }
}
