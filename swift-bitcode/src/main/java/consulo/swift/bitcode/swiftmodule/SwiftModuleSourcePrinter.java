/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Renders a whole {@code .swiftmodule} as a synthesized {@code .swift} file.
 * Mirrors the {@code dotnet-msil-decompiler} pattern: emit one file with
 * the headline declarations so the IDE's regular Swift parser/PSI can take
 * over from there for indexing, navigation, completion, etc.
 *
 * <p>The output is intentionally lightweight at this milestone — top-level
 * decls only, no member bodies, no attributes, no comments aside from a
 * module-banner header. Bodies and members will land once
 * {@code DECL_MEMBER_NAMES} parsing is wired up.
 */
public final class SwiftModuleSourcePrinter {

    /**
     * Render {@code info} as Swift source. Returns at minimum a banner
     * comment with the module name + target triple; if the index block
     * supplied top-level decl IDs, each gets one declaration emitted in
     * {@code ORDERED_TOP_LEVEL_DECLS} order.
     */
    public static String render(SwiftModuleInfo info) {
        if (info == null) {
            throw new NullPointerException("info");
        }
        StringBuilder sb = new StringBuilder();
        appendBanner(sb, info);

        SwiftDeclPrinter printer = new SwiftDeclPrinter(info);
        long[] order = info.getOrderedTopLevelDeclIds();
        if (order.length == 0) {
            sb.append("// no top-level decls advertised by INDEX_BLOCK.ORDERED_TOP_LEVEL_DECLS\n");
            return sb.toString();
        }
        for (long id : order) {
            SwiftDeclEntry entry = info.lookupDecl((int) id);
            if (entry == null) {
                continue;
            }
            // Filter out decls whose parent is another local decl — those
            // are nested types ({@code Package.Dependency.Kind}) that
            // should render inside their parent's body, not at file
            // level. Native {@code swift symbolgraph-extract} does the
            // same partitioning.
            if (isNestedInsideLocalDecl(info, entry)) {
                continue;
            }
            // Skip compiler-synthesized symbols that shouldn't appear at
            // source top level. Two categories:
            //   * Private discriminators: long hex hashes the compiler
            //     mangles into IDENTIFIER for file-scoped private decls
            //     (e.g. {@code _E4233AB738896A2F7E2AA5FF0FD77146}).
            //   * Sanitized "extension X" identifiers: the bitstream
            //     stores extension-context names like {@code "extension
            //     ContextModel"} in the identifier table; our
            //     sanitiser turns them into {@code extension_ContextModel}
            //     which then masquerades as a top-level type.
            if (isSyntheticName(info.lookupDeclName(entry.declId()))) {
                continue;
            }
            // SwiftDeclPrinter renders the complete declaration including
            // any required body (nominal members, function stubs, etc.).
            String text = printer.print(entry);
            if (text == null || text.isEmpty()) {
                continue;
            }
            sb.append(text);
            if (!text.endsWith("\n")) {
                sb.append('\n');
            }
            sb.append('\n');   // blank line between top-level decls
        }
        return sb.toString();
    }

    private static void appendBanner(StringBuilder sb, SwiftModuleInfo info) {
        sb.append("// swift-module: ").append(safe(info.getModuleName())).append('\n');
        if (info.getTarget() != null) {
            sb.append("// target:        ").append(info.getTarget()).append('\n');
        }
        if (info.getFormatMajor() >= 0) {
            sb.append("// format:        ")
                .append(info.getFormatMajor()).append('.').append(info.getFormatMinor())
                .append('\n');
        }
        if (notBlank(info.getSdkName())) {
            sb.append("// sdk:           ").append(info.getSdkName()).append('\n');
        }
        if (notBlank(info.getSdkVersion())) {
            sb.append("// sdk version:   ").append(info.getSdkVersion()).append('\n');
        }
        if (notBlank(info.getRevision())) {
            sb.append("// revision:      ").append(info.getRevision()).append('\n');
        }
        sb.append('\n');
    }

    /**
     * Same logic as {@code SwiftModuleArchiveFile.hasLocalDeclParent}:
     * skip decls whose parent context resolves to another real local
     * decl, so nested types render inside the parent body via the
     * MEMBERS trailer instead of at file root.
     */
    /**
     * Recognises compiler-synthesized names that shouldn't appear at
     * source top level: 32-char hex private discriminators and the
     * sanitised {@code extension_X} form of "extension X" identifiers
     * the bitstream uses internally.
     */
    private static boolean isSyntheticName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // 32+ hex chars optionally prefixed with `_` — Swift's private
        // file-scope discriminator hash.
        if (name.startsWith("_") && name.length() >= 33) {
            boolean hex = true;
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    hex = false;
                    break;
                }
            }
            if (hex) return true;
        }
        // "extension X" identifiers Swift stores in the table for
        // extension-context names — both raw ("extension Foo") and
        // sanitised ("extension_Foo") forms.
        return name.startsWith("extension ") || name.startsWith("extension_");
    }

    private static boolean isNestedInsideLocalDecl(SwiftModuleInfo info, SwiftDeclEntry decl) {
        int parentId = decl.parentContextId();
        if (parentId <= 0) {
            return false;
        }
        SwiftDeclEntry parent = info.lookupDecl(parentId);
        if (parent == null || parent.isExternal()) {
            return false;
        }
        return SwiftDeclRecord.isDecl(parent.recordKind());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "<unknown>" : s;
    }

    private SwiftModuleSourcePrinter() {
    }
}
