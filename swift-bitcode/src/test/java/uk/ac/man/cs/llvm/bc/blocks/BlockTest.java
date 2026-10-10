/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package uk.ac.man.cs.llvm.bc.blocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit coverage for the Swift-flavored {@link Block} enum. This file's
 * vendored ancestor shipped LLVM-IR block IDs; the consulo-swift fork
 * substitutes Swift's serialization block IDs from
 * {@code lib/Serialization/ModuleFormat.h}, so these tests pin the
 * numeric mapping in place so an accidental renumbering doesn't break
 * the real-binary integration test silently.
 */
class BlockTest {

    @Test
    void blockInfoIsZero() {
        assertEquals(0, Block.BLOCKINFO.id());
        assertSame(Block.BLOCKINFO, Block.lookup(0));
    }

    @Test
    void swiftModuleBlockIdsMatchModuleFormatH() {
        // Swift block IDs start at FIRST_APPLICATION_BLOCKID = 8 and follow
        // the BlockID enum's declaration order in ModuleFormat.h.
        assertEquals(8,  Block.MODULE.id());
        assertEquals(9,  Block.CONTROL.id());
        assertEquals(10, Block.INPUT.id());
        assertEquals(11, Block.DECLS_AND_TYPES.id());
        assertEquals(12, Block.IDENTIFIER_DATA.id());
        assertEquals(13, Block.INDEX.id());
        assertEquals(14, Block.SIL.id());
        assertEquals(15, Block.SIL_INDEX.id());
        assertEquals(16, Block.OPTIONS.id());
        assertEquals(17, Block.DECL_MEMBER_TABLES.id());
        assertEquals(18, Block.HIDDEN_TYPE_LAYOUTS.id());
        assertEquals(19, Block.COMMENT.id());
        assertEquals(20, Block.DECL_LOCS.id());
        assertEquals(21, Block.INCREMENTAL_INFORMATION.id());
    }

    @Test
    void lookupRoundtripsForEveryKnownBlock() {
        for (Block b : Block.values()) {
            if (b == Block.ROOT) continue;        // sentinel, never appears in bitstream
            assertSame(b, Block.lookup(b.id()),
                "lookup(" + b.id() + ") should round-trip to " + b);
        }
    }

    @Test
    void unknownBlockIdsReturnNull() {
        // The Swift schema does not reserve these slots — Parser.enter() treats
        // null as "skip the block", which is exactly the desired behavior on
        // an unrecognized future block.
        assertNull(Block.lookup(7));    // gap below FIRST_APPLICATION_BLOCKID
        assertNull(Block.lookup(99));   // far past last known Swift block
        assertNull(Block.lookup(-5));
    }

    @Test
    void rootBlockSentinelIsExcludedFromLookup() {
        // ROOT is a marker for "we're at the top, no parent" and should not
        // be reachable via numeric ID.
        assertNull(Block.lookup(-1));
        assertNotEquals(Block.ROOT, Block.lookup(0));
    }

    @Test
    void toStringFormatIsStable() {
        // The format "NAME - #ID" is used in debug logs; pinning it here
        // documents the contract.
        assertEquals("MODULE - #8",  Block.MODULE.toString());
        assertEquals("CONTROL - #9", Block.CONTROL.toString());
    }
}
