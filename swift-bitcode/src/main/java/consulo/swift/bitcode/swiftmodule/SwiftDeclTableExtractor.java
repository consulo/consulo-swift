/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.blocks.Block;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks {@code DECLS_AND_TYPES_BLOCK} once and pulls out both
 * {@link SwiftDeclEntry} (one per non-zero {@code INDEX_BLOCK.DECL_OFFSETS}
 * entry) and {@link SwiftTypeEntry} (one per non-zero
 * {@code INDEX_BLOCK.TYPE_OFFSETS} entry). Single pass over the bytes —
 * Swift interleaves type and decl records in the same block, so it's
 * cheaper to capture every record once than to walk twice.
 *
 * <p>The walker descends only into {@code MODULE -> DECLS_AND_TYPES}; every
 * other sub-block (OPTIONS, INPUT, INDEX, SIL, IDENTIFIER_DATA, ...) is
 * skipped wholesale via its block-length header. Inside
 * {@code DECLS_AND_TYPES} we capture every record's start offset; phase 2
 * then resolves each wanted offset to the matching decl or type record.
 *
 * <p>Abbreviation handling is the same shape as
 * {@link StreamingSwiftModuleScanner} — Swift's {@code DECLS_AND_TYPES}
 * block uses the same operand encodings: literal record-id, {@code BCFixed},
 * {@code BCVBR}, {@code BCArray}, {@code BCChar6}, {@code BCBlob}. We
 * decode them inline here rather than going through the upstream
 * {@code UserRecordBuilder} so we can track the bit-offset at the start of
 * each record (information the upstream parser doesn't expose to listeners).
 */
public final class SwiftDeclTableExtractor {

    private static final int OP_END_BLOCK       = 0;
    private static final int OP_ENTER_SUBBLOCK  = 1;
    private static final int OP_DEFINE_ABBREV   = 2;
    private static final int OP_UNABBREV_RECORD = 3;

    private static final int ENC_FIXED = 1;
    private static final int ENC_VBR   = 2;
    private static final int ENC_ARRAY = 3;
    private static final int ENC_CHAR6 = 4;
    private static final int ENC_BLOB  = 5;

    /** Combined output: arrays parallel to {@code declOffsets} / {@code typeOffsets}. */
    public record Result(SwiftDeclEntry[] declEntries, SwiftTypeEntry[] typeEntries) {
    }

    /**
     * Resolve every decl whose offset appears in {@code declOffsets} to a
     * {@link SwiftDeclEntry}. The returned array is parallel to
     * {@code declOffsets}: entry {@code i} corresponds to declID
     * {@code i + 1}. Slots for unresolved IDs (e.g. {@code declOffsets[i] == 0})
     * are {@code null}.
     */
    public static SwiftDeclEntry[] extract(byte[] moduleBytes, long[] declOffsets) {
        return extractAll(moduleBytes, declOffsets, new long[0]).declEntries();
    }

    /**
     * Same as {@link #extract(byte[], long[])} but also resolves
     * {@code typeOffsets} to {@link SwiftTypeEntry} objects. Single linear
     * pass over the byte stream regardless of which arrays are populated.
     */
    public static Result extractAll(byte[] moduleBytes, long[] declOffsets, long[] typeOffsets) {
        if (moduleBytes == null) {
            throw new NullPointerException("moduleBytes");
        }
        long[] decls = declOffsets == null ? new long[0] : declOffsets;
        long[] types = typeOffsets == null ? new long[0] : typeOffsets;
        if (decls.length == 0 && types.length == 0) {
            return new Result(new SwiftDeclEntry[0], new SwiftTypeEntry[0]);
        }

        // Phase 1 — walk DECLS_AND_TYPES_BLOCK once, capturing every record.
        // The wanted-offset set is the union of decl and type offsets: it
        // arms the walker on the first occurrence of either category.
        Set<Long> wanted = new HashSet<>((decls.length + types.length) * 2);
        for (long off : decls) {
            if (off > 0) {
                wanted.add(off);
            }
        }
        for (long off : types) {
            if (off > 0) {
                wanted.add(off);
            }
        }

        List<RawRecord> records;
        try (ByteArrayInputStream in = new ByteArrayInputStream(moduleBytes)) {
            // Skip the 4-byte sparkle signature; the bitstream proper starts at bit 32
            // and that's the frame DECL_OFFSETS/TYPE_OFFSETS records sit in.
            long sigSkipped = 0;
            while (sigSkipped < SwiftModuleSignature.LENGTH) {
                long n = in.skip(SwiftModuleSignature.LENGTH - sigSkipped);
                if (n <= 0) {
                    throw new IOException("Module truncated before signature");
                }
                sigSkipped += n;
            }
            StreamingBitReader r = new StreamingBitReader(in);
            Walker walker = new Walker(r, wanted, SwiftModuleSignature.LENGTH * 8L);
            walker.walkRoot();
            records = walker.records;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Phase 2 — for each non-zero wanted offset, find the matching record.
        // Records were captured in walk order, so they're already sorted by
        // offset; a binary search suffices.
        long[] capturedOffsets = new long[records.size()];
        for (int i = 0; i < capturedOffsets.length; i++) {
            capturedOffsets[i] = records.get(i).offset;
        }

        SwiftDeclEntry[] declEntries = resolveDecls(decls, records, capturedOffsets, wanted);
        SwiftTypeEntry[] typeEntries = resolveTypes(types, records, capturedOffsets, wanted);
        return new Result(declEntries, typeEntries);
    }

    private static SwiftDeclEntry[] resolveDecls(
        long[] declOffsets,
        List<RawRecord> records,
        long[] capturedOffsets,
        Set<Long> wanted
    ) {
        SwiftDeclEntry[] entries = new SwiftDeclEntry[declOffsets.length];
        for (int i = 0; i < declOffsets.length; i++) {
            long off = declOffsets[i];
            if (off <= 0) {
                continue;
            }
            int idx = findOffset(capturedOffsets, off);
            if (idx < 0) {
                continue;
            }
            // 1) Walk forward through prelude records (DESERIALIZATION_SAFETY,
            //    ERROR_FLAG, ABI_ONLY_COUNTERPART, *_DECL_ATTR codes 180+) to
            //    reach the actual decl record. CAPTURE the preludes so the
            //    printer can emit them as @attributes.
            int declIdx = -1;
            List<SwiftAuxRecord> preludes = null;
            for (int j = idx; j < records.size(); j++) {
                RawRecord rec = records.get(j);
                if (SwiftDeclRecord.isDecl(rec.kind) || rec.kind == SwiftDeclRecord.XREF) {
                    declIdx = j;
                    break;
                }
                if (j > idx && wanted.contains(rec.offset)) {
                    break;
                }
                if (preludes == null) {
                    preludes = new ArrayList<>(2);
                }
                preludes.add(new SwiftAuxRecord(rec.kind, rec.args));
            }
            if (declIdx < 0) {
                continue;
            }
            RawRecord decl = records.get(declIdx);

            // 2) Capture trailing records (PARAMETERLIST, MEMBERS,
            //    GENERIC_PARAM_LIST, INHERITED_PROTOCOLS, ...) until the next
            //    wanted entity boundary. These give us methods/members/params
            //    that the printer needs to emit a complete declaration.
            List<SwiftAuxRecord> trailers = null;
            for (int j = declIdx + 1; j < records.size(); j++) {
                RawRecord rec = records.get(j);
                if (wanted.contains(rec.offset)) {
                    break;
                }
                if (trailers == null) {
                    trailers = new ArrayList<>(4);
                }
                trailers.add(new SwiftAuxRecord(rec.kind, rec.args));
            }
            SwiftAuxRecord[] preludeArr = preludes == null
                ? new SwiftAuxRecord[0]
                : preludes.toArray(new SwiftAuxRecord[0]);
            SwiftAuxRecord[] trailerArr = trailers == null
                ? new SwiftAuxRecord[0]
                : trailers.toArray(new SwiftAuxRecord[0]);
            entries[i] = new SwiftDeclEntry(i + 1, decl.kind, decl.args, preludeArr, trailerArr);
        }
        return entries;
    }

    private static SwiftTypeEntry[] resolveTypes(
        long[] typeOffsets,
        List<RawRecord> records,
        long[] capturedOffsets,
        Set<Long> wanted
    ) {
        SwiftTypeEntry[] entries = new SwiftTypeEntry[typeOffsets.length];
        for (int i = 0; i < typeOffsets.length; i++) {
            long off = typeOffsets[i];
            if (off <= 0) {
                continue;
            }
            int idx = findOffset(capturedOffsets, off);
            if (idx < 0) {
                continue;
            }
            RawRecord rec = records.get(idx);
            // Types have no preludes — the offset already points at the type
            // record itself. The only catch is variable-arity types whose
            // elements live in trailing records (TUPLE_TYPE_ELT,
            // FUNCTION_PARAM); pull those into the entry's trailers array.
            SwiftAuxRecord[] trailers = collectTypeTrailers(rec.kind, idx, records);
            entries[i] = new SwiftTypeEntry(i + 1, rec.kind, rec.args, trailers);
        }
        return entries;
    }

    private static SwiftAuxRecord[] collectTypeTrailers(
        int parentKind,
        int parentIdx,
        List<RawRecord> records
    ) {
        // Only complex types carry trailers. Skip the cheap case.
        int wanted0;
        int wanted1 = -1;
        switch (parentKind) {
            case SwiftDeclRecord.TUPLE_TYPE:
                wanted0 = SwiftDeclRecord.TUPLE_TYPE_ELT;
                break;
            case SwiftDeclRecord.FUNCTION_TYPE:
            case SwiftDeclRecord.GENERIC_FUNCTION_TYPE:
                wanted0 = SwiftDeclRecord.FUNCTION_PARAM;
                break;
            default:
                return new SwiftAuxRecord[0];
        }
        List<SwiftAuxRecord> out = new ArrayList<>();
        for (int j = parentIdx + 1; j < records.size(); j++) {
            RawRecord rec = records.get(j);
            if (rec.kind == wanted0 || (wanted1 != -1 && rec.kind == wanted1)) {
                out.add(new SwiftAuxRecord(rec.kind, rec.args));
                continue;
            }
            // Boundary — next wanted entity or any non-trailer record.
            break;
        }
        return out.toArray(new SwiftAuxRecord[0]);
    }

    private static int findOffset(long[] capturedOffsets, long off) {
        int idx = Arrays.binarySearch(capturedOffsets, off);
        if (idx < 0) {
            // Insertion point — the offset wasn't captured. Returning -1
            // lets the caller leave the slot null.
            return -1;
        }
        return idx;
    }

    private SwiftDeclTableExtractor() {
    }

    // ------------------------------------------------------------------

    /** Captured record from {@code DECLS_AND_TYPES_BLOCK}. */
    private static final class RawRecord {
        final long offset;
        final int kind;
        final long[] args;

        RawRecord(long offset, int kind, long[] args) {
            this.offset = offset;
            this.kind = kind;
            this.args = args;
        }
    }

    private static final class Walker {
        private final StreamingBitReader r;
        private final Set<Long> wanted;
        /** Bits already consumed before we started — added to {@link StreamingBitReader#bitOffset()} for file-relative offsets. */
        private final long baseBits;
        /** All records encountered inside {@code DECLS_AND_TYPES_BLOCK}, in walk order. */
        final List<RawRecord> records = new ArrayList<>();
        /**
         * True while we should capture every record we see. We stay armed
         * from the first wanted offset until end of the block — that way
         * any prelude→decl pair lands in {@link #records} regardless of
         * what comes between or whether the decl ever materializes.
         */
        private boolean armed = false;

        Walker(StreamingBitReader r, Set<Long> wanted, long baseBits) {
            this.r = r;
            this.wanted = wanted;
            this.baseBits = baseBits;
        }

        private long currentBitOffset() {
            return baseBits + r.bitOffset();
        }

        void walkRoot() throws IOException {
            // Root abbreviation width is 2 — same as upstream Parser default.
            walkBlock(2, -1);
        }

        private void walkBlock(int idSize, int blockId) throws IOException {
            List<AbbrevDef> abbrevs = new ArrayList<>();
            while (r.hasAtLeast(idSize)) {
                long recordStart = currentBitOffset();
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
                    // Descend only the chain we care about.
                    if (innerId == Block.MODULE.id() || innerId == Block.DECLS_AND_TYPES.id()) {
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
                    boolean capture = isCapturing(blockId, recordStart);
                    if (capture) {
                        long[] args = new long[(int) count];
                        for (int i = 0; i < count; i++) {
                            args[i] = r.readVBR(6);
                        }
                        records.add(new RawRecord(recordStart, (int) recordId, args));
                    }
                    else {
                        for (int i = 0; i < count; i++) {
                            r.readVBR(6);
                        }
                    }
                    continue;
                }
                // Abbreviated record (>= 4)
                int abbrevIdx = (int) opcode - 4;
                if (abbrevIdx < 0 || abbrevIdx >= abbrevs.size()) {
                    throw new IOException("Unknown abbreviation id " + opcode
                        + " at bit offset " + currentBitOffset());
                }
                AbbrevDef def = abbrevs.get(abbrevIdx);
                boolean capture = isCapturing(blockId, recordStart);
                long[] args = decodeAbbrev(def);
                if (capture) {
                    records.add(new RawRecord(recordStart, (int) def.literalId, args));
                }
            }
        }

        /**
         * Whether the next record at {@code recordStart} should be captured.
         * Once we see the first wanted offset inside DECLS_AND_TYPES we stay
         * armed for the rest of the block — capturing everything keeps the
         * captured-records list a simple linear stream that the phase-2
         * pass can scan to skip preludes.
         */
        private boolean isCapturing(int blockId, long recordStart) {
            if (blockId != Block.DECLS_AND_TYPES.id()) {
                return false;
            }
            if (!armed && wanted.contains(recordStart)) {
                armed = true;
            }
            return armed;
        }

        private AbbrevDef parseAbbrev() throws IOException {
            int count = (int) r.readVBR(5);
            long literalId = -1;
            List<AbbrevOp> dataOps = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                boolean isLiteral = r.readBits(1) == 1;
                if (isLiteral) {
                    long value = r.readVBR(8);
                    if (i == 0) {
                        literalId = value;
                    }
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
                        boolean elLit = r.readBits(1) == 1;
                        if (elLit) {
                            r.readVBR(8);
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
                        i++;
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
            List<long[]> chunks = new ArrayList<>(def.ops.size());
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
                        out[i] = vals[0];
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
}
