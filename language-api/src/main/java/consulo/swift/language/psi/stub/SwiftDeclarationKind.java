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

package consulo.swift.language.psi.stub;

import consulo.language.psi.stub.StubIndexKey;
import consulo.swift.language.psi.SwiftDeclaration;
import org.jspecify.annotations.Nullable;

/**
 * Which index a declaration goes to, if any.
 */
public enum SwiftDeclarationKind {
    /**
     * class, struct, enum, protocol, actor, typealias, associatedtype
     */
    TYPE,
    /**
     * extension - indexed by the simple name of the extended type
     */
    EXTENSION,
    /**
     * func, also of protocols
     */
    FUNCTION,
    /**
     * every other declaration - no stub
     */
    NONE;

    /**
     * Resolved on use: the element types (and so this enum) are created with {@code SwiftTypes}, also where there are no
     * indexes - in the lexer, in parser tests - and creating an index key needs them.
     */
    public @Nullable StubIndexKey<String, SwiftDeclaration> getIndexKey() {
        return switch (this) {
            case TYPE -> SwiftStubIndexKeys.TYPE_NAME;
            case EXTENSION -> SwiftStubIndexKeys.EXTENSION_TYPE_NAME;
            case FUNCTION -> SwiftStubIndexKeys.FUNCTION_NAME;
            case NONE -> null;
        };
    }
}
