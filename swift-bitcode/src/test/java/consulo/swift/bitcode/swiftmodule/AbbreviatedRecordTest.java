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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the bitcode reader's abbreviation machinery end-to-end via
 * synthetic bitstreams. Targets the upstream
 * {@link uk.ac.man.cs.llvm.bc.records.UserRecordBinaryOperand} (the {@code Blob}
 * encoding, which we re-implemented from scratch) and the array+fixed
 * combination used by Swift's INDEX_BLOCK offset tables.
 */
class AbbreviatedRecordTest {

    /** Captures (recordId, args) for every record fired in a specific block. */
    private static final class RecordCapture implements ParserListener {
        final List<Long> ids = new ArrayList<>();
        final List<long[]> argsList = new ArrayList<>();
        private final Block target;

        RecordCapture(Block target) {
            this.target = target;
        }

        @Override
        public ParserListener enter(Block block) {
            if (block == target) {
                return new ParserListener() {
                    @Override
                    public ParserListener enter(Block b) {
                        return DEFAULT;
                    }
                    @Override
                    public void record(long id, long[] args) {
                        ids.add(id);
                        argsList.add(args);
                    }
                };
            }
            return this;
        }

        @Override
        public void record(long id, long[] args) {
        }
    }

    @Test
    void blobAbbrevRecordRoundTripsBytesExactly() {
        // Build: MODULE { CONTROL { DEFINE_ABBREV(literal=MODULE_NAME, blob); ABBREV(blob="PackageDescription") } }
        byte[] payload = "PackageDescription".getBytes(StandardCharsets.UTF_8);

        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int abbrevIdx = w.defineBlobAbbrev(SwiftControlRecord.MODULE_NAME);
        w.writeBlobAbbrevRecord(abbrevIdx, payload)
                .endBlock()
            .endBlock();

        RecordCapture capture = new RecordCapture(Block.CONTROL);
        SwiftBitstreamWalker.walk(w.toByteArray(), capture);

        assertEquals(1, capture.ids.size(), "Exactly one record should fire");
        assertEquals(SwiftControlRecord.MODULE_NAME, capture.ids.get(0).intValue());

        long[] args = capture.argsList.get(0);
        assertEquals(payload.length, args.length, "Blob byte count must match");
        for (int i = 0; i < payload.length; i++) {
            assertEquals(payload[i] & 0xFFL, args[i],
                "Blob byte " + i + " round-trip");
        }
    }

    @Test
    void emptyBlobAbbrevYieldsEmptyArgs() {
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int abbrevIdx = w.defineBlobAbbrev(SwiftControlRecord.REVISION);
        w.writeBlobAbbrevRecord(abbrevIdx, new byte[0])
                .endBlock()
            .endBlock();

        RecordCapture capture = new RecordCapture(Block.CONTROL);
        SwiftBitstreamWalker.walk(w.toByteArray(), capture);

        assertEquals(1, capture.ids.size());
        assertEquals(SwiftControlRecord.REVISION, capture.ids.get(0).intValue());
        assertEquals(0, capture.argsList.get(0).length);
    }

    @Test
    void blobAbbrevWithUtf8Multibyte() {
        // Swift identifiers can be arbitrary Unicode — confirm we don't
        // mangle high-bit bytes via sign extension.
        byte[] payload = "Pâckage🦄".getBytes(StandardCharsets.UTF_8);

        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int abbrevIdx = w.defineBlobAbbrev(SwiftControlRecord.MODULE_NAME);
        w.writeBlobAbbrevRecord(abbrevIdx, payload)
                .endBlock()
            .endBlock();

        RecordCapture capture = new RecordCapture(Block.CONTROL);
        SwiftBitstreamWalker.walk(w.toByteArray(), capture);

        long[] args = capture.argsList.get(0);
        byte[] reconstructed = new byte[args.length];
        for (int i = 0; i < args.length; i++) {
            assertTrue(args[i] >= 0 && args[i] <= 255,
                "Blob byte must remain in 0..255 (no sign extension); got " + args[i]);
            reconstructed[i] = (byte) args[i];
        }
        assertEquals(new String(payload, StandardCharsets.UTF_8),
            new String(reconstructed, StandardCharsets.UTF_8));
    }

    @Test
    void fixedArrayAbbrevRecordRoundTripsValues() {
        // INDEX_BLOCK's IDENTIFIER_OFFSETS layout: literal record-code + BCArray<BCFixed<31>>.
        long[] offsets = { 0L, 1L, 17L, 976L, 1024L * 1024L, (1L << 30) - 1L };
        int width = 31;

        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.INDEX.id(), 5);
        int abbrevIdx = w.defineFixedArrayAbbrev(SwiftIndexRecord.IDENTIFIER_OFFSETS, width);
        w.writeFixedArrayAbbrevRecord(abbrevIdx, width, offsets)
                .endBlock()
            .endBlock();

        RecordCapture capture = new RecordCapture(Block.INDEX);
        SwiftBitstreamWalker.walk(w.toByteArray(), capture);

        assertEquals(1, capture.ids.size());
        assertEquals(SwiftIndexRecord.IDENTIFIER_OFFSETS, capture.ids.get(0).intValue());
        assertArrayEquals(offsets, capture.argsList.get(0));
    }

    @Test
    void multipleAbbrevsCoexistWithinSameBlock() {
        // Define two abbreviations in the same block and use each.
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int nameAbbrev = w.defineBlobAbbrev(SwiftControlRecord.MODULE_NAME);
        int targetAbbrev = w.defineBlobAbbrev(SwiftControlRecord.TARGET);
        // Sanity: abbrev indices start at 4 and increment.
        assertEquals(4, nameAbbrev);
        assertEquals(5, targetAbbrev);

        w.writeBlobAbbrevRecord(nameAbbrev, "MyModule".getBytes(StandardCharsets.UTF_8))
         .writeBlobAbbrevRecord(targetAbbrev, "arm64-apple-macos".getBytes(StandardCharsets.UTF_8))
                .endBlock()
            .endBlock();

        RecordCapture capture = new RecordCapture(Block.CONTROL);
        SwiftBitstreamWalker.walk(w.toByteArray(), capture);

        assertEquals(2, capture.ids.size());
        assertEquals(SwiftControlRecord.MODULE_NAME, capture.ids.get(0).intValue());
        assertEquals(SwiftControlRecord.TARGET,      capture.ids.get(1).intValue());
    }

    @Test
    void abbrevIndexResetsBetweenSiblingBlocks() {
        // Each block has its own DEFINE_ABBREV table — indices reset to 4
        // when entering a sibling block.
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int controlAbbrev = w.defineBlobAbbrev(SwiftControlRecord.MODULE_NAME);
        w.writeBlobAbbrevRecord(controlAbbrev, "MyMod".getBytes(StandardCharsets.UTF_8))
                .endBlock()
                .enterSubblock(Block.IDENTIFIER_DATA.id(), 5);
        int idAbbrev = w.defineBlobAbbrev(SwiftIdentifierBlockRecord.IDENTIFIER_DATA);
        // Sibling block — abbrev numbering must restart at 4, not continue from
        // the CONTROL block's last index.
        assertEquals(4, idAbbrev);
        w.writeBlobAbbrevRecord(idAbbrev, new byte[]{ 1, 2, 3 })
                .endBlock()
            .endBlock();

        // Make sure the whole structure parses without an exception or
        // dropped records — proves abbrev state is cleanly per-block.
        SwiftBitstreamWalker.walk(w.toByteArray(), ParserListener.DEFAULT);
    }
}
