/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module-name → {@link SwiftModuleInfo} resolver backed by a
 * {@link SwiftModuleSearchPath}. Caches each module's parsed
 * {@code SwiftModuleInfo} so transitive XREFs into the same dependency
 * (e.g. dozens of references to {@code Swift.Int}) don't reparse the
 * 30 MiB stdlib binary every time.
 *
 * <p>The pattern mirrors {@code swift-frontend}'s own {@code ModuleLoader} —
 * a thin lookup cache on top of search-path resolution. We don't go as
 * far as following transitive imports recursively (yet); callers ask for
 * a module by name and get its top-level info or {@code null}.
 *
 * <p>Thread-safe — the cache uses {@link ConcurrentHashMap} so IDE-side
 * features that resolve cross-module references off the EDT see a single
 * shared cache.
 */
public final class SwiftModuleResolver {

    /** Sentinel used in the cache when a module name resolved to nothing. */
    private static final SwiftModuleInfo NOT_FOUND = new SwiftModuleInfo();

    private final SwiftModuleSearchPath searchPath;
    private final Map<String, SwiftModuleInfo> cache = new ConcurrentHashMap<>();
    private final Map<String, Path> pathCache = new HashMap<>();

    public SwiftModuleResolver(SwiftModuleSearchPath searchPath) {
        if (searchPath == null) {
            throw new NullPointerException("searchPath");
        }
        this.searchPath = searchPath;
    }

    /** Build a resolver against the standard system Swift toolchain. */
    public static SwiftModuleResolver system() {
        return new SwiftModuleResolver(SwiftModuleSearchPath.system());
    }

    public SwiftModuleSearchPath getSearchPath() {
        return searchPath;
    }

    /**
     * Locate the binary file for {@code moduleName}, or {@code null}
     * when no search-path entry has it. Cached.
     */
    public synchronized Path resolvePath(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        if (pathCache.containsKey(moduleName)) {
            return pathCache.get(moduleName);
        }
        Path p = searchPath.resolve(moduleName);
        pathCache.put(moduleName, p);
        return p;
    }

    /**
     * Parse and return the {@link SwiftModuleInfo} for {@code moduleName},
     * or {@code null} when no .swiftmodule matches on the search path
     * (or it failed to open). Repeated calls return the same
     * {@code SwiftModuleInfo} instance.
     */
    public SwiftModuleInfo resolve(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        SwiftModuleInfo cached = cache.get(moduleName);
        if (cached != null) {
            return cached == NOT_FOUND ? null : cached;
        }
        Path file = resolvePath(moduleName);
        if (file == null) {
            cache.put(moduleName, NOT_FOUND);
            return null;
        }
        try {
            SwiftModuleReader reader = SwiftModuleReader.open(file);
            SwiftModuleInfo info = reader.readModuleInfo();
            cache.put(moduleName, info);
            return info;
        }
        catch (IOException e) {
            cache.put(moduleName, NOT_FOUND);
            // Per-caller decision: throw or return null. We swallow IO
            // errors here because the IDE-side callers should treat a
            // broken sibling .swiftmodule the same as a missing one —
            // best-effort enrichment, never blocking the main module's
            // decompile.
            return null;
        }
    }

    /**
     * Convenience: open the underlying {@link SwiftModuleReader} for
     * {@code moduleName} without caching the {@link SwiftModuleInfo}.
     * Useful when callers want to feed the bytes through
     * {@link SwiftModuleArchiveFile#open(SwiftModuleReader, long)} for
     * dependency-stub generation.
     */
    public SwiftModuleReader openReader(String moduleName) throws IOException {
        Path file = resolvePath(moduleName);
        if (file == null) {
            throw new IOException("Module not on search path: " + moduleName);
        }
        return SwiftModuleReader.open(file);
    }

    /**
     * Force-eject the cached info for {@code moduleName}. Mostly useful
     * for tests; production code shouldn't need to evict since
     * {@code .swiftmodule} content doesn't change at runtime.
     */
    public void invalidate(String moduleName) {
        cache.remove(moduleName);
        pathCache.remove(moduleName);
    }

    public int cachedModuleCount() {
        // Don't count NOT_FOUND sentinels — those reflect resolution
        // failures, not actual loaded modules.
        int n = 0;
        for (SwiftModuleInfo v : cache.values()) {
            if (v != NOT_FOUND) n++;
        }
        return n;
    }

}
