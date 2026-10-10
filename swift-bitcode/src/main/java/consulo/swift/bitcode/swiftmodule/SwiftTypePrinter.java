/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Renders Swift type IDs as Swift source text. The printer is fed a
 * fully-populated {@link SwiftModuleInfo} — it does no I/O of its own,
 * just walks the {@link SwiftTypeEntry} graph and consults
 * {@link SwiftModuleInfo#lookupDeclName(int)} / {@link SwiftModuleInfo#lookupIdentifier(int)}
 * to name nominal types.
 *
 * <p>Supported kinds (covers the common Swift surface):
 * <ul>
 *   <li>{@link SwiftDeclRecord#NOMINAL_TYPE} → {@code DeclName}</li>
 *   <li>{@link SwiftDeclRecord#BOUND_GENERIC_TYPE} → {@code Decl<A, B, ...>}</li>
 *   <li>{@link SwiftDeclRecord#OPTIONAL_TYPE} → {@code T?}</li>
 *   <li>{@link SwiftDeclRecord#ARRAY_SLICE_TYPE} → {@code [T]}</li>
 *   <li>{@link SwiftDeclRecord#DICTIONARY_TYPE} → {@code [K: V]}</li>
 *   <li>{@link SwiftDeclRecord#METATYPE_TYPE} → {@code T.Type}</li>
 *   <li>{@link SwiftDeclRecord#EXISTENTIAL_METATYPE_TYPE} → {@code (any T).Type}</li>
 *   <li>{@link SwiftDeclRecord#EXISTENTIAL_TYPE} → {@code any T}</li>
 *   <li>{@link SwiftDeclRecord#PROTOCOL_COMPOSITION_TYPE} → {@code A & B & C}</li>
 *   <li>{@link SwiftDeclRecord#PARAMETERIZED_PROTOCOL_TYPE} → {@code P<A, B>}</li>
 *   <li>{@link SwiftDeclRecord#NAME_ALIAS_TYPE} → typealias decl name</li>
 *   <li>{@link SwiftDeclRecord#BUILTIN_ALIAS_TYPE} → builtin typealias decl name</li>
 *   <li>{@link SwiftDeclRecord#REFERENCE_STORAGE_TYPE} → {@code weak/unowned T}</li>
 *   <li>{@link SwiftDeclRecord#DYNAMIC_SELF_TYPE} → {@code Self}</li>
 *   <li>{@link SwiftDeclRecord#GENERIC_TYPE_PARAM_TYPE} → {@code τ_d_i} fallback</li>
 *   <li>{@link SwiftDeclRecord#TUPLE_TYPE} → {@code (T1, T2, ...)}</li>
 *   <li>{@link SwiftDeclRecord#FUNCTION_TYPE} → {@code (params) -> result}</li>
 *   <li>{@link SwiftDeclRecord#ARRAY_SLICE_TYPE}, {@link SwiftDeclRecord#VARIADIC_SEQUENCE_TYPE},
 *       {@link SwiftDeclRecord#ERROR_TYPE} → graceful placeholders</li>
 * </ul>
 *
 * <p>Anything else falls back to a {@code <typeID:kind>} placeholder so a
 * decompiled module remains parseable rather than crashing on an exotic
 * type.
 *
 * <p>Cycle protection: type graphs are usually acyclic, but generic
 * substitutions or recovery types could in principle form cycles. The
 * printer tracks the in-progress {@code SwiftTypeEntry} set and emits
 * {@code <recursive>} instead of looping forever.
 */
public final class SwiftTypePrinter {

    /**
     * Stand-in returned for typeID 0 (Swift's "no type" sentinel).
     * Emitted as a Swift comment so the output stays valid Swift even when
     * the original signature referenced a type slot we don't track yet.
     */
    public static final String NONE = "Any /* no-type */";

    /**
     * Wrap a placeholder note as a Swift comment glued to {@code Any}.
     * The point is to keep the surrounding declaration parseable by
     * {@code swiftc -parse}: bare angle-bracketed sentinels are not valid
     * Swift, but {@code Any} followed by a Swift {@code /} *...* {@code /}
     * comment is. Callers should pass a short reason — it becomes part of
     * the synthesized file so it shows up in code review.
     */
    private static String unknown(String note) {
        return "Any /* " + note + " */";
    }

    /**
     * Recognises strings produced by {@link #unknown(String)} or by the
     * {@code lookupDeclName} fallback paths. Use this when you need to
     * know whether a child render result represents a real Swift type
     * name or a placeholder — placeholders can't be decorated (with
     * generics, member access, etc.) without producing invalid Swift.
     */
    private static boolean isPlaceholder(String text) {
        return text != null
            && (text.startsWith("Any /*") || text.startsWith("_Unresolved_"));
    }


    private final SwiftModuleInfo info;

    public SwiftTypePrinter(SwiftModuleInfo info) {
        if (info == null) {
            throw new NullPointerException("info");
        }
        this.info = info;
    }

    /** Render the type with the given 1-based ID. Stable, side-effect free. */
    public String print(int typeId) {
        return new Context().print(typeId);
    }

    /** Render the underlying type for a decl that references one (typealias, var, etc.). */
    public String printAt(SwiftTypeEntry entry) {
        if (entry == null) {
            return NONE;
        }
        return new Context().printEntry(entry);
    }

    // ------------------------------------------------------------------

    private final class Context {
        /** Entries currently mid-print. Cycle guard. */
        private final Map<SwiftTypeEntry, Boolean> visiting = new IdentityHashMap<>();

        String print(int typeId) {
            if (typeId == 0) {
                return NONE;
            }
            SwiftTypeEntry entry = info.lookupType(typeId);
            if (entry == null) {
                // typeID was declared in INDEX_BLOCK but we never captured a
                // record at that offset (or it points outside DECLS_AND_TYPES).
                // Emit a parseable placeholder so the surrounding decl is valid.
                return unknown("typeID=" + typeId + " not resolved by extractor");
            }
            return printEntry(entry);
        }

        String printEntry(SwiftTypeEntry entry) {
            if (visiting.containsKey(entry)) {
                return "<recursive>";
            }
            visiting.put(entry, Boolean.TRUE);
            try {
                return render(entry);
            }
            finally {
                visiting.remove(entry);
            }
        }

        private String render(SwiftTypeEntry entry) {
            long[] args = entry.args();
            switch (entry.recordKind()) {
                case SwiftDeclRecord.NOMINAL_TYPE: {
                    // args = (DeclID, parent TypeID)
                    String name = lookupDeclName((int) longAt(args, 0));
                    long parent = longAt(args, 1);
                    if (parent > 0) {
                        String parentText = print((int) parent);
                        // Don't dot-prefix a real type with a placeholder
                        // — {@code Any /* ... */.Foo} is invalid Swift.
                        // Drop the parent in that case; the resulting name
                        // is technically less precise but still parses.
                        if (isPlaceholder(parentText) || isPlaceholder(name)) {
                            return isPlaceholder(name) ? name : parentText;
                        }
                        return parentText + "." + name;
                    }
                    return name;
                }
                case SwiftDeclRecord.BOUND_GENERIC_TYPE: {
                    // args = (DeclID, parent TypeID, [generic args...])
                    int baseDeclId = (int) longAt(args, 0);
                    String head = lookupDeclName(baseDeclId);
                    // If the base couldn't be resolved to a real Swift type
                    // name (because the XREF chain led to an operator / init
                    // / etc. position), don't try to apply generics to a
                    // placeholder — emit a single unified placeholder so
                    // the surrounding declaration stays parseable.
                    if (isPlaceholder(head)) {
                        return unknown("bound generic over unresolved base (declID="
                            + baseDeclId + ")");
                    }
                    long parent = longAt(args, 1);
                    if (parent > 0) {
                        String parentText = print((int) parent);
                        if (isPlaceholder(parentText)) {
                            return unknown("bound generic with unresolved parent type");
                        }
                        head = parentText + "." + head;
                    }
                    StringBuilder sb = new StringBuilder(head);
                    sb.append('<');
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) {
                            sb.append(", ");
                        }
                        sb.append(print((int) args[i]));
                    }
                    sb.append('>');
                    return sb.toString();
                }
                case SwiftDeclRecord.NAME_ALIAS_TYPE: {
                    // args = (DeclID, underlying, substituted, parent, [generic args])
                    String name = lookupDeclName((int) longAt(args, 0));
                    if (args.length > 5) {
                        StringBuilder sb = new StringBuilder(name);
                        sb.append('<');
                        for (int i = 4; i < args.length; i++) {
                            if (i > 4) {
                                sb.append(", ");
                            }
                            sb.append(print((int) args[i]));
                        }
                        sb.append('>');
                        return sb.toString();
                    }
                    return name;
                }
                case SwiftDeclRecord.BUILTIN_ALIAS_TYPE: {
                    // args = (DeclID, canonical TypeID)
                    return lookupDeclName((int) longAt(args, 0));
                }
                case SwiftDeclRecord.OPTIONAL_TYPE: {
                    // Bare {@code Foo<T>?} parses natively now that {@code >}
                    // is a primitive token at the lexer (see the comment in
                    // {@code _SwiftLexer.flex} where {@code >} was removed
                    // from {@code OP_CHAR}).
                    //
                    // Two cases where we DON'T append {@code ?}:
                    //   * Inner is a placeholder ({@code Any /* ... */}).
                    //     {@code Any /* */?} doesn't append cleanly because
                    //     the comment + {@code ?} can combine into a
                    //     compound operator token; just return the
                    //     placeholder, which is already a usable type.
                    //   * Inner already ends with {@code ?} or {@code !}
                    //     (nested Optionals: {@code Optional<Optional<X>>}).
                    //     {@code X??} is valid only when both {@code ?}s
                    //     are atomic; with a placeholder in between, the
                    //     lexer combines {@code ??} into a binary
                    //     null-coalesce operator. Returning {@code Any}
                    //     once is safer than {@code Any??}.
                    String inner = print((int) longAt(args, 0));
                    if (isPlaceholder(inner)) {
                        return inner;
                    }
                    return inner + "?";
                }
                case SwiftDeclRecord.ARRAY_SLICE_TYPE:
                    return "[" + print((int) longAt(args, 0)) + "]";
                case SwiftDeclRecord.VARIADIC_SEQUENCE_TYPE:
                    return print((int) longAt(args, 0)) + "...";
                case SwiftDeclRecord.DICTIONARY_TYPE:
                    return "[" + print((int) longAt(args, 0)) + " : " + print((int) longAt(args, 1)) + "]";
                case SwiftDeclRecord.INLINE_ARRAY_TYPE:
                    // (count type, element type) — count is a value-type expression we don't render yet
                    return "[" + print((int) longAt(args, 0)) + " of " + print((int) longAt(args, 1)) + "]";
                case SwiftDeclRecord.METATYPE_TYPE:
                    return print((int) longAt(args, 0)) + ".Type";
                case SwiftDeclRecord.EXISTENTIAL_METATYPE_TYPE: {
                    // Same {@code any X} dance as EXISTENTIAL_TYPE: if X
                    // resolved to a placeholder or bare {@code Any}, the
                    // {@code any} prefix is invalid, so emit
                    // {@code X.Type} (or the placeholder's metatype).
                    String inner = print((int) longAt(args, 0));
                    if (isPlaceholder(inner) || "Any".equals(inner)) {
                        return inner + ".Type";
                    }
                    return "(any " + inner + ").Type";
                }
                case SwiftDeclRecord.EXISTENTIAL_TYPE: {
                    // {@code any T}. Two cases where we DON'T prepend
                    // {@code any}:
                    //   * inner resolved to an {@code Any /* ... */}
                    //     placeholder — {@code any Any /* ... */} is
                    //     invalid Swift.
                    //   * inner resolved to bare {@code "Any"} (an empty
                    //     protocol composition) — {@code any Any} is
                    //     ALSO rejected by swiftc; just emit {@code Any}.
                    String inner = print((int) longAt(args, 0));
                    if (isPlaceholder(inner) || "Any".equals(inner)) {
                        return inner;
                    }
                    return "any " + inner;
                }
                case SwiftDeclRecord.PROTOCOL_COMPOSITION_TYPE: {
                    // args = (hasAnyObject, hasNonCopyable, hasNonEscapable, [protocol typeIDs])
                    boolean hasAnyObject = longAt(args, 0) != 0;
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (int i = 3; i < args.length; i++) {
                        if (!first) {
                            sb.append(" & ");
                        }
                        sb.append(print((int) args[i]));
                        first = false;
                    }
                    if (hasAnyObject) {
                        if (!first) {
                            sb.append(" & ");
                        }
                        sb.append("AnyObject");
                    }
                    if (first) {
                        return "Any";
                    }
                    return sb.toString();
                }
                case SwiftDeclRecord.PARAMETERIZED_PROTOCOL_TYPE: {
                    // args = (base TypeID, [argument TypeIDs])
                    String base = print((int) longAt(args, 0));
                    StringBuilder sb = new StringBuilder(base);
                    sb.append('<');
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) {
                            sb.append(", ");
                        }
                        sb.append(print((int) args[i]));
                    }
                    sb.append('>');
                    return sb.toString();
                }
                case SwiftDeclRecord.REFERENCE_STORAGE_TYPE: {
                    // args = (ownership kind, implementation TypeID)
                    long kind = longAt(args, 0);
                    String inner = print((int) longAt(args, 1));
                    return ownershipPrefix(kind) + inner;
                }
                case SwiftDeclRecord.DYNAMIC_SELF_TYPE:
                    return "Self";
                case SwiftDeclRecord.GENERIC_TYPE_PARAM_TYPE:
                    // Without a generic environment we can't recover the user-visible
                    // letter ("T", "U", ...). Emit `Any /* τ_d_i */` so the
                    // surrounding decl stays parseable; later milestones will
                    // wire up the generic environment to recover real names.
                    if (args.length >= 5) {
                        long depth = longAt(args, 2);
                        long index = longAt(args, 4);
                        return unknown("generic param τ_" + depth + "_" + index
                            + " — generic environment not resolved");
                    }
                    return unknown("generic param — generic environment not resolved");
                case SwiftDeclRecord.DEPENDENT_MEMBER_TYPE: {
                    // args = (base TypeID, associated DeclID)
                    // Renders as {@code <base>.<assoc>} — but only when
                    // both sides are real identifiers. If the base type
                    // resolved to a {@code Any /* ... */} placeholder
                    // (most commonly an unresolved generic parameter),
                    // appending {@code .Element} produces invalid Swift
                    // like {@code Any /* gen-param */.Element}. Drop the
                    // base in that case; the resulting type is less
                    // precise but stays parseable.
                    String base = print((int) longAt(args, 0));
                    String member = lookupDeclName((int) longAt(args, 1));
                    if (isPlaceholder(base) || isPlaceholder(member)) {
                        return isPlaceholder(member) ? member : base;
                    }
                    return base + "." + member;
                }
                case SwiftDeclRecord.TUPLE_TYPE:
                    return renderTuple(entry);
                case SwiftDeclRecord.FUNCTION_TYPE:
                case SwiftDeclRecord.GENERIC_FUNCTION_TYPE:
                    return renderFunction(entry);
                case SwiftDeclRecord.SIL_FUNCTION_TYPE:
                    // SIL is the lowered representation Swift uses internally
                    // for code-generation; source-level signatures should
                    // never reference it directly, but it does occasionally
                    // leak into serialized module files. Render as an opaque
                    // closure type so the surrounding signature still parses.
                    return "(() -> Any)";
                case SwiftDeclRecord.SIL_BLOCK_STORAGE_TYPE:
                    // args = (TypeID capture). Wrap the captured type in a
                    // descriptive placeholder; the user only needs to know
                    // there's a block-storage indirection here.
                    return "(() -> Any) /* SIL block-storage capture="
                        + print((int) longAt(args, 0)) + " */";
                case SwiftDeclRecord.SIL_MOVE_ONLY_TYPE:
                    // args = (TypeID inner). Move-only is a wrapper that
                    // changes the type's value semantics; source-level Swift
                    // doesn't have a syntax marker for it yet, so we render
                    // the underlying type as-is.
                    return print((int) longAt(args, 0));
                case SwiftDeclRecord.SIL_BOX_TYPE:
                    // Indirect heap-allocated box used by SIL for captures.
                    // No source-level equivalent — placeholder type.
                    return "AnyObject /* SIL box */";
                case SwiftDeclRecord.SIL_PACK_TYPE:
                    return unknown("SIL pack type — pack expansions not rendered");
                case SwiftDeclRecord.UNBOUND_GENERIC_TYPE: {
                    // args = (DeclID, parent TypeID). Same shape as NOMINAL,
                    // without explicit generic arguments — Swift internally
                    // uses this for the "raw" generic type sans its args.
                    String name = lookupDeclName((int) longAt(args, 0));
                    long parent = longAt(args, 1);
                    if (parent > 0) {
                        String parentText = print((int) parent);
                        if (!isPlaceholder(parentText) && !isPlaceholder(name)) {
                            return parentText + "." + name;
                        }
                    }
                    return name;
                }
                case SwiftDeclRecord.ERROR_TYPE:
                    return unknown("error-type — original signature failed type-check");
                case SwiftDeclRecord.INTEGER_TYPE:
                    return unknown("integer-literal type — value-type not rendered yet");
                default:
                    return unknown("typeID=" + entry.typeId()
                        + " kind=" + entry.recordKind() + " — printer not implemented");
            }
        }

        private String renderTuple(SwiftTypeEntry entry) {
            SwiftAuxRecord[] trailers = entry.trailers();
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < trailers.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                SwiftAuxRecord t = trailers[i];
                // TUPLE_TYPE_ELT: (IdentifierID name, TypeID type)
                long nameId = longAt(t.args(), 0);
                long typeId = longAt(t.args(), 1);
                if (nameId > 0) {
                    String label = info.lookupIdentifier((int) nameId);
                    // Tuple element labels must be plain identifiers.
                    // The bitstream sometimes stores Swift's compound
                    // function name shape ({@code foo(_:bar:)}) in the
                    // identifier table — using that verbatim as a tuple
                    // label produces unparseable {@code (foo(_:bar:): T)}.
                    // Drop the label in that case rather than corrupt
                    // the type signature; the type alone still parses.
                    if (label != null && !label.isEmpty() && isPlainIdentifier(label)) {
                        sb.append(label).append(": ");
                    }
                }
                sb.append(print((int) typeId));
            }
            sb.append(')');
            return sb.toString();
        }

        /**
         * True when {@code s} is a syntactically plain Swift identifier
         * usable as a tuple-element label or argument label — no
         * parentheses, colons, or whitespace.
         */
        private static boolean isPlainIdentifier(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_';
                if (!ok) {
                    return false;
                }
            }
            return !s.isEmpty();
        }

        private String renderFunction(SwiftTypeEntry entry) {
            long[] args = entry.args();
            // FunctionTypeLayout: args[0] = output TypeID; subsequent fixed fields
            // describe representation/throws/async/etc. We don't render attributes
            // yet — just the (params) -> result skeleton.
            String result = print((int) longAt(args, 0));

            SwiftAuxRecord[] trailers = entry.trailers();
            StringBuilder params = new StringBuilder("(");
            for (int i = 0; i < trailers.length; i++) {
                if (i > 0) {
                    params.append(", ");
                }
                SwiftAuxRecord t = trailers[i];
                // FunctionParamLayout: (label, internalLabel, TypeID, ...)
                long label = longAt(t.args(), 0);
                long typeId = longAt(t.args(), 2);
                if (label > 0) {
                    String name = info.lookupIdentifier((int) label);
                    if (name != null && !name.isEmpty()) {
                        params.append(name).append(": ");
                    }
                }
                params.append(print((int) typeId));
            }
            params.append(") -> ").append(result);
            return params.toString();
        }

        /**
         * Resolve a decl ID to a Swift type reference. Three cases:
         * <ol>
         *   <li><b>Local decl with a valid name</b> — emit the identifier
         *       verbatim ({@code "Package"}).</li>
         *   <li><b>External (XREF) decl</b> — walk the XREF's
         *       {@code XREF_*_PATH_PIECE} trailers to produce a fully
         *       qualified path ({@code "Swift.Hashable"} or
         *       {@code "Foundation.Date"}). Cross-module references are
         *       serialized as a base module ID + a list of path pieces in
         *       {@code DECLS_AND_TYPES_BLOCK}; the printer reconstructs them
         *       so the rendered Swift looks like real source rather than a
         *       declID placeholder.</li>
         *   <li><b>Unresolvable</b> — fall back to a Swift-safe placeholder
         *       identifier ({@code _Unresolved_Decl_N}) so the rendered
         *       declaration still parses; a trailing comment documents
         *       what we couldn't resolve.</li>
         * </ol>
         */
        private String lookupDeclName(int declId) {
            SwiftDeclEntry e = info.lookupDecl(declId);
            if (e != null && e.isExternal()) {
                String qn = resolveXref(e);
                if (qn != null) {
                    return qn;
                }
                // XREF malformed or path contains non-type pieces. Fall
                // back to {@code Any} so the surrounding type expression
                // still parses; the comment captures the declID for
                // grep-ability.
                return "Any /* extern decl " + declId + " — XREF not a clean type path */";
            }
            String name = info.lookupDeclName(declId);
            if (name != null && !name.isEmpty() && SwiftIdentifierNames.isValid(name)) {
                return name;
            }
            if (name != null && !name.isEmpty()) {
                // We have a name from the identifier table but it's not a
                // valid Swift identifier (synthesized symbol with spaces,
                // operators, etc.). Emit just the sanitized form — an
                // inline {@code /* original: ... */} comment would land
                // in places where {@code swiftc} doesn't accept it
                // (between an identifier and the {@code .} that follows
                // it in a dotted type expression, for example).
                return SwiftIdentifierNames.sanitize(name);
            }
            return "_Unresolved_Decl_" + declId + " /* declID=" + declId + " */";
        }

        /**
         * Resolve an XREF (cross-module reference) to a <b>fully qualified</b>
         * Swift name like {@code Swift.Hashable} or
         * {@code Foundation.URLSession.DataTaskPublisher}.
         *
         * <p>Why fully qualified? Short names ({@code Hashable}) would
         * require the IDE to scan every imported module to find which
         * provides the symbol — that's expensive in SDK-scale workloads
         * where a single search-path may contain thousands of modules.
         * The XREF record already carries the module name in its first
         * operand, and the path pieces give us every intervening scope;
         * stitching them with dots is O(path length) and skips that
         * resolution step entirely. The output is also unambiguous in the
         * face of overlapping symbol names across imports.
         *
         * <p>XREF layout: {@code (moduleID, pathLen)} followed by
         * {@code pathLen} {@code XREF_*_PATH_PIECE} records (TYPE / VALUE /
         * INITIALIZER / OPERATOR / EXTENSION / GENERIC_PARAM /
         * OPAQUE_RETURN_TYPE). We walk them in order and emit
         * {@code module + "." + piece1 + "." + piece2 + ...}; pieces that
         * don't carry a user-visible name (EXTENSION, GENERIC_PARAM,
         * OPAQUE_RETURN_TYPE) are skipped without inserting a separator.
         */
        private String resolveXref(SwiftDeclEntry xref) {
            long[] args = xref.args();
            int moduleId = args.length > 0 ? (int) args[0] : 0;
            int pathLen = args.length > 1 ? (int) args[1] : Integer.MAX_VALUE;
            String module = info.lookupModuleName(moduleId);
            StringBuilder out = new StringBuilder();
            if (module != null && !module.isEmpty()) {
                out.append(module);
            }
            // XREF pathLen tells us exactly how many follow-up records belong
            // to this reference; anything we captured beyond that is stray
            // (records of the next decl that didn't get its own wanted-offset
            // boundary). Stay within bounds.
            SwiftAuxRecord[] trailers = xref.trailers();
            int n = Math.min(pathLen, trailers.length);
            for (int i = 0; i < n; i++) {
                SwiftAuxRecord aux = trailers[i];
                // For a TYPE reference position we only emit TYPE path pieces.
                // Value/operator/init pieces refer to a value-shaped decl
                // (function, init, operator); embedding their names inside a
                // dotted type expression yields invalid Swift like
                // {@code Foo.<=} or {@code Foo.init(...)}. Skip those so the
                // resulting reference stays a clean qualified type name.
                if (aux.kind() != SwiftDeclRecord.XREF_TYPE_PATH_PIECE) {
                    continue;
                }
                String segment = identOrNull(aux.args(), 0);
                if (segment == null) {
                    continue;
                }
                // Defensive: a TYPE_PATH_PIECE should always name a type,
                // but corrupt / over-captured trailers occasionally surface
                // operator-shaped names like "<=" or compound names with
                // colons. Those can't appear in a Swift type expression —
                // give up on the whole reference so we don't produce
                // unparseable Swift like {@code Foo.<=}.
                if (!SwiftIdentifierNames.isValid(segment)) {
                    return null;
                }
                if (out.length() > 0) {
                    out.append('.');
                }
                out.append(segment);
            }
            return out.length() == 0 ? null : out.toString();
        }

        /**
         * Extract the user-visible name from a single XREF path piece.
         * Layout summary (from {@code ModuleFormat.h}):
         * <ul>
         *   <li>{@code XREF_TYPE_PATH_PIECE} (103): {@code (nameID, privateDisc, restrict, fromClang)}</li>
         *   <li>{@code XREF_VALUE_PATH_PIECE} (104): {@code (typeID, nameID, restrict, fromClang, static)}</li>
         *   <li>{@code XREF_GENERIC_PARAM_PATH_PIECE} (107): generic-param index</li>
         *   <li>{@code XREF_INITIALIZER_PATH_PIECE} (108): always "init"</li>
         *   <li>{@code XREF_OPERATOR_OR_ACCESSOR_PATH_PIECE} (106): operator name</li>
         *   <li>{@code XREF_EXTENSION_PATH_PIECE} (105): extension scope — no name</li>
         *   <li>{@code XREF_OPAQUE_RETURN_TYPE_PATH_PIECE} (152): opaque return type</li>
         * </ul>
         */
        private String xrefPathSegment(SwiftAuxRecord aux) {
            long[] a = aux.args();
            switch (aux.kind()) {
                case SwiftDeclRecord.XREF_TYPE_PATH_PIECE:
                    return identOrNull(a, 0);
                case SwiftDeclRecord.XREF_VALUE_PATH_PIECE:
                    return identOrNull(a, 1);
                case SwiftDeclRecord.XREF_OPERATOR_OR_ACCESSOR_PATH_PIECE:
                    return identOrNull(a, 0);
                case SwiftDeclRecord.XREF_INITIALIZER_PATH_PIECE:
                    return "init";
                case SwiftDeclRecord.XREF_EXTENSION_PATH_PIECE:
                    // Scope marker — no user-visible name. Skip.
                    return null;
                case SwiftDeclRecord.XREF_GENERIC_PARAM_PATH_PIECE:
                case SwiftDeclRecord.XREF_OPAQUE_RETURN_TYPE_PATH_PIECE:
                    return null;
                default:
                    return null;
            }
        }

        private String identOrNull(long[] a, int idx) {
            if (idx >= a.length || a[idx] <= 0) {
                return null;
            }
            String s = info.lookupIdentifier((int) a[idx]);
            return (s == null || s.isEmpty()) ? null : s;
        }

        private long longAt(long[] args, int i) {
            return i < args.length ? args[i] : 0L;
        }

        private String ownershipPrefix(long kind) {
            // Swift's ReferenceOwnership: 0 Strong, 1 Weak, 2 Unowned, 3 UnownedUnsafe.
            switch ((int) kind) {
                case 1:  return "weak ";
                case 2:  return "unowned ";
                case 3:  return "unowned(unsafe) ";
                default: return "";
            }
        }
    }
}
