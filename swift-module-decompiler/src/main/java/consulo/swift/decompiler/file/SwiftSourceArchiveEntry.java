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

import java.nio.charset.StandardCharsets;

/**
 * Synthesized {@code .swift} file inside the archive view. The text is
 * fully materialized at construction time (one rendered top-level decl
 * per entry, or the module banner for the root entry); we hand callers
 * a {@code byte[]} so multiple readers can stream the same content
 * without re-running the printer.
 *
 * <p>UTF-8 encoding is fixed — matches how the rest of the IDE reads
 * {@code .swift} files on disk.
 */
public final class SwiftSourceArchiveEntry implements SwiftArchiveEntry {

    private final String path;
    private final byte[] content;
    private final long lastModified;

    public SwiftSourceArchiveEntry(String path, String source, long lastModified) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must be non-empty");
        }
        if (source == null) {
            throw new NullPointerException("source");
        }
        this.path = path;
        this.content = source.getBytes(StandardCharsets.UTF_8);
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
        return content.length;
    }

    /**
     * Raw UTF-8 bytes of the synthesized Swift text. Returns the internal
     * array — callers must treat it as read-only.
     */
    public byte[] getContent() {
        return content;
    }

    /** Convenience: the rendered text as a Swift {@link String}. */
    public String getSource() {
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "SwiftSourceArchiveEntry{" + path + ", " + content.length + " bytes}";
    }
}
