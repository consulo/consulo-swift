/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import uk.ac.man.cs.llvm.bc.ParserListener;
import uk.ac.man.cs.llvm.bc.blocks.Block;

import java.nio.charset.StandardCharsets;

/**
 * Walks a {@code .swiftmodule} and pulls just enough metadata out of the
 * {@code CONTROL_BLOCK} to populate a {@link SwiftModuleInfo}: module name,
 * target triple, format version, SDK info, compiler revision.
 * <p>
 * Uses the stacked-listener convention from {@code uk.ac.man.cs.llvm.bc} —
 * {@link ParserListener#enter(Block)} returns a new listener for the inner
 * block, {@link ParserListener#exit()} cleans up. Blocks we don't care
 * about return a no-op listener that drops records.
 */
public final class SwiftModuleInfoExtractor {

    public static SwiftModuleInfo extract(byte[] bytes) {
        SwiftModuleInfo info = new SwiftModuleInfo();
        SwiftBitstreamWalker.walk(bytes, new RootListener(info));
        // Phase 2: now that INDEX_BLOCK has populated declOffsets, do a
        // second pass over DECLS_AND_TYPES_BLOCK to capture decl records
        // by bit offset and resolve top-level decl names. The upstream
        // parser doesn't expose per-record bit offsets to listeners, so
        // we use SwiftDeclTableExtractor's bespoke walker here.
        long[] declOffsets = info.getDeclOffsets();
        long[] typeOffsets = info.getTypeOffsets();
        if (declOffsets.length > 0 || typeOffsets.length > 0) {
            SwiftDeclTableExtractor.Result r =
                SwiftDeclTableExtractor.extractAll(bytes, declOffsets, typeOffsets);
            info.setDeclEntries(r.declEntries());
            info.setTypeEntries(r.typeEntries());
        }
        return info;
    }

    private SwiftModuleInfoExtractor() {
    }

    // --- Listener stack -------------------------------------------------

    private static final class RootListener implements ParserListener {
        final SwiftModuleInfo info;

        RootListener(SwiftModuleInfo info) {
            this.info = info;
        }

        @Override
        public ParserListener enter(Block block) {
            if (block == Block.MODULE) {
                return new ModuleListener(info);
            }
            return DEFAULT;
        }

        @Override
        public void record(long id, long[] args) {
            // Records at root level are not expected.
        }
    }

    private static final class ModuleListener implements ParserListener {
        final SwiftModuleInfo info;

        ModuleListener(SwiftModuleInfo info) {
            this.info = info;
        }

        @Override
        public ParserListener enter(Block block) {
            if (block == Block.CONTROL) {
                return new ControlListener(info);
            }
            if (block == Block.IDENTIFIER_DATA) {
                return new IdentifierListener(info);
            }
            if (block == Block.INDEX) {
                return new IndexListener(info);
            }
            // OPTIONS, INPUT, DECLS_AND_TYPES — not yet inspected.
            return DEFAULT;
        }

        @Override
        public void record(long id, long[] args) {
        }
    }

    /**
     * Captures the three offset tables we'll need to look anything up:
     * IDENTIFIER_OFFSETS (identifierID → byte offset in identifier blob),
     * DECL_OFFSETS (declID → bit offset in DECLS_AND_TYPES_BLOCK), and
     * TYPE_OFFSETS (typeID → bit offset). Plus ORDERED_TOP_LEVEL_DECLS
     * which gives us the public-API decl IDs in source order.
     * <p>
     * All four records share {@code OffsetsLayout} /
     * {@code OrderedDeclsLayout}: a record kind followed by a single
     * {@code BCArray<BCFixed<31>>}. After {@code UserRecordBuilder} accumulates
     * the array operand, our {@code record(id, args)} receives {@code id} =
     * record kind and {@code args} = the offset/decl-ID array directly.
     */
    private static final class IndexListener implements ParserListener {
        final SwiftModuleInfo info;

        IndexListener(SwiftModuleInfo info) {
            this.info = info;
        }

        @Override
        public ParserListener enter(Block block) {
            // Sub-blocks (DECL_MEMBER_TABLES) — not yet inspected.
            return DEFAULT;
        }

        @Override
        public void record(long id, long[] args) {
            switch ((int) id) {
                case SwiftIndexRecord.TYPE_OFFSETS:
                    info.setTypeOffsets(args);
                    break;
                case SwiftIndexRecord.DECL_OFFSETS:
                    info.setDeclOffsets(args);
                    break;
                case SwiftIndexRecord.IDENTIFIER_OFFSETS:
                    info.setIdentifierOffsets(args);
                    break;
                case SwiftIndexRecord.ORDERED_TOP_LEVEL_DECLS:
                    info.setOrderedTopLevelDeclIds(args);
                    break;
                default:
                    // Many index records use DeclListLayout (hash-table blobs) —
                    // skip until milestone 4 needs them.
            }
        }
    }

    private static final class IdentifierListener implements ParserListener {
        final SwiftModuleInfo info;

        IdentifierListener(SwiftModuleInfo info) {
            this.info = info;
        }

        @Override
        public ParserListener enter(Block block) {
            return DEFAULT;
        }

        @Override
        public void record(long id, long[] args) {
            if (id == SwiftIdentifierBlockRecord.IDENTIFIER_DATA) {
                byte[] blob = new byte[args.length];
                for (int i = 0; i < args.length; i++) {
                    blob[i] = (byte) (args[i] & 0xFF);
                }
                info.setIdentifiers(new SwiftIdentifierTable(blob));
            }
        }
    }

    private static final class ControlListener implements ParserListener {
        final SwiftModuleInfo info;

        ControlListener(SwiftModuleInfo info) {
            this.info = info;
        }

        @Override
        public ParserListener enter(Block block) {
            // OPTIONS_BLOCK can nest inside CONTROL_BLOCK. Skip for now.
            return DEFAULT;
        }

        @Override
        public void record(long id, long[] args) {
            switch ((int) id) {
                case SwiftControlRecord.METADATA:
                    // Layout: BCFixed<16> majorVersion, BCFixed<16> minorVersion,
                    // BCVBR<8> shortVersionLen, BCVBR<8> shortCompatLen,
                    // BCVBR<17> user-major, BCVBR<17> user-minor, BCVBR<17> user-sub-minor,
                    // BCBlob (concatenated version strings)
                    if (args.length >= 2) {
                        info.setFormatMajor((int) args[0]);
                        info.setFormatMinor((int) args[1]);
                    }
                    break;
                case SwiftControlRecord.MODULE_NAME:
                    info.setModuleName(blobToString(args));
                    break;
                case SwiftControlRecord.TARGET:
                    info.setTarget(blobToString(args));
                    break;
                case SwiftControlRecord.SDK_NAME:
                    info.setSdkName(blobToString(args));
                    break;
                case SwiftControlRecord.REVISION:
                    info.setRevision(blobToString(args));
                    break;
                case SwiftControlRecord.CHANNEL:
                    info.setChannel(blobToString(args));
                    break;
                case SwiftControlRecord.SDK_VERSION:
                    info.setSdkVersion(blobToString(args));
                    break;
                default:
                    // ALLOWABLE_CLIENT_NAME and any future records — ignore.
            }
        }
    }

    // --- Helpers ---------------------------------------------------------

    /**
     * Convert a {@code long[]} of byte values (from a {@link
     * uk.ac.man.cs.llvm.bc.records.UserRecordBinaryOperand} blob) to a
     * UTF-8 string, stripping any trailing NUL bytes the layout may pad
     * the blob with.
     */
    private static String blobToString(long[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            b[i] = (byte) (bytes[i] & 0xFF);
        }
        int len = b.length;
        while (len > 0 && b[len - 1] == 0) {
            len--;
        }
        return new String(b, 0, len, StandardCharsets.UTF_8);
    }
}
