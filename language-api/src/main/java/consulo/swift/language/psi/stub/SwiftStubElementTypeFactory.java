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

import consulo.language.ast.IElementType;

/**
 * {@code elementTypeFactory} of the grammar for all declarations.
 */
public final class SwiftStubElementTypeFactory {
    private SwiftStubElementTypeFactory() {
    }

    public static IElementType create(String name) {
        SwiftDeclarationKind kind = switch (name) {
            case "CLASS_DECLARATION", "STRUCT_DECLARATION", "ENUM_DECLARATION", "PROTOCOL_DECLARATION", "ACTOR_DECLARATION",
                 "TYPEALIAS_DECLARATION", "PROTOCOL_ASSOCIATED_TYPE_DECLARATION" -> SwiftDeclarationKind.TYPE;
            case "EXTENSION_DECLARATION" -> SwiftDeclarationKind.EXTENSION;
            case "FUNCTION_DECLARATION", "PROTOCOL_METHOD_DECLARATION" -> SwiftDeclarationKind.FUNCTION;
            default -> SwiftDeclarationKind.NONE;
        };
        return new SwiftDeclarationStubElementType(name, kind);
    }
}
