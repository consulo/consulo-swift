/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renders a {@link SwiftDeclEntry} as a Swift source-text declaration —
 * the building block of the decompiler. Includes:
 *
 * <ul>
 *   <li>The decl's headline (modifiers + kind + name + inheritance).</li>
 *   <li>For nominal types and extensions: a body with every member
 *       declaration recursively rendered. Bodies that need a value
 *       (functions, initializers, properties) get a {@code fatalError()}
 *       stub so the synthesized file is accepted by {@code swiftc -parse}
 *       — we don't reconstruct method bodies, only declarations.</li>
 *   <li>For functions, initializers, subscripts: a {@code PARAMETERLIST}
 *       trailing record drives a {@code (label name: Type, ...)} signature
 *       with real parameter types (looked up via {@link SwiftTypePrinter}).</li>
 * </ul>
 *
 * <p>Where bitstream data is missing we emit a Swift comment (e.g.
 * {@code Any /* ... *}{@code /}) so the user can see at a glance what
 * couldn't be extracted and why, while the file still parses.
 */
public final class SwiftDeclPrinter {

    /** Stub body used for methods/inits — we don't reconstruct bodies. */
    private static final String STUB_BODY = "{ fatalError() }";

    private final SwiftModuleInfo info;
    private final SwiftTypePrinter typePrinter;

    public SwiftDeclPrinter(SwiftModuleInfo info) {
        if (info == null) {
            throw new NullPointerException("info");
        }
        this.info = info;
        this.typePrinter = new SwiftTypePrinter(info);
    }

    /** Render the decl as a complete declaration block. */
    public String print(SwiftDeclEntry entry) {
        StringBuilder out = new StringBuilder();
        print(entry, out, 0, new HashSet<>());
        return out.toString();
    }

    // ------------------------------------------------------------------

    private void print(SwiftDeclEntry entry, StringBuilder out, int indent, Set<Integer> visiting) {
        print(entry, out, indent, visiting, false);
    }

    private void print(SwiftDeclEntry entry, StringBuilder out, int indent, Set<Integer> visiting, boolean inProtocol) {
        if (entry == null) {
            return;
        }
        if (!visiting.add(entry.declId())) {
            indent(out, indent);
            out.append("// (recursive member: declID=").append(entry.declId()).append(")\n");
            return;
        }
        try {
            if (entry.isExternal()) {
                indent(out, indent);
                out.append("// extern declID=").append(entry.declId()).append('\n');
                return;
            }
            // Emit each @attribute on its own line just before the decl
            // header. We do this once per decl, so the per-kind printers
            // below don't have to think about preludes.
            appendAttributes(out, entry, indent);
            switch (entry.recordKind()) {
                case SwiftDeclRecord.TYPE_ALIAS_DECL:       printTypeAlias(entry, out, indent); break;
                case SwiftDeclRecord.STRUCT_DECL:           printNominalWithMembers(entry, "struct", out, indent, 5, 7, 8, visiting); break;
                case SwiftDeclRecord.CLASS_DECL:            printClass(entry, out, indent, visiting); break;
                case SwiftDeclRecord.ENUM_DECL:             printEnum(entry, out, indent, visiting); break;
                case SwiftDeclRecord.PROTOCOL_DECL:         printProtocol(entry, out, indent, visiting); break;
                case SwiftDeclRecord.VAR_DECL:              printVar(entry, out, indent, inProtocol); break;
                case SwiftDeclRecord.FUNC_DECL:             printFunc(entry, out, indent, inProtocol); break;
                case SwiftDeclRecord.CONSTRUCTOR_DECL:      printConstructor(entry, out, indent, inProtocol); break;
                case SwiftDeclRecord.DESTRUCTOR_DECL:       printDestructor(entry, out, indent); break;
                case SwiftDeclRecord.EXTENSION_DECL:        printExtension(entry, out, indent, visiting); break;
                case SwiftDeclRecord.ENUM_ELEMENT_DECL:     printEnumElement(entry, out, indent); break;
                case SwiftDeclRecord.PARAM_DECL:            // params are rendered inline by their parent
                    break;
                case SwiftDeclRecord.SUBSCRIPT_DECL:        printSubscript(entry, out, indent, inProtocol); break;
                case SwiftDeclRecord.ACCESSOR_DECL:         // accessors are rendered inline by their var
                    break;
                case SwiftDeclRecord.PATTERN_BINDING_DECL:
                    // PATTERN_BINDING_DECL records the {@code let x = 5}
                    // pattern + initializer shape — each bound name has
                    // its own VAR_DECL that we emit separately. Nothing
                    // to render here; suppress so the output isn't a
                    // sea of "// pattern-binding" comments.
                    break;
                case SwiftDeclRecord.PRECEDENCE_GROUP_DECL: {
                    indent(out, indent);
                    out.append("precedencegroup ").append(emit(name(entry, 0))).append(" { /* body unrendered */ }\n");
                    break;
                }
                case SwiftDeclRecord.INFIX_OPERATOR_DECL:
                case SwiftDeclRecord.PREFIX_OPERATOR_DECL:
                case SwiftDeclRecord.POSTFIX_OPERATOR_DECL: {
                    String opName = name(entry, 0);
                    // Swift requires the operator name to be operator
                    // characters (`+`, `<<`, `~>`, etc.) — never a plain
                    // identifier. The stdlib bitstream occasionally has
                    // operator-decl records whose name field carries an
                    // identifier (likely an extractor mis-attribution).
                    // Skip those rather than emit `infix operator Foo`,
                    // which the parser rightly rejects.
                    if (!isOperatorCharsOnly(opName)) {
                        indent(out, indent);
                        out.append("// skipped operator decl with non-operator name `")
                            .append(opName).append("` (declID=").append(entry.declId()).append(")\n");
                        break;
                    }
                    indent(out, indent);
                    String kw = entry.recordKind() == SwiftDeclRecord.PREFIX_OPERATOR_DECL
                        ? "prefix" : entry.recordKind() == SwiftDeclRecord.POSTFIX_OPERATOR_DECL
                        ? "postfix" : "infix";
                    // Emit the operator name verbatim — DON'T route
                    // through {@code emit()}, because that sanitiser
                    // turns operator characters like {@code *} into
                    // identifier-shaped names like {@code __}. Operators
                    // are not identifiers; they don't need (or want)
                    // identifier sanitisation.
                    out.append(kw).append(" operator ").append(opName).append('\n');
                    break;
                }
                case SwiftDeclRecord.MACRO_DECL: {
                    indent(out, indent);
                    out.append("// macro decl (declID=").append(entry.declId())
                        .append(") — macro body extraction not implemented\n");
                    break;
                }
                default:
                    indent(out, indent);
                    out.append("// TODO: ").append(SwiftDeclRecord.name(entry.recordKind()))
                        .append(" (declID=").append(entry.declId())
                        .append(") — printer not implemented yet\n");
            }
        }
        finally {
            visiting.remove(entry.declId());
        }
    }

    // ---------------------------------------------------------- specific kinds

    /** {@code args = (name, ctx, underlyingType, _, implicit, genEnv, access, deps...)} */
    private void printTypeAlias(SwiftDeclEntry e, StringBuilder out, int indent) {
        indent(out, indent);
        appendAccess(out, longAt(e, 6));
        out.append("typealias ").append(emit(name(e, 0)));
        long underlying = longAt(e, 2);
        out.append(" = ");
        if (underlying > 0) {
            out.append(typePrinter.print((int) underlying));
        }
        else {
            out.append("Any /* underlying type missing */");
        }
        out.append('\n');
    }

    /**
     * Nominal types with the same inheritance shape: STRUCT/ENUM/...
     * @param accessArg index of access-level field in args
     * @param numInhArg index of {@code numInheritedTypes}
     * @param inhStart  index of the first entry in the inherited BCArray
     */
    private void printNominalWithMembers(
        SwiftDeclEntry e, String kindKeyword, StringBuilder out, int indent,
        int accessArg, int numInhArg, int inhStart, Set<Integer> visiting
    ) {
        indent(out, indent);
        appendAccess(out, longAt(e, accessArg));
        out.append(kindKeyword).append(' ').append(emit(name(e, 0)));
        appendGenericParams(out, e);
        appendInherited(out, e, inhStart, (int) longAt(e, numInhArg), 0);
        appendMembersBody(out, e, indent, visiting);
    }

    /**
     * {@code args = (name, ctx, implicit, objc, actor, inheritsConv, hasMissingDes,
     *               genEnv, superclass, access, #conf, #inh, [inheritedIDs...])}
     */
    private void printClass(SwiftDeclEntry e, StringBuilder out, int indent, Set<Integer> visiting) {
        indent(out, indent);
        boolean isActor = longAt(e, 4) != 0;
        appendAccess(out, longAt(e, 9));
        out.append(isActor ? "actor " : "class ").append(emit(name(e, 0)));
        appendGenericParams(out, e);

        long superclass = longAt(e, 8);
        StringBuilder bases = new StringBuilder();
        if (superclass > 0) {
            bases.append(typePrinter.print((int) superclass));
        }
        appendInheritedTo(bases, e, 12, (int) longAt(e, 11));
        if (bases.length() > 0) {
            out.append(": ").append(bases);
        }
        appendMembersBody(out, e, indent, visiting);
    }

    /**
     * {@code args = (name, ctx, implicit, objc, genEnv, rawType, access, #conf, #inh, [inheritedIDs...])}
     */
    private void printEnum(SwiftDeclEntry e, StringBuilder out, int indent, Set<Integer> visiting) {
        indent(out, indent);
        appendAccess(out, longAt(e, 6));
        out.append("enum ").append(emit(name(e, 0)));
        appendGenericParams(out, e);

        StringBuilder bases = new StringBuilder();
        long raw = longAt(e, 5);
        if (raw > 0) {
            String rawText = typePrinter.print((int) raw);
            // Enum raw types in Swift must be literal-convertible primitives
            // (Int, String, ...). Function-type / tuple-type / placeholder
            // raw-types are not legal as a raw type — emitting them here
            // produces something like {@code enum X: (() -> Any)} which
            // {@code swiftc -parse} accepts grammatically but our PSI
            // parser (correctly) rejects. Drop anything that isn't a
            // single-token identifier.
            if (looksLikeIdentifierPath(rawText)) {
                bases.append(rawText);
            }
        }
        appendInheritedTo(bases, e, 9, (int) longAt(e, 8));
        if (bases.length() > 0) {
            out.append(": ").append(bases);
        }
        appendMembersBody(out, e, indent, visiting);
    }

    /**
     * True if {@code text} is a chain of dot-separated identifiers — the
     * shape Swift accepts in inheritance / raw-type positions. Reject
     * function types {@code (... -> ...)}, tuples {@code (A, B)}, sugar
     * {@code [T]} / {@code T?} / {@code T!}, and placeholder comments.
     */
    /**
     * True when {@code text} consists ONLY of Swift operator characters
     * — used to guard the {@code operator} decl printer against
     * identifier names that would produce unparseable
     * {@code infix operator Foo}.
     */
    private static boolean isOperatorCharsOnly(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Swift "operator-head" ASCII subset matching the lexer's
            // OP_CHAR rule (minus `>` which we kept primitive).
            boolean ok = c == '/' || c == '=' || c == '-' || c == '+' || c == '!'
                || c == '*' || c == '%' || c == '&' || c == '|' || c == '<'
                || c == '>' || c == '^' || c == '~' || c == '?' || c == '.';
            if (!ok) return false;
        }
        return true;
    }

    private static boolean looksLikeIdentifierPath(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '`';
            if (!ok) return false;
        }
        // {@code Foo.Type} is a metatype expression — valid Swift in normal
        // type positions but illegal as an enum raw-type or in an
        // inheritance clause. Reject the suffix specifically.
        if (text.endsWith(".Type") || text.endsWith(".Protocol")) return false;
        return true;
    }

    /**
     * {@code args = (name, ctx, implicit, classBounded, objc, existentialSupported,
     *               superclassDecl, access, [deps...])}
     */
    private void printProtocol(SwiftDeclEntry e, StringBuilder out, int indent, Set<Integer> visiting) {
        indent(out, indent);
        appendAccess(out, longAt(e, 7));
        out.append("protocol ").append(emit(name(e, 0)));
        boolean classBounded = longAt(e, 3) != 0;
        long superclassDecl = longAt(e, 6);
        StringBuilder bases = new StringBuilder();
        if (classBounded) {
            bases.append("AnyObject");
        }
        if (superclassDecl > 0) {
            String n = declNameOrPlaceholder((int) superclassDecl);
            if (looksLikeIdentifierPath(n)) {
                if (bases.length() > 0) {
                    bases.append(", ");
                }
                bases.append(n);
            }
        }
        // INHERITED_PROTOCOLS is the canonical source of protocol inheritance —
        // available as a trailer of kind 162 (TRAILING_INFO(INHERITED_PROTOCOLS)).
        // Filter unresolved entries: a placeholder in the inheritance list
        // ({@code protocol P: Any /* ... */, Q { ... }}) breaks downstream
        // PSI parsing because {@code Any} isn't a legal protocol-inheritance
        // entry. Skip silently — the placeholder still shows up below in
        // the body comments for grep-ability.
        SwiftAuxRecord inh = e.findTrailer(SwiftDeclRecord.INHERITED_PROTOCOLS);
        if (inh != null) {
            for (long protoDeclId : inh.args()) {
                if (protoDeclId == 0) continue;
                String n = declNameOrPlaceholder((int) protoDeclId);
                if (n == null || n.isEmpty() || !looksLikeIdentifierPath(n)) continue;
                if (bases.length() > 0) bases.append(", ");
                bases.append(n);
            }
        }
        if (bases.length() > 0) {
            out.append(": ").append(bases);
        }
        // Protocol bodies require their methods; emit declarations only.
        appendMembersBody(out, e, indent, visiting, true);
    }

    /**
     * VAR_DECL — render as {@code <access> static let|var name: Type}.
     * Args layout (see VarLayout in ModuleFormat.h):
     * 0=name, 4=isStatic, 5=introducer, 15=interface type, 18=access level.
     */
    private void printVar(SwiftDeclEntry e, StringBuilder out, int indent, boolean inProtocol) {
        indent(out, indent);
        appendAccess(out, longAt(e, 18));
        if (longAt(e, 4) != 0) {
            out.append("static ");
        }
        out.append(introducerKeyword(longAt(e, 5))).append(' ');
        out.append(emit(name(e, 0)));
        long ifaceType = longAt(e, 15);
        out.append(": ");
        if (ifaceType > 0) {
            out.append(typePrinter.print((int) ifaceType));
        }
        else {
            out.append("Any /* type missing — interfaceType field is 0 */");
        }
        // Protocol context: properties must be declared with `{ get }` or
        // `{ get set }`. Outside a protocol we need either an initializer
        // (for `let`) or a computed-property body (for `var`).
        if (inProtocol) {
            out.append(" { get set }\n");
        }
        else if (longAt(e, 5) == 0 /* let */) {
            out.append(" = fatalError()\n");
        }
        else {
            // Computed-property form: `var x: T { fatalError() }` — equivalent
            // to a getter that returns the body's expression.
            out.append(" { fatalError() }\n");
        }
    }

    /**
     * FUNC_DECL — render full {@code (label name: Type, ...) -> Result} with
     * parameter types from the PARAMETERLIST trailer.
     */
    private void printFunc(SwiftDeclEntry e, StringBuilder out, int indent, boolean inProtocol) {
        indent(out, indent);
        appendAccess(out, longAt(e, 17));
        if (longAt(e, 2) != 0) {
            out.append("static ");
        }
        boolean isAsync = longAt(e, 7) != 0;
        boolean throws_ = longAt(e, 8) != 0;
        out.append("func ").append(emit(name(e, 23)));
        appendGenericParams(out, e);
        appendParameterList(out, e, 16);   // arg[16] = numNameComponents
        if (isAsync) out.append(" async");
        if (throws_) out.append(" throws");
        long resultType = longAt(e, 11);
        out.append(" -> ");
        if (resultType > 0) {
            out.append(typePrinter.print((int) resultType));
        }
        else {
            out.append("Void");
        }
        if (inProtocol) {
            out.append('\n');
        }
        else {
            out.append(' ').append(STUB_BODY).append('\n');
        }
    }

    /**
     * CONSTRUCTOR_DECL — {@code init(...) [throws] [stubBody]}.
     */
    private void printConstructor(SwiftDeclEntry e, StringBuilder out, int indent, boolean inProtocol) {
        indent(out, indent);
        appendAccess(out, longAt(e, 13));
        boolean failable = longAt(e, 1) != 0;
        boolean isAsync = longAt(e, 5) != 0;
        boolean throws_ = longAt(e, 6) != 0;
        out.append("init").append(failable ? "?" : "");
        // Failable init followed by a generic clause needs WS between `?`
        // and `<` so the lexer doesn't combine `?<` into a single
        // operator token (`?` and `<` are both in {@code OP_CHAR}).
        // {@code init?<T>(...)} is valid Swift but our lexer sees the
        // glued `?<` as one operator. Inserting a space breaks the
        // greedy match without changing semantics.
        if (failable && e.findTrailer(SwiftDeclRecord.GENERIC_PARAM_LIST) != null) {
            out.append(' ');
        }
        appendGenericParams(out, e);
        appendParameterList(out, e, 16);
        if (isAsync) out.append(" async");
        if (throws_) out.append(" throws");
        // Protocol inits are declarations only — no body.
        out.append(inProtocol ? "\n" : (" " + STUB_BODY + "\n"));
    }

    /** DESTRUCTOR_DECL — {@code deinit { ... }}. */
    private void printDestructor(SwiftDeclEntry e, StringBuilder out, int indent) {
        indent(out, indent);
        out.append("deinit ").append(STUB_BODY).append('\n');
    }

    /**
     * ExtensionLayout: {@code (extendedType, extendedNominal, ctx, implicit,
     * genEnv, #conf, #inh, [inheritedIDs...])}
     */
    private void printExtension(SwiftDeclEntry e, StringBuilder out, int indent, Set<Integer> visiting) {
        long extendedType = longAt(e, 0);
        long extendedDecl = longAt(e, 1);
        String header;
        if (extendedType > 0) {
            header = typePrinter.print((int) extendedType);
        }
        else if (extendedDecl > 0) {
            header = declNameOrPlaceholder((int) extendedDecl);
        }
        else {
            header = "";
        }
        // {@code extension Any { ... }} is rejected by both swiftc and our
        // PSI parser — Swift requires a concrete extended type. If we
        // couldn't resolve it, emit a comment block instead of producing
        // invalid syntax that takes the parser down with it.
        if (!looksLikeIdentifierPath(header)) {
            indent(out, indent);
            out.append("// extension on unresolved type (declID=")
                .append(e.declId()).append(", body suppressed)\n");
            return;
        }
        indent(out, indent);
        out.append("extension ").append(header);
        appendInherited(out, e, 7, (int) longAt(e, 6), 0);
        appendMembersBody(out, e, indent, visiting);
    }

    /**
     * ENUM_ELEMENT_DECL — {@code case name(...payload)}. Args layout:
     * 0=ctx, 1=implicit, 2=hasPayload, 7=numNameTokens, 8..=name components.
     */
    private void printEnumElement(SwiftDeclEntry e, StringBuilder out, int indent) {
        indent(out, indent);
        out.append("case ").append(emit(name(e, 8)));
        boolean hasPayload = longAt(e, 2) != 0;
        if (hasPayload) {
            // Payload types come from a trailing PARAM_DECL list. Enum
            // case labels are restricted to single names — pass
            // {@code allowTwoNames=false} so we never emit
            // {@code case foo(label name: T)} (illegal).
            appendParameterList(out, e, 7, false);
        }
        out.append('\n');
    }

    /** SUBSCRIPT_DECL — {@code subscript(args) -> T }. */
    private void printSubscript(SwiftDeclEntry e, StringBuilder out, int indent, boolean inProtocol) {
        indent(out, indent);
        appendAccess(out, longAt(e, 14));
        out.append("subscript");
        appendGenericParams(out, e);
        appendParameterList(out, e, 17);
        long resultType = longAt(e, 11);
        out.append(" -> ");
        if (resultType > 0) {
            out.append(typePrinter.print((int) resultType));
        }
        else {
            out.append("Any /* subscript element type missing */");
        }
        // Subscripts require an accessor block. In a protocol context that's
        // {@code { get set }}; everywhere else we stub a getter body.
        out.append(inProtocol ? " { get set }\n" : " { get { fatalError() } }\n");
    }

    // ---------------------------------------------------------- shared bits

    /**
     * Recursively emit member declarations between {@code {...}}. Members
     * come from the {@link SwiftDeclRecord#MEMBERS} trailer (an array of
     * decl IDs); each member's full record gets printed at one deeper
     * indent level.
     */
    private void appendMembersBody(StringBuilder out, SwiftDeclEntry e, int indent, Set<Integer> visiting) {
        appendMembersBody(out, e, indent, visiting, false);
    }

    private void appendMembersBody(StringBuilder out, SwiftDeclEntry e, int indent, Set<Integer> visiting, boolean inProtocol) {
        SwiftAuxRecord members = e.findTrailer(SwiftDeclRecord.MEMBERS);
        if (members == null) {
            out.append(" {\n");
            indent(out, indent + 1);
            out.append("// (no MEMBERS record captured — body not extracted)\n");
            indent(out, indent);
            out.append("}\n");
            return;
        }
        out.append(" {\n");
        if (members.args().length == 0) {
            indent(out, indent + 1);
            out.append("// empty body\n");
        }
        else {
            for (long memberId : members.args()) {
                if (memberId <= 0) {
                    continue;
                }
                SwiftDeclEntry memberEntry = info.lookupDecl((int) memberId);
                if (memberEntry == null) {
                    indent(out, indent + 1);
                    out.append("// member declID=").append(memberId)
                        .append(" — not resolvable\n");
                    continue;
                }
                print(memberEntry, out, indent + 1, visiting, inProtocol);
            }
        }
        indent(out, indent);
        out.append("}\n");
    }

    /**
     * Append the parameter signature {@code (label name: Type, ...)} using
     * the PARAMETERLIST trailer (one decl ID per param). If no trailer is
     * present, fall back to argument-label tokens stored at {@code args[nameArrayStart..]}
     * with {@code _: Any} placeholder types — that still parses, and a
     * comment notes why.
     */
    private void appendParameterList(StringBuilder out, SwiftDeclEntry e, int nameCountArg) {
        appendParameterList(out, e, nameCountArg, true);
    }

    /**
     * @param allowTwoNames when {@code false}, never emit the
     *                      {@code label name:} two-name form — used for
     *                      enum case payloads, where Swift's grammar
     *                      restricts the label slot to a single
     *                      identifier (or {@code _}).
     */
    private void appendParameterList(StringBuilder out, SwiftDeclEntry e,
                                     int nameCountArg, boolean allowTwoNames) {
        SwiftAuxRecord params = e.findTrailer(SwiftDeclRecord.PARAMETERLIST);
        if (params == null) {
            // Fall back to label tokens
            long n = longAt(e, nameCountArg);
            int labels = (int) Math.max(n - 1, 0);
            out.append('(');
            for (int i = 0; i < labels; i++) {
                if (i > 0) out.append(", ");
                long lblId = longAt(e, nameCountArg + 1 + i);
                String lbl = lblId > 0 ? info.lookupIdentifier((int) lblId) : null;
                out.append(lbl == null ? "_" : SwiftIdentifierNames.emit(lbl))
                    .append(": Any /* no PARAMETERLIST trailer; param type not extracted */");
            }
            out.append(')');
            return;
        }
        out.append('(');
        long[] paramIds = params.args();
        for (int i = 0; i < paramIds.length; i++) {
            if (i > 0) out.append(", ");
            int pid = (int) paramIds[i];
            SwiftDeclEntry paramDecl = info.lookupDecl(pid);
            if (paramDecl == null || paramDecl.recordKind() != SwiftDeclRecord.PARAM_DECL) {
                out.append("_: Any /* PARAM_DECL ").append(pid).append(" not captured */");
                continue;
            }
            // PARAM_DECL layout: args[0]=argName, args[1]=paramName, args[4]=interfaceType
            String argName = identOr(paramDecl, 0, "_");
            String paramName = identOr(paramDecl, 1, argName);
            long typeId = longAt(paramDecl, 4);
            if (!argName.equals(paramName) && allowTwoNames) {
                out.append(SwiftIdentifierNames.emit(argName)).append(' ')
                    .append(SwiftIdentifierNames.emit(paramName));
            }
            else {
                // Single-label form: prefer the public {@code argName}
                // for func/init context (when allowTwoNames is true but
                // both names are equal), or for enum-case context where
                // two-name form is forbidden.
                out.append(SwiftIdentifierNames.emit(argName));
            }
            out.append(": ");
            if (typeId > 0) {
                out.append(typePrinter.print((int) typeId));
            }
            else {
                out.append("Any /* param type missing */");
            }
        }
        out.append(')');
    }

    /**
     * If the decl has a GENERIC_PARAM_LIST trailer, render {@code <T1, T2>}.
     * Constraints aren't pulled out yet — those live in the GENERIC_SIGNATURE
     * record which is a heavier dependency; a comment notes the elision.
     */
    private void appendGenericParams(StringBuilder out, SwiftDeclEntry e) {
        SwiftAuxRecord gp = e.findTrailer(SwiftDeclRecord.GENERIC_PARAM_LIST);
        if (gp == null) {
            return;
        }
        long[] ids = gp.args();
        if (ids.length == 0) {
            return;
        }
        out.append('<');
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) out.append(", ");
            SwiftDeclEntry pe = info.lookupDecl((int) ids[i]);
            String n = pe == null ? null : info.lookupDeclName(pe.declId());
            out.append(n == null || n.isEmpty() ? ("T" + (i + 1)) : SwiftIdentifierNames.emit(n));
        }
        out.append('>');
    }

    private void appendInherited(StringBuilder out, SwiftDeclEntry e,
                                  int arrayStart, int count, int extraStart) {
        StringBuilder bases = new StringBuilder();
        appendInheritedTo(bases, e, arrayStart, count);
        if (bases.length() > 0) {
            out.append(": ").append(bases);
        }
    }

    private void appendInheritedTo(StringBuilder bases, SwiftDeclEntry e, int arrayStart, int count) {
        for (int i = 0; i < count; i++) {
            long raw = longAt(e, arrayStart + i);
            if (raw == 0) {
                continue;
            }
            // Inherited entries are packed as
            // (typeID << (NumProtocolConformanceOptions + 1)) | options | suppressed.
            int typeId = (int) (raw >>> 8);
            if (typeId == 0) {
                continue;
            }
            String t = typePrinter.print(typeId);
            // Don't add anything that isn't an identifier-shaped type to
            // an inheritance clause. Swift's inheritance grammar accepts
            // only dotted type-identifiers — function types like
            // {@code (() -> Any)}, tuples {@code (A, B)}, sugar {@code [T]}
            // and our own {@code Any /* ... */} placeholders are all
            // invalid in this position and break downstream PSI parsing.
            if (t == null || t.isEmpty() || !looksLikeIdentifierPath(t)) {
                continue;
            }
            if (bases.length() > 0) {
                bases.append(", ");
            }
            bases.append(t);
        }
    }

    private void appendAccess(StringBuilder sb, long access) {
        String kw = accessKeyword(access);
        if (!kw.isEmpty()) {
            sb.append(kw).append(' ');
        }
    }

    /**
     * Swift's {@code AccessLevel}: 0 Private, 1 FilePrivate, 2 Internal,
     * 3 Package, 4 Public, 5 Open. {@code internal} is the default — emit
     * the empty string.
     */
    private static String accessKeyword(long level) {
        switch ((int) level) {
            case 0: return "private";
            case 1: return "fileprivate";
            case 3: return "package";
            case 4: return "public";
            case 5: return "open";
            default: return "";
        }
    }

    /** Swift's {@code VarDeclIntroducer}: 0 let, 1 var, 2 inout, 3 borrowing. */
    private static String introducerKeyword(long kind) {
        switch ((int) kind) {
            case 0:  return "let";
            case 1:  return "var";
            case 2:  return "inout";
            case 3:  return "borrowing";
            default: return "let";
        }
    }

    /**
     * Resolve a decl ID to a Swift identifier suitable for use in a
     * declaration's inheritance clause, generic-constraint clause, or
     * superclass position. External (XREF) refs emit a fully qualified
     * {@code Module.Path.Name} so the IDE doesn't have to re-resolve
     * which module owns a given short name — see
     * {@link SwiftTypePrinter} for the same logic on the type-printer
     * side.
     */
    private String declNameOrPlaceholder(int declId) {
        SwiftDeclEntry e = info.lookupDecl(declId);
        if (e == null) {
            return "Any /* decl " + declId + " not captured */";
        }
        if (e.isExternal()) {
            // Build full Module.Path.Name from the XREF. We respect the
            // path-length field (args[1]) so we don't drift into the next
            // entity's records, and we only emit TYPE_PATH_PIECE segments
            // here because this method is used in type positions
            // (inheritance, generic constraint, superclass).
            long[] args = e.args();
            int moduleId = args.length > 0 ? (int) args[0] : 0;
            int pathLen = args.length > 1 ? (int) args[1] : Integer.MAX_VALUE;
            String module = info.lookupModuleName(moduleId);
            StringBuilder out = new StringBuilder();
            if (module != null && !module.isEmpty()) {
                out.append(module);
            }
            SwiftAuxRecord[] trailers = e.trailers();
            int n = Math.min(pathLen, trailers.length);
            for (int i = 0; i < n; i++) {
                SwiftAuxRecord aux = trailers[i];
                if (aux.kind() != SwiftDeclRecord.XREF_TYPE_PATH_PIECE) {
                    continue;
                }
                String segment = ident(aux.args(), 0);
                // Reject anything that isn't a clean Swift identifier —
                // compound names like {@code validateVersion(_:)} would
                // produce {@code Module.validateVersion(_:)} in source which
                // doesn't parse as a type expression.
                if (segment == null || !SwiftIdentifierNames.isValid(segment)) {
                    // Whole XREF is unusable as a type reference.
                    return "Any /* extern decl " + declId
                        + " — XREF segment not a type identifier */";
                }
                if (out.length() > 0) {
                    out.append('.');
                }
                out.append(segment);
            }
            return out.length() == 0 ? "_Unresolved_Extern_" + declId : out.toString();
        }
        String n = info.lookupDeclName(declId);
        if (n == null || n.isEmpty()) {
            return "_Unresolved_Decl_" + declId;
        }
        if (!SwiftIdentifierNames.isValid(n)) {
            return SwiftIdentifierNames.sanitize(n);
        }
        return n;
    }

    private String xrefPathSegment(SwiftAuxRecord aux) {
        long[] a = aux.args();
        switch (aux.kind()) {
            case SwiftDeclRecord.XREF_TYPE_PATH_PIECE:               return ident(a, 0);
            case SwiftDeclRecord.XREF_VALUE_PATH_PIECE:              return ident(a, 1);
            case SwiftDeclRecord.XREF_OPERATOR_OR_ACCESSOR_PATH_PIECE: return ident(a, 0);
            case SwiftDeclRecord.XREF_INITIALIZER_PATH_PIECE:        return "init";
            default:                                                  return null;
        }
    }

    private String ident(long[] a, int idx) {
        if (idx >= a.length || a[idx] <= 0) return null;
        String s = info.lookupIdentifier((int) a[idx]);
        return (s == null || s.isEmpty()) ? null : s;
    }

    /** Resolve identifier arg, returning {@code def} if absent. */
    private String identOr(SwiftDeclEntry e, int argIdx, String def) {
        long id = longAt(e, argIdx);
        if (id <= 0) return def;
        String s = info.lookupIdentifier((int) id);
        return (s == null || s.isEmpty()) ? def : s;
    }

    private String name(SwiftDeclEntry e, int argIdx) {
        long id = longAt(e, argIdx);
        String s = id > 0 ? info.lookupIdentifier((int) id) : null;
        if (s != null && !s.isEmpty()) {
            return s;
        }
        return "_anon_" + e.declId();
    }

    private static String emit(String name) {
        return SwiftIdentifierNames.emit(name);
    }

    private static void indent(StringBuilder out, int n) {
        for (int i = 0; i < n; i++) out.append("    ");
    }

    /**
     * Walk the decl's prelude records and emit each {@code @attribute} on
     * its own line at the given indent. Non-attribute prelude records
     * ({@code DESERIALIZATION_SAFETY}, {@code ERROR_FLAG},
     * {@code ABI_ONLY_COUNTERPART}) are silently skipped — they're
     * compiler-internal markers, not source attributes.
     *
     * <p>Attribute formatting comes from {@link SwiftDeclAttribute} which
     * knows the source spelling for every {@code DECL_ATTR} record kind
     * the compiler emits ({@code @inlinable}, {@code @usableFromInline},
     * {@code @frozen}, {@code @available(...)}, etc.). Simple attributes
     * render as bare {@code @Name}; parameterised attributes render as
     * {@code @Name(/* payload elided *}{@code /)} because we don't decode
     * the inner args yet — same spirit as a real {@code .swiftinterface}
     * file, just without the args.
     */
    private void appendAttributes(StringBuilder out, SwiftDeclEntry e, int indent) {
        for (SwiftAuxRecord aux : e.preludes()) {
            // Prefer the richer overload that decodes payloads for known
            // attributes ({@code @available} version tuples + messages,
            // etc.) — falls back to the bare-name form when the attribute
            // is a {@code SIMPLE_DECL_ATTR}.
            String src = SwiftDeclAttribute.renderAsSource(aux);
            if (src == null) {
                continue;
            }
            indent(out, indent);
            out.append(src).append('\n');
        }
    }

    private static long longAt(SwiftDeclEntry e, int i) {
        long[] a = e.args();
        return i < a.length ? a[i] : 0L;
    }

    private static long longAt0(long[] a, int i) {
        return i < a.length ? a[i] : 0L;
    }
}
