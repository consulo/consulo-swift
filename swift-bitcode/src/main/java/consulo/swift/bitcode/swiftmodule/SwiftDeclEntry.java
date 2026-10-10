/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * One decoded record from {@code DECLS_AND_TYPES_BLOCK}, captured during
 * the post-control walk of a {@code .swiftmodule}. We hold on to:
 *
 * <ul>
 *   <li>{@link #declId()} — the 1-based id this record was stored at,
 *       matched against {@code INDEX_BLOCK.DECL_OFFSETS}.</li>
 *   <li>{@link #recordKind()} — one of the {@link SwiftDeclRecord} {@code _DECL}
 *       constants, telling you the layout of {@link #args()}.</li>
 *   <li>{@link #args()} — raw operand array from the bitstream, exactly as
 *       delivered by the underlying parser (no decoding applied — the
 *       per-kind helpers below pluck specific fields).</li>
 * </ul>
 *
 * The args layouts come from {@code lib/Serialization/ModuleFormat.h}.
 * Each {@code BCRecordLayout<...>} listed there maps positionally onto the
 * elements of {@link #args()} (except the layout's leading record-kind
 * literal — that is the {@code recordKind} field here, not part of
 * {@code args}).
 */
public final class SwiftDeclEntry {

    /** {@link #baseNameIdentifierId()} value when the record has no name. */
    public static final int NO_NAME = 0;

    private final int declId;
    private final int recordKind;
    private final long[] args;
    private final SwiftAuxRecord[] preludes;
    private final SwiftAuxRecord[] trailers;

    private static final SwiftAuxRecord[] NO_AUX = new SwiftAuxRecord[0];

    public SwiftDeclEntry(int declId, int recordKind, long[] args) {
        this(declId, recordKind, args, NO_AUX, NO_AUX);
    }

    public SwiftDeclEntry(int declId, int recordKind, long[] args, SwiftAuxRecord[] trailers) {
        this(declId, recordKind, args, NO_AUX, trailers);
    }

    public SwiftDeclEntry(int declId, int recordKind, long[] args,
                          SwiftAuxRecord[] preludes, SwiftAuxRecord[] trailers) {
        this.declId = declId;
        this.recordKind = recordKind;
        this.args = args == null ? new long[0] : args;
        this.preludes = preludes == null ? NO_AUX : preludes;
        this.trailers = trailers == null ? NO_AUX : trailers;
    }

    public int declId() {
        return declId;
    }

    public int recordKind() {
        return recordKind;
    }

    public long[] args() {
        return args;
    }

    /**
     * Auxiliary records captured immediately after this decl's main record
     * and before the next decl's offset. Typical contents for nominal
     * decls: {@link SwiftDeclRecord#GENERIC_PARAM_LIST},
     * {@link SwiftDeclRecord#INHERITED_PROTOCOLS},
     * {@link SwiftDeclRecord#MEMBERS}, plus conformance records. For
     * function-shaped decls: {@link SwiftDeclRecord#PARAMETERLIST}.
     */
    public SwiftAuxRecord[] trailers() {
        return trailers;
    }

    /**
     * Auxiliary records captured immediately BEFORE this decl's main
     * record (in [{@code DECL_OFFSETS[id-1]}, {@code declRecordOffset})).
     * Typical contents: Swift's per-decl prelude — {@code DESERIALIZATION_SAFETY},
     * {@code ERROR_FLAG}, {@code ABI_ONLY_COUNTERPART}, and most
     * importantly the {@code *_DECL_ATTR} records (kinds 180+CODE)
     * carrying each {@code @attribute} the source wrote on the decl.
     *
     * <p>The decompiler reads these to emit {@code @inlinable},
     * {@code @usableFromInline}, {@code @frozen}, etc. on the synthesized
     * declarations.
     */
    public SwiftAuxRecord[] preludes() {
        return preludes;
    }

    /** Find the first trailing record matching {@code kind}, or {@code null}. */
    public SwiftAuxRecord findTrailer(int kind) {
        for (SwiftAuxRecord t : trailers) {
            if (t.kind() == kind) {
                return t;
            }
        }
        return null;
    }

    /**
     * Return the {@code DeclContextID} of this decl's parent scope, or
     * {@code 0} when the decl is at module top-level / external.
     *
     * <p>The DeclContext field's position in {@link #args()} depends on
     * the record's BCRecordLayout in {@code lib/Serialization/ModuleFormat.h}.
     * Most decls put it at args[1] (right after the leading IdentifierID
     * name); function-shaped decls without a leading name put it at
     * args[0]; PARAM_DECL puts it at args[2] (after argName + paramName).
     *
     * <p>The returned ID is in the same space as {@code DeclID} —
     * resolve via {@link SwiftModuleInfo#lookupDecl(int)} to find out
     * whether the parent is another decl in this module (then we have a
     * nested type) or a synthetic context record like
     * {@code PATTERN_BINDING_INITIALIZER_CONTEXT} (then this decl is
     * effectively top-level).
     */
    public int parentContextId() {
        int idx = parentContextArgIndex();
        if (idx < 0 || idx >= args.length) {
            return 0;
        }
        return (int) args[idx];
    }

    private int parentContextArgIndex() {
        switch (recordKind) {
            // Decls with a leading IdentifierID name: context at args[1].
            case SwiftDeclRecord.TYPE_ALIAS_DECL:
            case SwiftDeclRecord.GENERIC_TYPE_PARAM_DECL:
            case SwiftDeclRecord.ASSOCIATED_TYPE_DECL:
            case SwiftDeclRecord.STRUCT_DECL:
            case SwiftDeclRecord.CLASS_DECL:
            case SwiftDeclRecord.ENUM_DECL:
            case SwiftDeclRecord.PROTOCOL_DECL:
            case SwiftDeclRecord.VAR_DECL:
            case SwiftDeclRecord.INFIX_OPERATOR_DECL:
            case SwiftDeclRecord.PREFIX_OPERATOR_DECL:
            case SwiftDeclRecord.POSTFIX_OPERATOR_DECL:
            case SwiftDeclRecord.PRECEDENCE_GROUP_DECL:
                return 1;
            // Decls without a leading name: context at args[0].
            case SwiftDeclRecord.CONSTRUCTOR_DECL:
            case SwiftDeclRecord.DESTRUCTOR_DECL:
            case SwiftDeclRecord.FUNC_DECL:
            case SwiftDeclRecord.ACCESSOR_DECL:
            case SwiftDeclRecord.OPAQUE_TYPE_DECL:
            case SwiftDeclRecord.EXTENSION_DECL:
            case SwiftDeclRecord.SUBSCRIPT_DECL:
            case SwiftDeclRecord.PATTERN_BINDING_DECL:
            case SwiftDeclRecord.MACRO_DECL:
            case SwiftDeclRecord.ENUM_ELEMENT_DECL:
                return 0;
            // PARAM_DECL has (argName, paramName) before the context.
            case SwiftDeclRecord.PARAM_DECL:
                return 2;
            default:
                return -1;
        }
    }

    /**
     * True when this entry stands in for a decl that physically lives in
     * <em>another</em> module — Swift records its identity here as an XREF
     * (kind {@link SwiftDeclRecord#XREF}) so type uses can name it without
     * inlining its body. The XREF payload is a chain of path pieces
     * (module name → nested type → final value) recorded as subsequent
     * {@code XREF_*_PATH_PIECE} records; we don't expose those yet — clients
     * that need the cross-module name should treat this as an opaque
     * reference for now.
     */
    public boolean isExternal() {
        return recordKind == SwiftDeclRecord.XREF;
    }

    /**
     * The Swift identifier ID for the decl's base name, or {@link #NO_NAME}
     * if this kind of decl doesn't carry a stand-alone identifier (e.g.
     * {@code init}/{@code deinit}/extensions/accessors).
     * <p>
     * Resolve the ID to a string by calling
     * {@link SwiftModuleInfo#lookupIdentifier(int)} on the owning info.
     * <p>
     * Layouts come from {@code lib/Serialization/ModuleFormat.h}. For
     * function-shaped decls ({@code FuncDecl}, {@code SubscriptDecl},
     * {@code MacroDecl}, etc.) the name lives at the head of a trailing
     * {@code BCArray<IdentifierIDField>} — the index of that array's first
     * element is the count of fixed scalar fields that precede it.
     */
    public int baseNameIdentifierId() {
        switch (recordKind) {
            // Leading IdentifierID — the common nominal/typealias shape.
            case SwiftDeclRecord.TYPE_ALIAS_DECL:
            case SwiftDeclRecord.GENERIC_TYPE_PARAM_DECL:
            case SwiftDeclRecord.ASSOCIATED_TYPE_DECL:
            case SwiftDeclRecord.STRUCT_DECL:
            case SwiftDeclRecord.VAR_DECL:
            case SwiftDeclRecord.CLASS_DECL:
            case SwiftDeclRecord.ENUM_DECL:
            case SwiftDeclRecord.PROTOCOL_DECL:
            case SwiftDeclRecord.PREFIX_OPERATOR_DECL:
            case SwiftDeclRecord.POSTFIX_OPERATOR_DECL:
            case SwiftDeclRecord.INFIX_OPERATOR_DECL:
            case SwiftDeclRecord.PRECEDENCE_GROUP_DECL:
                return args.length > 0 ? (int) args[0] : NO_NAME;

            // PARAM_DECL = (argName, paramName, ...) — return the public arg label.
            case SwiftDeclRecord.PARAM_DECL:
                return args.length > 0 ? (int) args[0] : NO_NAME;

            // FUNC_DECL — see FuncLayout in ModuleFormat.h. After 23 fixed scalar
            // fields, args[23..] is BCArray<IdentifierIDField> { baseName, argLabel, ..., typeDeps }.
            case SwiftDeclRecord.FUNC_DECL:
                return args.length > 23 ? (int) args[23] : NO_NAME;

            // ACCESSOR_DECL — same FuncDecl-derived shape but with extras (storage decl,
            // accessor kind, transparent, distributed thunk). 20 fixed scalar fields
            // precede the BCArray. Accessors usually have no user-visible name (NO_NAME).
            case SwiftDeclRecord.ACCESSOR_DECL:
                return NO_NAME;

            // ENUM_ELEMENT_DECL — see EnumElementLayout. After 7 fixed scalar fields
            // args[7..] is BCArray<IdentifierIDField> name components.
            case SwiftDeclRecord.ENUM_ELEMENT_DECL:
                return args.length > 7 ? (int) args[7] : NO_NAME;

            // SUBSCRIPT_DECL — see SubscriptLayout. After 18 fixed scalar fields,
            // args[18..] is BCArray<IdentifierIDField> name components — but Swift
            // names every subscript "subscript"; we emit NO_NAME here and surface
            // the literal in the printer.
            case SwiftDeclRecord.SUBSCRIPT_DECL:
                return NO_NAME;

            // MACRO_DECL — see MacroLayout. After 11 fixed scalar fields,
            // args[11..] is BCArray<IdentifierIDField> name components.
            case SwiftDeclRecord.MACRO_DECL:
                return args.length > 11 ? (int) args[11] : NO_NAME;

            // Decls without a stand-alone identifier in their layout.
            case SwiftDeclRecord.CONSTRUCTOR_DECL:
            case SwiftDeclRecord.DESTRUCTOR_DECL:
            case SwiftDeclRecord.EXTENSION_DECL:
            case SwiftDeclRecord.OPAQUE_TYPE_DECL:
            case SwiftDeclRecord.PATTERN_BINDING_DECL:
                return NO_NAME;

            default:
                return NO_NAME;
        }
    }

    @Override
    public String toString() {
        return "SwiftDeclEntry{"
            + "declId=" + declId
            + ", kind=" + SwiftDeclRecord.name(recordKind)
            + ", nameId=" + baseNameIdentifierId()
            + ", args=" + args.length
            + '}';
    }
}
