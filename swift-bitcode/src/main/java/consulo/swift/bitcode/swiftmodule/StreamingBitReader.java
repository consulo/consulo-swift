/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package consulo.swift.bitcode.swiftmodule;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Forward-only LLVM-bitstream bit reader that pulls bytes lazily from an
 * {@link InputStream} as bits are consumed — never holds the whole file
 * in memory. Bit packing matches {@link uk.ac.man.cs.llvm.bc.Bitstream
 * upstream Bitstream}: little-endian within bytes.
 * <p>
 * Random-access seeks are not supported. The reader is intended for
 * <em>quick-info</em> scans that walk a {@code .swiftmodule} linearly to
 * pluck a few control-block fields (module name, target, format version)
 * without paying the cost of materialising the whole file.
 */
public final class StreamingBitReader {

    private final InputStream in;
    /** Total bits consumed so far. */
    private long bitOffset;
    /**
     * Buffered bits not yet consumed. The next bit returned by
     * {@link #readBits} is at position 0 (LSB).
     */
    private long bufferedBits;
    /** Count of valid bits currently in {@link #bufferedBits} (0..56). */
    private int bufferedBitCount;

    public StreamingBitReader(InputStream in) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        this.in = in;
    }

    public long bitOffset() {
        return bitOffset;
    }

    /** Read up to 56 bits, LSB-first. Throws on premature EOF. */
    public long readBits(int width) throws IOException {
        if (width <= 0) {
            return 0;
        }
        if (width > 56) {
            throw new IllegalArgumentException("readBits width must be <= 56, got " + width);
        }
        fillBufferTo(width);
        long mask = (width == 64) ? -1L : ((1L << width) - 1L);
        long value = bufferedBits & mask;
        bufferedBits >>>= width;
        bufferedBitCount -= width;
        bitOffset += width;
        return value;
    }

    /** Read an LLVM VBR-encoded unsigned integer. */
    public long readVBR(int width) throws IOException {
        if (width < 2) {
            throw new IllegalArgumentException("VBR width must be >= 2, got " + width);
        }
        int dataBits = width - 1;
        long contMask = 1L << dataBits;
        long dataMask = contMask - 1L;
        long value = 0;
        int shift = 0;
        while (true) {
            long chunk = readBits(width);
            value |= (chunk & dataMask) << shift;
            if ((chunk & contMask) == 0) {
                return value;
            }
            shift += dataBits;
            if (shift >= 64) {
                throw new IOException("VBR overflow at bit offset " + bitOffset);
            }
        }
    }

    /** Advance to the next multiple of {@code boundary} bits. */
    public void align(int boundary) throws IOException {
        long mask = boundary - 1;
        long pad = (boundary - (bitOffset & mask)) & mask;
        skipBits(pad);
    }

    /** Skip {@code count} bits without producing a value. */
    public void skipBits(long count) throws IOException {
        if (count <= 0) {
            return;
        }
        // First exhaust the buffer.
        if (bufferedBitCount > 0) {
            int take = (int) Math.min(count, bufferedBitCount);
            bufferedBits >>>= take;
            bufferedBitCount -= take;
            bitOffset += take;
            count -= take;
        }
        // Then skip whole bytes through the stream — we're aligned at this point.
        long wholeBytes = count >>> 3;
        long remaining = wholeBytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            }
            else {
                // skip() can legally return 0 on a still-open stream; force progress with read().
                int b = in.read();
                if (b < 0) {
                    throw new EOFException(
                        "Unexpected EOF while skipping " + count + " bits at offset " + bitOffset);
                }
                remaining--;
            }
        }
        long byteBits = wholeBytes << 3;
        bitOffset += byteBits;
        count -= byteBits;
        // Finally any sub-byte tail.
        if (count > 0) {
            fillBufferTo((int) count);
            bufferedBits >>>= count;
            bufferedBitCount -= count;
            bitOffset += count;
        }
    }

    /** Read {@code n} bytes, byte-aligned. */
    public byte[] readBytes(int n) throws IOException {
        if ((bitOffset & 7L) != 0) {
            throw new IllegalStateException(
                "readBytes requires byte-aligned position, got bit offset " + bitOffset);
        }
        if (bufferedBitCount != 0) {
            throw new IllegalStateException(
                "Internal: byte-aligned but bufferedBitCount=" + bufferedBitCount);
        }
        byte[] out = new byte[n];
        int total = 0;
        while (total < n) {
            int read = in.read(out, total, n - total);
            if (read < 0) {
                throw new EOFException(
                    "Unexpected EOF reading " + n + " bytes at offset " + bitOffset);
            }
            total += read;
        }
        bitOffset += (long) n << 3;
        return out;
    }

    /** True if {@code width} more bits can be read without EOF. */
    public boolean hasAtLeast(int width) {
        if (bufferedBitCount >= width) {
            return true;
        }
        // Best-effort: fill the buffer and see how many bits we actually got.
        try {
            fillBufferTo(width);
            return bufferedBitCount >= width;
        }
        catch (IOException e) {
            return false;
        }
    }

    private void fillBufferTo(int width) throws IOException {
        while (bufferedBitCount < width) {
            int b = in.read();
            if (b < 0) {
                if (bufferedBitCount >= width) {
                    return;
                }
                throw new EOFException(
                    "Unexpected EOF reading " + width + " bits at offset " + bitOffset
                        + " (only " + bufferedBitCount + " buffered)");
            }
            bufferedBits |= ((long) (b & 0xFF)) << bufferedBitCount;
            bufferedBitCount += 8;
        }
    }
}
