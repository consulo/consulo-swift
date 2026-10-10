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
 * Modifications by consulo.io (2026): replaced the LLVM-IR block enumeration
 * with the Swift serialization block IDs from
 * lib/Serialization/ModuleFormat.h in the apple/swift compiler. The structure
 * of the enum, lookup() helper, and parser-factory hook are unchanged.
 */
package uk.ac.man.cs.llvm.bc.blocks;

import uk.ac.man.cs.llvm.bc.Parser;

/**
 * Block IDs for the Swift binary serialization format.
 * <p>
 * Numeric IDs are derived from the {@code BlockID} enum in Swift's
 * {@code lib/Serialization/ModuleFormat.h}. LLVM reserves IDs 0-7 for
 * standard blocks; Swift's user-defined blocks start at
 * {@code FIRST_APPLICATION_BLOCKID = 8}.
 */
public enum Block {

    ROOT(-1, parser -> parser),

    /** LLVM standard block: top-level abbreviation definitions. */
    BLOCKINFO(0, parser -> new InformationBlockParser(parser)),

    /** Outer envelope. Every other Swift block is nested inside it. */
    MODULE(8, parser -> parser),

    /** Module name, target triple, format version, SDK info. */
    CONTROL(9, parser -> parser),

    /** Imported modules and other inputs. */
    INPUT(10, parser -> parser),

    /** All declarations and types serialized from this module. */
    DECLS_AND_TYPES(11, parser -> parser),

    /** String table backing all identifiers in the module. */
    IDENTIFIER_DATA(12, parser -> parser),

    /** Cross-reference index. */
    INDEX(13, parser -> parser),

    /** Serialized SIL function bodies — only for inlinable / cross-mod-opt code. */
    SIL(14, parser -> parser),

    /** Index into the SIL block. */
    SIL_INDEX(15, parser -> parser),

    /** Compiler-options sub-block of the control block. */
    OPTIONS(16, parser -> parser),

    /** Per-decl member tables, sub-block of INDEX. */
    DECL_MEMBER_TABLES(17, parser -> parser),

    /** Hidden type-layout info used by reflection. */
    HIDDEN_TYPE_LAYOUTS(18, parser -> parser),

    /** Doc-comment block. */
    COMMENT(19, parser -> parser),

    /** Per-decl source-location info. */
    DECL_LOCS(20, parser -> parser),

    /** fine-grained incremental dependency-info block. */
    INCREMENTAL_INFORMATION(21, parser -> parser);

    public static Block lookup(long id) {
        if (id == 0) {
            return BLOCKINFO;
        }
        for (Block b : values()) {
            // ROOT is a sentinel — it doesn't appear as a block in the
            // bitstream and must not be resolvable by numeric id.
            if (b == ROOT) {
                continue;
            }
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    @FunctionalInterface
    private static interface ParserCreator {

        public Parser parser(Parser parser);
    }

    private final int id;

    private final ParserCreator creator;

    private Block(int id, ParserCreator creator) {
        this.id = id;
        this.creator = creator;
    }

    public int id() {
        return id;
    }

    public Parser parser(Parser parser) {
        return creator.parser(parser);
    }

    @Override
    public String toString() {
        return String.format("%s - #%d", name(), id());
    }
}
