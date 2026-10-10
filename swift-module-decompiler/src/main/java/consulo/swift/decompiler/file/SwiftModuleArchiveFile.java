/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.swift.decompiler.file;

import consulo.swift.bitcode.swiftmodule.SwiftDeclEntry;
import consulo.swift.bitcode.swiftmodule.SwiftDeclPrinter;
import consulo.swift.bitcode.swiftmodule.SwiftDeclRecord;
import consulo.swift.bitcode.swiftmodule.SwiftModuleInfo;
import consulo.swift.bitcode.swiftmodule.SwiftModuleReader;
import consulo.swift.bitcode.swiftmodule.SwiftModuleSourcePrinter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual archive view of a {@code .swiftmodule} binary. Treats the
 * module the way Consulo treats a {@code .dll}: one logical
 * <em>archive</em> with synthesized source files inside.
 *
 * <p>Tree layout (mirrors what a Swift Package would look like on disk):
 * <pre>
 *   {@code <ModuleName>.swift}            // module-level banner
 *   {@code Sources/}                      // directory holding per-decl files
 *     {@code Sources/Package.swift}
 *     {@code Sources/Product.swift}
 *     {@code Sources/Target.swift}
 *     {@code Sources/...}
 * </pre>
 *
 * The IDE side wraps this in a {@code VirtualFileSystem} so a user clicks
 * a {@code .swiftmodule} in Project View and gets to browse those synthetic
 * files just like real source — full PSI/parsing applies because the
 * bodies are valid Swift (see {@code SwiftDeclPrinter}).
 *
 * <p>Per-decl files are nice on their own — focus-locked file tabs, smaller
 * scroll-bars, structure-view summaries that aren't dominated by extensions.
 * Top-level types that re-use a name (rare in PackageDescription but common
 * elsewhere) get a {@code _<n>} disambiguator suffix.
 */
public final class SwiftModuleArchiveFile {

    /** Subdirectory inside the archive where per-decl files live. */
    public static final String SOURCES_DIR = "Sources/";

    private final SwiftModuleReader reader;
    private final SwiftModuleInfo info;
    private final long lastModified;
    private final String moduleName;

    /** Insertion-ordered: directories first (sort key), then files. */
    private final Map<String, SwiftArchiveEntry> entries;

    private SwiftModuleArchiveFile(SwiftModuleReader reader, long lastModified) {
        this.reader = reader;
        this.info = reader.readModuleInfo();
        this.lastModified = lastModified;
        String name = info.getModuleName();
        this.moduleName = (name == null || name.isEmpty()) ? "Module" : name;
        this.entries = build();
    }

    /** Open a {@code .swiftmodule} from disk and parse it into an archive view. */
    public static SwiftModuleArchiveFile open(Path path) throws IOException {
        long mtime = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
        SwiftModuleReader r = SwiftModuleReader.open(path);
        return new SwiftModuleArchiveFile(r, mtime);
    }

    /** Open from an already-loaded {@link SwiftModuleReader}. */
    public static SwiftModuleArchiveFile open(SwiftModuleReader reader, long lastModified) {
        if (reader == null) {
            throw new NullPointerException("reader");
        }
        return new SwiftModuleArchiveFile(reader, lastModified);
    }

    // -------------------------------------------------------- public API

    public SwiftModuleReader getReader() {
        return reader;
    }

    public SwiftModuleInfo getInfo() {
        return info;
    }

    public String getModuleName() {
        return moduleName;
    }

    public long getLastModified() {
        return lastModified;
    }

    /** Total entries (both files and directories). */
    public int getEntryCount() {
        return entries.size();
    }

    /** Lookup an entry by full archive path; {@code null} if missing. */
    public SwiftArchiveEntry getEntry(String path) {
        return entries.get(path);
    }

    /** Enumerate every entry in archive order (directories then files). */
    public Collection<SwiftArchiveEntry> entries() {
        return entries.values();
    }

    /**
     * Open an input stream over the entry's payload — empty for directories,
     * raw UTF-8 Swift source for {@link SwiftSourceArchiveEntry}.
     */
    public InputStream openStream(SwiftArchiveEntry entry) {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        if (entry instanceof SwiftSourceArchiveEntry s) {
            return new ByteArrayInputStream(s.getContent());
        }
        return InputStream.nullInputStream();
    }

    // -------------------------------------------------------- assembly

    private Map<String, SwiftArchiveEntry> build() {
        Map<String, SwiftArchiveEntry> out = new LinkedHashMap<>();

        // 1) Module-level banner file at archive root.
        String banner = renderBanner();
        out.put(moduleName + ".swift",
            new SwiftSourceArchiveEntry(moduleName + ".swift", banner, lastModified));

        long[] order = info.getOrderedTopLevelDeclIds();
        if (order.length == 0) {
            // Nothing else to do — the banner is the whole archive.
            return out;
        }

        // 2) `Sources/` directory marker. Inserted unconditionally when we
        //    have any top-level decls so the tree shape is predictable for
        //    callers walking entries().
        out.put(SOURCES_DIR, new SwiftDirArchiveEntry(SOURCES_DIR, lastModified));

        // 3) One file per *true* top-level decl. ORDERED_TOP_LEVEL_DECLS
        //    can include nested types (e.g. {@code Package.Dependency.Kind})
        //    that appear in the parent's MEMBERS list too — filter them
        //    out via the decl's parentContextId so the IDE's project view
        //    shows each type exactly once, in its real source location.
        //    Native {@code swift symbolgraph-extract} does the same: a
        //    nested type isn't a separate top-level symbol.
        SwiftDeclPrinter printer = new SwiftDeclPrinter(info);
        Map<String, Integer> collisionCount = new LinkedHashMap<>();
        for (long declId : order) {
            SwiftDeclEntry decl = info.lookupDecl((int) declId);
            if (decl == null) {
                continue;
            }
            if (hasLocalDeclParent(info, decl)) {
                // Renders inline as a member of its parent. Skip.
                continue;
            }
            String entryName = pickFileName(decl);
            String path;
            int collision = collisionCount.merge(entryName, 1, Integer::sum);
            if (collision == 1) {
                path = SOURCES_DIR + entryName + ".swift";
            }
            else {
                path = SOURCES_DIR + entryName + "_" + collision + ".swift";
            }

            String source = renderDeclFile(printer, decl);
            out.put(path, new SwiftSourceArchiveEntry(path, source, lastModified));
        }
        return out;
    }

    /**
     * Render the file content for one top-level decl: a small banner
     * (module + decl kind + USR placeholder) followed by the decl's full
     * Swift text. Keeps each file self-describing without re-running the
     * extractor.
     */
    private String renderDeclFile(SwiftDeclPrinter printer, SwiftDeclEntry decl) {
        StringBuilder sb = new StringBuilder();
        sb.append("// swift-module: ").append(moduleName).append('\n');
        sb.append("// kind:          ").append(SwiftDeclRecord.name(decl.recordKind())).append('\n');
        if (info.getTarget() != null) {
            sb.append("// target:        ").append(info.getTarget()).append('\n');
        }
        sb.append('\n');
        sb.append(printer.print(decl));
        if (sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
        return sb.toString();
    }

    private String renderBanner() {
        // The whole-module renderer is already format-stable and parseable;
        // re-use it for the banner file rather than duplicating the format.
        return SwiftModuleSourcePrinter.render(info);
    }

    /**
     * True when {@code decl}'s parent {@code DeclContextID} points at
     * another real local decl in this module — i.e. {@code decl} is a
     * member nested inside something else. Such decls render inline via
     * the parent's MEMBERS body and don't need a top-level file.
     *
     * <p>A {@code 0} or unknown parent context means top-level (module
     * scope). A parent that resolves to a synthetic context record
     * (PATTERN_BINDING_INITIALIZER_CONTEXT, etc.) is also treated as
     * top-level — those are expression-level scopes, not nominal parents.
     */
    private static boolean hasLocalDeclParent(SwiftModuleInfo info, SwiftDeclEntry decl) {
        int parentId = decl.parentContextId();
        if (parentId <= 0) {
            return false;
        }
        SwiftDeclEntry parent = info.lookupDecl(parentId);
        if (parent == null || parent.isExternal()) {
            return false;
        }
        // If the parent's record kind isn't a real decl, treat as top-level
        // — synthetic contexts (closures, pattern bindings) shouldn't pull
        // their inner decls out of the file root.
        return SwiftDeclRecord.isDecl(parent.recordKind());
    }

    /**
     * Pick a file name for a top-level decl. Uses the decl's identifier
     * when it's a clean Swift identifier; falls back to a synthesized name
     * (kind + declID) for anonymous / mangled cases so the archive map
     * never has an empty key.
     */
    private String pickFileName(SwiftDeclEntry decl) {
        String name = info.lookupDeclName(decl.declId());
        if (name != null && !name.isEmpty() && isFileNameSafe(name)) {
            return name;
        }
        // EXTENSION_DECL etc. don't have an identifier — synthesize one.
        return SwiftDeclRecord.name(decl.recordKind()) + "_" + decl.declId();
    }

    /**
     * Most filesystems reject {@code /}, {@code \}, {@code :}, {@code *}
     * and friends; Swift identifiers can legitimately contain none of
     * those, but synthesized internal names (with spaces, parentheses,
     * etc.) need filtering. Keep ASCII letters/digits/underscores +
     * dots so the result is unambiguously a single-segment filename.
     */
    private static boolean isFileNameSafe(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return !s.isEmpty();
    }
}
