/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.blocks.Block;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fast, low-memory scanner that walks just the {@code CONTROL_BLOCK} of a
 * {@code .swiftmodule} via {@link StreamingBitReader} and returns the
 * module's identity (name, target, format version, etc.) <em>without
 * loading the whole file</em>. Everything past CONTROL is skipped using
 * the block-length headers, so a 30 MiB module yields its name after a
 * few hundred bytes of I/O.
 * <p>
 * Implementation notes:
 * <ul>
 *   <li>Reserved opcodes (END_BLOCK, ENTER_SUBBLOCK, DEFINE_ABBREV,
 *       UNABBREV_RECORD) are handled inline. Abbreviated records are
 *       decoded for the operand shapes Swift's CONTROL block actually
 *       uses — a literal record-code followed by a {@code BCBlob}.</li>
 *   <li>Once CONTROL is exited, we stop reading entirely; the remainder
 *       of the file never touches our buffer.</li>
 * </ul>
 */
public final class StreamingSwiftModuleScanner {

    private static final int OP_END_BLOCK       = 0;
    private static final int OP_ENTER_SUBBLOCK  = 1;
    private static final int OP_DEFINE_ABBREV   = 2;
    private static final int OP_UNABBREV_RECORD = 3;

    /** Abbrev operand encodings, mirrored from upstream UserRecordOperand. */
    private static final int ENC_FIXED = 1;
    private static final int ENC_VBR   = 2;
    private static final int ENC_ARRAY = 3;
    private static final int ENC_CHAR6 = 4;
    private static final int ENC_BLOB  = 5;

    /**
     * Scan the stream and return the module's identity from its CONTROL
     * block. The {@link SwiftModuleInfo} returned has no decl/type/identifier
     * offsets populated — for full parsing use {@link SwiftModuleReader}.
     *
     * @param in stream positioned at byte 0 of a {@code .swiftmodule}
     */
    public static SwiftModuleInfo quickInfo(InputStream in) throws IOException {
        // Validate signature first — 4 bytes only.
        byte[] sig = new byte[SwiftModuleSignature.LENGTH];
        int sigRead = 0;
        while (sigRead < sig.length) {
            int n = in.read(sig, sigRead, sig.length - sigRead);
            if (n < 0) {
                throw new IOException("Stream ended before .swiftmodule signature");
            }
            sigRead += n;
        }
        if (!SwiftModuleSignature.isSwiftModule(sig)) {
            throw new IOException("Not a Swift module file (bad signature)");
        }

        StreamingBitReader r = new StreamingBitReader(in);
        SwiftModuleInfo info = new SwiftModuleInfo();
        Scanner scanner = new Scanner(r, info);
        scanner.walkRoot();
        return info;
    }

    private StreamingSwiftModuleScanner() {
    }

    // --------------------------------------------------------- driver

    private static final class Scanner {
        private final StreamingBitReader r;
        private final SwiftModuleInfo info;
        private boolean stop = false;

        Scanner(StreamingBitReader r, SwiftModuleInfo info) {
            this.r = r;
            this.info = info;
        }

        void walkRoot() throws IOException {
            // Root abbreviation width is 2 — same as upstream Parser default.
            walkBlock(2, -1);
        }

        /**
         * Walk records inside the current block until END_BLOCK or until
         * {@code stop} is set (CONTROL fully captured).
         *
         * @param idSize  abbreviation-id width in bits for this block
         * @param blockId the block ID we're inside, or {@code -1} at root
         */
        private void walkBlock(int idSize, int blockId) throws IOException {
            List<AbbrevDef> abbrevs = new ArrayList<>();
            while (!stop && r.hasAtLeast(idSize)) {
                long opcode = r.readBits(idSize);
                if (opcode == OP_END_BLOCK) {
                    r.align(32);
                    return;
                }
                if (opcode == OP_ENTER_SUBBLOCK) {
                    long innerId = r.readVBR(8);
                    long innerIdSize = r.readVBR(4);
                    r.align(32);
                    long sizeWords = r.readBits(32);
                    long bodyBits = sizeWords << 5;
                    // MODULE is the outer envelope around CONTROL — we have to
                    // descend into it to reach the records we want. CONTROL
                    // itself is where they live. Everything else (OPTIONS,
                    // INPUT, DECLS_AND_TYPES, INDEX, IDENTIFIER_DATA, SIL, ...)
                    // is skipped wholesale via its block-length header.
                    if (innerId == Block.MODULE.id() || innerId == Block.CONTROL.id()) {
                        walkBlock((int) innerIdSize, (int) innerId);
                    }
                    else {
                        r.skipBits(bodyBits);
                    }
                    continue;
                }
                if (opcode == OP_DEFINE_ABBREV) {
                    abbrevs.add(parseAbbrev());
                    continue;
                }
                if (opcode == OP_UNABBREV_RECORD) {
                    long recordId = r.readVBR(6);
                    long count = r.readVBR(6);
                    long[] args = new long[(int) count];
                    for (int i = 0; i < count; i++) {
                        args[i] = r.readVBR(6);
                    }
                    if (blockId == Block.CONTROL.id()) {
                        applyControlRecord((int) recordId, args);
                    }
                    continue;
                }
                // Abbreviated record (>= 4)
                int abbrevIdx = (int) opcode - 4;
                if (abbrevIdx < 0 || abbrevIdx >= abbrevs.size()) {
                    throw new IOException("Unknown abbreviation id " + opcode
                        + " at bit offset " + r.bitOffset());
                }
                AbbrevDef def = abbrevs.get(abbrevIdx);
                long[] args = decodeAbbrev(def);
                if (blockId == Block.CONTROL.id()) {
                    applyControlRecord((int) def.literalId, args);
                }
            }
        }

        private void applyControlRecord(int recordId, long[] args) {
            switch (recordId) {
                case SwiftControlRecord.METADATA:
                    if (args.length >= 2) {
                        info.setFormatMajor((int) args[0]);
                        info.setFormatMinor((int) args[1]);
                    }
                    break;
                case SwiftControlRecord.MODULE_NAME:
                    info.setModuleName(bytes(args));
                    // We have what we need — CONTROL has nothing else
                    // we want past this point. Stop the entire walk.
                    if (info.getTarget() != null && info.getFormatMajor() >= 0) {
                        stop = true;
                    }
                    break;
                case SwiftControlRecord.TARGET:
                    info.setTarget(bytes(args));
                    if (info.getModuleName() != null && info.getFormatMajor() >= 0) {
                        stop = true;
                    }
                    break;
                case SwiftControlRecord.SDK_NAME:
                    info.setSdkName(bytes(args));
                    break;
                case SwiftControlRecord.REVISION:
                    info.setRevision(bytes(args));
                    break;
                case SwiftControlRecord.CHANNEL:
                    info.setChannel(bytes(args));
                    break;
                case SwiftControlRecord.SDK_VERSION:
                    info.setSdkVersion(bytes(args));
                    break;
                default:
                    // ALLOWABLE_CLIENT_NAME etc. — ignored for quick info
            }
        }

        private AbbrevDef parseAbbrev() throws IOException {
            int count = (int) r.readVBR(5);
            long literalId = -1;
            // Track the data ops only (after the leading literal record-id).
            List<AbbrevOp> dataOps = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                boolean isLiteral = r.readBits(1) == 1;
                if (isLiteral) {
                    long value = r.readVBR(8);
                    if (i == 0) {
                        literalId = value;
                    }
                    // Non-leading literals are unusual; capture as zero-arg op.
                    continue;
                }
                int encoding = (int) r.readBits(3);
                switch (encoding) {
                    case ENC_FIXED:
                    case ENC_VBR:
                        int width = (int) r.readVBR(5);
                        dataOps.add(new AbbrevOp(encoding, width, null));
                        break;
                    case ENC_ARRAY:
                        // Next op describes the element type. Consume it now.
                        boolean elLit = r.readBits(1) == 1;
                        if (elLit) {
                            r.readVBR(8); // literal element — not supported in array, skip value
                            dataOps.add(new AbbrevOp(ENC_ARRAY, 0,
                                new AbbrevOp(ENC_FIXED, 0, null)));
                        }
                        else {
                            int innerEnc = (int) r.readBits(3);
                            int innerWidth = (innerEnc == ENC_FIXED || innerEnc == ENC_VBR)
                                ? (int) r.readVBR(5) : 0;
                            dataOps.add(new AbbrevOp(ENC_ARRAY, 0,
                                new AbbrevOp(innerEnc, innerWidth, null)));
                        }
                        i++; // we just consumed the inner op slot
                        break;
                    case ENC_CHAR6:
                    case ENC_BLOB:
                        dataOps.add(new AbbrevOp(encoding, 0, null));
                        break;
                    default:
                        throw new IOException("Unknown abbreviation operand encoding " + encoding);
                }
            }
            return new AbbrevDef(literalId, dataOps);
        }

        private long[] decodeAbbrev(AbbrevDef def) throws IOException {
            List<long[]> chunks = new ArrayList<>();
            int total = 0;
            for (AbbrevOp op : def.ops) {
                long[] vals = decodeOp(op);
                chunks.add(vals);
                total += vals.length;
            }
            long[] out = new long[total];
            int idx = 0;
            for (long[] c : chunks) {
                System.arraycopy(c, 0, out, idx, c.length);
                idx += c.length;
            }
            return out;
        }

        private long[] decodeOp(AbbrevOp op) throws IOException {
            switch (op.encoding) {
                case ENC_FIXED:
                    return new long[]{ r.readBits(op.width) };
                case ENC_VBR:
                    return new long[]{ r.readVBR(op.width) };
                case ENC_CHAR6:
                    return new long[]{ r.readBits(6) };
                case ENC_BLOB: {
                    int len = (int) r.readVBR(6);
                    r.align(32);
                    byte[] bytes = r.readBytes(len);
                    r.align(32);
                    long[] out = new long[len];
                    for (int i = 0; i < len; i++) {
                        out[i] = bytes[i] & 0xFFL;
                    }
                    return out;
                }
                case ENC_ARRAY: {
                    int len = (int) r.readVBR(6);
                    long[] out = new long[len];
                    for (int i = 0; i < len; i++) {
                        long[] vals = decodeOp(op.element);
                        out[i] = vals[0];      // arrays of single-value ops
                    }
                    return out;
                }
                default:
                    throw new IOException("Unsupported encoding " + op.encoding);
            }
        }
    }

    private record AbbrevOp(int encoding, int width, AbbrevOp element) {}

    private record AbbrevDef(long literalId, List<AbbrevOp> ops) {}

    private static String bytes(long[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        byte[] b = new byte[args.length];
        for (int i = 0; i < args.length; i++) {
            b[i] = (byte) (args[i] & 0xFF);
        }
        int len = b.length;
        while (len > 0 && b[len - 1] == 0) {
            len--;
        }
        return new String(b, 0, len, StandardCharsets.UTF_8);
    }
}
