/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;
import uk.ac.man.cs.llvm.bc.ParserListener;
import uk.ac.man.cs.llvm.bc.blocks.Block;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives {@link SwiftBitstreamWalker} over synthetic bitstreams produced
 * by {@link BitstreamWriter}. Isolates the walker (block entry / exit
 * dispatch, opcode dispatch, abbreviation width tracking) from the
 * Swift-specific listeners and the PackageDescription fixture.
 */
class SwiftBitstreamWalkerTest {

    /**
     * A trace listener that records every {@code enter}/{@code exit}/
     * {@code record} call so tests can assert the exact event sequence.
     */
    /**
     * Records every enter/exit/record event into a shared list, with each
     * line prefixed by the slash-joined path of enclosing block names so
     * the assertions describe the nesting at a glance.
     */
    private static final class TraceListener implements ParserListener {
        final List<String> events;
        private final String prefix;

        TraceListener() {
            this(new ArrayList<>(), "");
        }

        private TraceListener(List<String> events, String prefix) {
            this.events = events;
            this.prefix = prefix;
        }

        @Override
        public ParserListener enter(Block block) {
            events.add(prefix + "enter(" + block.name() + ")");
            return new TraceListener(events, prefix + block.name() + "/");
        }

        @Override
        public void exit() {
            events.add(prefix + "exit");
        }

        @Override
        public void record(long id, long[] args) {
            events.add(prefix + "record(" + id + "," + args.length + " args)");
        }
    }

    @Test
    void rejectsBytesWithoutSignature() {
        byte[] bogus = { 1, 2, 3, 4, 5, 6, 7, 8 };
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SwiftBitstreamWalker.walk(bogus, ParserListener.DEFAULT));
        assertTrue(ex.getMessage().contains("signature"));
    }

    @Test
    void walksEmptyModuleBlock() {
        // Signature, then an empty MODULE block.
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
            .endBlock();

        TraceListener trace = new TraceListener();
        SwiftBitstreamWalker.walk(w.toByteArray(), trace);

        assertEquals(List.of("enter(MODULE)", "MODULE/exit"), trace.events);
    }

    @Test
    void walksControlBlockNestedInModule() {
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 4)
                .endBlock()
            .endBlock();

        TraceListener trace = new TraceListener();
        SwiftBitstreamWalker.walk(w.toByteArray(), trace);

        assertEquals(List.of(
            "enter(MODULE)",
            "MODULE/enter(CONTROL)",
            "MODULE/CONTROL/exit",
            "MODULE/exit"
        ), trace.events);
    }

    @Test
    void unabbrevRecordsAreDispatchedToListener() {
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 4)
                    .unabbrevRecord(SwiftControlRecord.METADATA, 0L, 976L, 0L, 0L, 0L, 0L, 0L)
                    .unabbrevRecord(SwiftControlRecord.REVISION, 0L)
                .endBlock()
            .endBlock();

        TraceListener trace = new TraceListener();
        SwiftBitstreamWalker.walk(w.toByteArray(), trace);

        assertEquals(List.of(
            "enter(MODULE)",
            "MODULE/enter(CONTROL)",
            "MODULE/CONTROL/record(1,7 args)",       // METADATA = 1, 7 operands
            "MODULE/CONTROL/record(5,1 args)",       // REVISION = 5, 1 operand
            "MODULE/CONTROL/exit",
            "MODULE/exit"
        ), trace.events);
    }

    @Test
    void recordPayloadMatchesEncodedOperandValues() {
        // Make sure operand values round-trip exactly — no truncation, no
        // off-by-one in the VBR encoder/decoder.
        long[] expected = { 0L, 1L, 17L, 976L, 12345L, 1L << 20, 1L << 30 };

        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 4)
                    .unabbrevRecord(42, expected)
                .endBlock()
            .endBlock();

        long[][] captured = new long[1][];
        SwiftBitstreamWalker.walk(w.toByteArray(), new ParserListener() {
            @Override
            public ParserListener enter(Block block) {
                return this;
            }
            @Override
            public void record(long id, long[] args) {
                if (id == 42) {
                    captured[0] = args.clone();
                }
            }
        });

        assertArrayEquals(expected, captured[0]);
    }

    @Test
    void unknownBlockIdsAreSkippedSilently() {
        // ID 99 is not a known Swift block. The walker should skip its
        // contents (Block.lookup returns null and Parser.enter advances
        // past the block size).
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(99, 4)
                    .unabbrevRecord(7, 1L, 2L, 3L)   // discarded
                .endBlock()
                .enterSubblock(Block.CONTROL.id(), 4)
                .endBlock()
            .endBlock();

        TraceListener trace = new TraceListener();
        SwiftBitstreamWalker.walk(w.toByteArray(), trace);

        // The unknown block produces no enter/exit/record events; CONTROL still does.
        assertEquals(List.of(
            "enter(MODULE)",
            "MODULE/enter(CONTROL)",
            "MODULE/CONTROL/exit",
            "MODULE/exit"
        ), trace.events);
    }
}
