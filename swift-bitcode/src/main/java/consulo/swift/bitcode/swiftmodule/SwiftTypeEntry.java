/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * One decoded type record from {@code DECLS_AND_TYPES_BLOCK}, captured by
 * the same phase-2 walker that produces {@link SwiftDeclEntry}. Types are
 * keyed by 1-based {@code typeID} and the offset for each ID lives in
 * {@link SwiftModuleInfo#getTypeOffsets() typeOffsets}.
 *
 * <p>Unlike decl records, types have no prelude (no DESERIALIZATION_SAFETY,
 * ERROR_FLAG or DECL_ATTR before them), so the offset points straight at
 * the type record itself. Variable-arity types ({@link SwiftDeclRecord#TUPLE_TYPE},
 * {@link SwiftDeclRecord#FUNCTION_TYPE}, {@link SwiftDeclRecord#GENERIC_FUNCTION_TYPE})
 * carry their elements as follow-up records of kind {@link SwiftDeclRecord#TUPLE_TYPE_ELT}
 * and {@link SwiftDeclRecord#FUNCTION_PARAM}; we capture those in
 * {@link #trailers()} ordered as they appear in the bitstream.
 *
 * <p>{@link #args()} is the raw operand array exactly as the parser produced
 * it, matching the corresponding {@code TYPE_LAYOUT(...)} in
 * {@code lib/Serialization/ModuleFormat.h}.
 */
public final class SwiftTypeEntry {

    private final int typeId;
    private final int recordKind;
    private final long[] args;
    private final SwiftAuxRecord[] trailers;

    private static final SwiftAuxRecord[] NO_TRAILERS = new SwiftAuxRecord[0];

    public SwiftTypeEntry(int typeId, int recordKind, long[] args) {
        this(typeId, recordKind, args, NO_TRAILERS);
    }

    public SwiftTypeEntry(int typeId, int recordKind, long[] args, SwiftAuxRecord[] trailers) {
        this.typeId = typeId;
        this.recordKind = recordKind;
        this.args = args == null ? new long[0] : args;
        this.trailers = trailers == null ? NO_TRAILERS : trailers;
    }

    public int typeId() {
        return typeId;
    }

    public int recordKind() {
        return recordKind;
    }

    public long[] args() {
        return args;
    }

    /**
     * Trailing TUPLE_TYPE_ELT / FUNCTION_PARAM records that belong to this
     * type, in serialization order. Empty for the (vast) majority of types.
     */
    public SwiftAuxRecord[] trailers() {
        return trailers;
    }

    @Override
    public String toString() {
        return "SwiftTypeEntry{typeId=" + typeId
            + ", kind=" + recordKind
            + ", args=" + args.length
            + ", trailers=" + trailers.length
            + '}';
    }
}
