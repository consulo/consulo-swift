/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.Operation;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Test-only LLVM bitstream <em>encoder</em>. The format is little-endian-
 * within-bytes — bit {@code i} of every value goes into byte {@code i/8} at
 * bit position {@code i%8} (LSB first). Matches exactly what
 * {@link uk.ac.man.cs.llvm.bc.Bitstream#readVBR Bitstream.readVBR} expects.
 * <p>
 * Used by unit tests to synthesize minimal {@code .swiftmodule}-shaped
 * byte sequences so the readers can be exercised in isolation from the
 * full PackageDescription fixture.
 */
public final class BitstreamWriter {
    /** Initial abbreviation width — same as upstream's {@code Parser} default. */
    public static final int INITIAL_ID_SIZE = 2;

    /** Reserved opcode IDs at the top of every block. */
    public static final int OP_END_BLOCK       = 0;
    public static final int OP_ENTER_SUBBLOCK  = 1;
    public static final int OP_DEFINE_ABBREV   = 2;
    public static final int OP_UNABBREV_RECORD = 3;

    private byte[] buf = new byte[256];
    private long bitOffset = 0;

    /**
     * Stack of frames for each open ENTER_SUBBLOCK. On {@link #endBlock()} we
     * pop and back-patch the block-length field and restore the outer
     * abbreviation width + next-abbrev index.
     */
    private final Deque<Frame> blockStack = new ArrayDeque<>();
    private int currentIdSize = INITIAL_ID_SIZE;

    /**
     * Next abbreviation ID to hand out in the current block. The four
     * reserved opcodes 0..3 are pre-occupied (END/ENTER/DEFINE/UNABBREV)
     * so user abbreviations start at 4.
     */
    private int nextAbbrevIndex = 4;

    // ---------------------------------------------------------------- low-level

    public void writeBits(long value, int width) {
        if (width <= 0) {
            return;
        }
        ensureCapacityForBits(width);
        for (int i = 0; i < width; i++) {
            long bit = (value >>> i) & 1L;
            if (bit == 0) {
                continue;   // buffer starts zero-filled
            }
            int byteIdx = (int) ((bitOffset + i) >>> 3);
            int bitIdx  = (int) ((bitOffset + i) & 7);
            buf[byteIdx] |= (byte) (1 << bitIdx);
        }
        bitOffset += width;
    }

    public void writeVBR(long value, int width) {
        if (width < 2) {
            throw new IllegalArgumentException("VBR width must be >= 2, got " + width);
        }
        int dataBits = width - 1;
        long dataMask = (1L << dataBits) - 1L;
        while (true) {
            long data = value & dataMask;
            long rest = value >>> dataBits;
            boolean more = rest != 0;
            long chunk = data | (more ? (1L << dataBits) : 0L);
            writeBits(chunk, width);
            if (!more) {
                return;
            }
            value = rest;
        }
    }

    public void align(int boundary) {
        long mask = boundary - 1;
        long pad = (boundary - (bitOffset & mask)) & mask;
        if (pad > 0) {
            // Buffer is zero-filled, so we just need to advance.
            ensureCapacityForBits(pad);
            bitOffset += pad;
        }
    }

    public long bitOffset() {
        return bitOffset;
    }

    public byte[] toByteArray() {
        align(8);
        return Arrays.copyOf(buf, (int) (bitOffset >>> 3));
    }

    // ---------------------------------------------------------- mid-level: ops

    /** Writes the 4-byte {@code .swiftmodule} signature at the current offset. */
    public BitstreamWriter writeSwiftModuleSignature() {
        if (bitOffset != 0) {
            throw new IllegalStateException("Signature must be at offset 0");
        }
        writeBits(SwiftModuleSignature.SPARKLE_BYTE_0 & 0xFF, 8);
        writeBits(SwiftModuleSignature.SPARKLE_BYTE_1 & 0xFF, 8);
        writeBits(SwiftModuleSignature.SPARKLE_BYTE_2 & 0xFF, 8);
        writeBits(SwiftModuleSignature.TAG_SWIFTMODULE & 0xFF, 8);
        return this;
    }

    /**
     * Emit an ENTER_SUBBLOCK opcode and switch into the new block's
     * abbreviation width. The block length is back-patched by
     * {@link #endBlock()}.
     */
    public BitstreamWriter enterSubblock(int blockId, int newIdSize) {
        writeBits(OP_ENTER_SUBBLOCK, currentIdSize);
        writeVBR(blockId, 8);
        writeVBR(newIdSize, 4);
        align(32);
        // Reserve 32 bits for block length (in 32-bit words). Patched in endBlock().
        long sizeFieldBitOffset = bitOffset;
        writeBits(0, 32);
        blockStack.push(new Frame(currentIdSize, sizeFieldBitOffset, nextAbbrevIndex));
        currentIdSize = newIdSize;
        nextAbbrevIndex = 4;   // reset abbrev numbering inside the new block
        return this;
    }

    /**
     * Closes the current subblock, patches its block-length header, and
     * restores the enclosing abbreviation width.
     */
    public BitstreamWriter endBlock() {
        if (blockStack.isEmpty()) {
            throw new IllegalStateException("endBlock() with no open block");
        }
        writeBits(OP_END_BLOCK, currentIdSize);
        align(32);

        Frame f = blockStack.pop();
        long blockEnd = bitOffset;
        long blockBodyStart = f.sizeFieldBitOffset + 32;   // 32-bit-aligned by construction
        long sizeWords = (blockEnd - blockBodyStart) >>> 5;
        patchBitsAtOffset(f.sizeFieldBitOffset, sizeWords, 32);

        currentIdSize = f.outerIdSize;
        nextAbbrevIndex = f.outerNextAbbrevIndex;
        return this;
    }

    /**
     * Emit an UNABBREV_RECORD with the given record-code and VBR(6) operands.
     */
    public BitstreamWriter unabbrevRecord(int recordCode, long... ops) {
        writeBits(OP_UNABBREV_RECORD, currentIdSize);
        writeVBR(recordCode, 6);
        writeVBR(ops.length, 6);
        for (long op : ops) {
            writeVBR(op, 6);
        }
        return this;
    }

    // ---------------------------------------------------------- blob

    /**
     * Emit just the body of a BCBlob operand (length VBR(6), align(32),
     * bytes, align(32)). The caller is responsible for surrounding it with
     * the appropriate record framing.
     */
    public BitstreamWriter writeBlobBody(byte[] data) {
        writeVBR(data.length, 6);
        align(32);
        for (byte b : data) {
            writeBits(b & 0xFFL, 8);
        }
        align(32);
        return this;
    }

    // -------------------------------------------------- abbreviation support

    /**
     * Encoding code-numbers used in DEFINE_ABBREV. Mirror upstream
     * {@link uk.ac.man.cs.llvm.bc.records.UserRecordOperand}'s
     * {@code parse()} dispatch.
     */
    private static final int ENC_FIXED = 1;
    private static final int ENC_VBR   = 2;
    private static final int ENC_ARRAY = 3;
    private static final int ENC_CHAR6 = 4;
    private static final int ENC_BLOB  = 5;

    /**
     * Define an abbreviation whose first operand is a literal record-code
     * and whose second operand is a {@code BCBlob}. Used by Swift for
     * MODULE_NAME, TARGET, REVISION, SDK_NAME, etc. Returns the abbreviation
     * index that subsequent {@link #writeBlobAbbrevRecord} calls should use.
     */
    public int defineBlobAbbrev(int recordCode) {
        writeBits(OP_DEFINE_ABBREV, currentIdSize);
        writeVBR(2, 5);                         // numabbrevops
        writeLiteralOp(recordCode);
        writeEncodedOp(ENC_BLOB);
        return nextAbbrevIndex++;
    }

    /**
     * Define an abbreviation: literal record-code, then an array of
     * fixed-width values. Used by Swift's INDEX_BLOCK for DECL_OFFSETS,
     * TYPE_OFFSETS, IDENTIFIER_OFFSETS, ORDERED_TOP_LEVEL_DECLS.
     *
     * @param recordCode    literal record kind (the abbreviation's first op)
     * @param elementWidth  width in bits of each array element (typically 31)
     */
    public int defineFixedArrayAbbrev(int recordCode, int elementWidth) {
        writeBits(OP_DEFINE_ABBREV, currentIdSize);
        writeVBR(3, 5);                         // numabbrevops: literal, array, element
        writeLiteralOp(recordCode);
        writeEncodedOp(ENC_ARRAY);
        writeEncodedOp(ENC_FIXED, elementWidth);
        return nextAbbrevIndex++;
    }

    /**
     * Emit an abbreviated record using a previously-defined blob abbrev.
     */
    public BitstreamWriter writeBlobAbbrevRecord(int abbrevIndex, byte[] data) {
        writeBits(abbrevIndex, currentIdSize);
        writeBlobBody(data);
        return this;
    }

    /**
     * Emit an abbreviated record using a previously-defined fixed-array abbrev.
     */
    public BitstreamWriter writeFixedArrayAbbrevRecord(int abbrevIndex, int elementWidth, long... values) {
        writeBits(abbrevIndex, currentIdSize);
        writeVBR(values.length, 6);
        for (long v : values) {
            writeBits(v, elementWidth);
        }
        return this;
    }

    private void writeLiteralOp(long value) {
        writeBits(1, 1);                        // literal flag
        writeVBR(value, 8);
    }

    private void writeEncodedOp(int encoding) {
        writeBits(0, 1);                        // encoded flag
        writeBits(encoding, 3);
    }

    private void writeEncodedOp(int encoding, int width) {
        writeEncodedOp(encoding);
        writeVBR(width, 5);
    }

    // ----------------------------------------------------------------- internals

    private void ensureCapacityForBits(long bits) {
        long needed = ((bitOffset + bits + 7) >>> 3);
        if (needed > buf.length) {
            int newLen = Math.max(buf.length * 2, (int) needed);
            buf = Arrays.copyOf(buf, newLen);
        }
    }

    private void patchBitsAtOffset(long offset, long value, int width) {
        for (int i = 0; i < width; i++) {
            long bit = (value >>> i) & 1L;
            int byteIdx = (int) ((offset + i) >>> 3);
            int bitIdx  = (int) ((offset + i) & 7);
            if (bit != 0) {
                buf[byteIdx] |= (byte) (1 << bitIdx);
            }
            else {
                buf[byteIdx] &= (byte) ~(1 << bitIdx);
            }
        }
    }

    private record Frame(int outerIdSize, long sizeFieldBitOffset, int outerNextAbbrevIndex) {}

    // The Operation imports are unused at runtime; they are referenced
    // implicitly via the OP_* constants above and exist to make the
    // intent of those constants traceable to upstream code.
    @SuppressWarnings("unused")
    private static final Class<?> UPSTREAM_OPERATION_LINK = Operation.class;
}
