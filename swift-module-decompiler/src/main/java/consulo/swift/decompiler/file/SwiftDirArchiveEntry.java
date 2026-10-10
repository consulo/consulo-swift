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

/**
 * Directory entry inside the synthesized archive — purely structural,
 * carries no payload. We emit one of these per intermediate slash in the
 * source-tree we generate ({@code "Sources/"}, {@code "Sources/Models/"})
 * so the virtual-file-system layer can walk a real tree rather than a
 * flat list.
 */
public final class SwiftDirArchiveEntry implements SwiftArchiveEntry {

    private final String path;
    private final long lastModified;

    public SwiftDirArchiveEntry(String path, long lastModified) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must be non-empty");
        }
        // Directory paths are conventionally suffixed with "/" — normalize
        // so callers don't have to remember.
        this.path = path.endsWith("/") ? path : path + "/";
        this.lastModified = lastModified;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public long getSize() {
        return 0L;
    }

    @Override
    public String toString() {
        return "SwiftDirArchiveEntry{" + path + '}';
    }
}
