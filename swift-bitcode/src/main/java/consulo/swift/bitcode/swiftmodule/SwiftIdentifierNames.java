/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Small helpers for emitting Swift identifiers safely.
 * <p>
 * Identifier strings pulled out of {@code IDENTIFIER_DATA_BLOCK} can be:
 * <ul>
 *   <li>Plain Swift identifiers — emit as-is.</li>
 *   <li>Swift keywords ({@code class}, {@code default}, ...) — wrap in
 *       backticks so the parser accepts them as identifiers.</li>
 *   <li>Compiler-synthesized symbol names with spaces or punctuation
 *       (e.g. {@code "extension ContextModel"}) — these can't be a
 *       valid Swift identifier under any escape; we substitute a
 *       Swift-safe placeholder so the surrounding declaration still
 *       parses, with the original captured in a comment elsewhere.</li>
 * </ul>
 */
public final class SwiftIdentifierNames {

    /**
     * Does {@code s} look like a Swift identifier we can emit without
     * any escaping or substitution? Backtick-wrapped keywords are <em>not</em>
     * considered "valid" here — callers should test
     * {@code isValid(name) || isKeyword(name)} and backtick the keyword
     * case explicitly.
     */
    public static boolean isValid(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (!isIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!isIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return !isKeyword(s);
    }

    /**
     * Sanitize {@code raw} into something the Swift parser will accept as
     * an identifier. Punctuation and whitespace become underscores; leading
     * digits get an underscore prefix; empty input becomes
     * {@code _empty}. The result is meant to be paired with a comment
     * preserving the original spelling.
     */
    public static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "_empty";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 4);
        char first = raw.charAt(0);
        if (!isIdentifierStart(first)) {
            sb.append('_');
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (i == 0 ? isIdentifierStart(c) : isIdentifierPart(c)) {
                sb.append(c);
            }
            else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "_empty" : sb.toString();
    }

    /**
     * Emit a name as it should appear in Swift source — backtick-wrapped if
     * it's a reserved keyword, sanitized if it isn't a valid identifier at
     * all, otherwise verbatim.
     */
    public static String emit(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "_anon";
        }
        if (isKeyword(raw)) {
            return "`" + raw + "`";
        }
        // Bare {@code _} is the wildcard-pattern token, not an identifier.
        // Render as {@code _anon} so it can appear as a parameter / case
        // name without triggering a parser error.
        if ("_".equals(raw)) {
            return "_anon";
        }
        if (isValid(raw)) {
            return raw;
        }
        return sanitize(raw);
    }

    public static boolean isKeyword(String s) {
        // Conservative subset — covers the ones we've actually hit in
        // PackageDescription (`default`, `extension`, `Type`). Extending
        // this list is harmless; missing one only forces the printer to
        // emit a name without backticks that swiftc would otherwise accept.
        switch (s) {
            case "associatedtype":
            case "class":
            case "deinit":
            case "enum":
            case "extension":
            case "fileprivate":
            case "func":
            case "import":
            case "init":
            case "inout":
            case "internal":
            case "let":
            case "open":
            case "operator":
            case "private":
            case "precedencegroup":
            case "protocol":
            case "public":
            case "rethrows":
            case "static":
            case "struct":
            case "subscript":
            case "typealias":
            case "var":
            case "break":
            case "case":
            case "continue":
            case "default":
            case "defer":
            case "do":
            case "else":
            case "fallthrough":
            case "for":
            case "guard":
            case "if":
            case "in":
            case "repeat":
            case "return":
            case "switch":
            case "where":
            case "while":
            case "as":
            case "catch":
            case "false":
            case "is":
            case "nil":
            case "super":
            case "self":
            case "Self":
            case "throw":
            case "throws":
            case "true":
            case "try":
            case "Any":
            // Contextual / type-only keywords that the Swift parser
            // accepts as identifiers ONLY when backticked.
            case "Type":
            case "Protocol":
                return true;
            default:
                return false;
        }
    }

    private static boolean isIdentifierStart(char c) {
        return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    private SwiftIdentifierNames() {
    }
}
