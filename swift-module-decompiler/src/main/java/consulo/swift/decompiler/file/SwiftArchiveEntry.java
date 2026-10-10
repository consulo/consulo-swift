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
 * Common base for entries that live inside a virtual archive view of a
 * {@code .swiftmodule} file. Subtypes are
 * {@link SwiftSourceArchiveEntry} (a synthesized {@code .swift} file) and
 * {@link SwiftDirArchiveEntry} (an intermediate directory in the tree).
 *
 * <p>Modelled after {@code consulo.internal.dotnet.msil.decompiler.file.DotNetArchiveEntry}
 * so downstream IDE wiring (virtual-file-system, project view, structure
 * view) can plug in with a familiar shape: a flat map keyed by archive
 * path ({@code "Sources/Package.swift"}) with one {@code SwiftArchiveEntry}
 * per slot.
 */
public sealed interface SwiftArchiveEntry permits SwiftSourceArchiveEntry, SwiftDirArchiveEntry {

    /**
     * Path of this entry relative to the archive root, using {@code "/"}
     * as separator. Directory entries always end with {@code "/"}.
     */
    String getPath();

    /** Last-modified timestamp inherited from the underlying {@code .swiftmodule} file. */
    long getLastModified();

    /** Size in bytes of this entry's payload (0 for directories). */
    long getSize();

    /** Convenience: is this a directory? */
    default boolean isDirectory() {
        return this instanceof SwiftDirArchiveEntry;
    }
}
