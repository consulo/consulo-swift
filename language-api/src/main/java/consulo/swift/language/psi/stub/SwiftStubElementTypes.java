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
import consulo.swift.language.SwiftFileElementTypes;
import consulo.swift.language.psi.SwiftTypes;

/**
 * Every stub element type of Swift - registered through {@link SwiftStubElementTypeHolder}. A declaration added to the
 * {@code elementTypeFactory} of the grammar has to be added here too.
 */
public interface SwiftStubElementTypes {
    IElementType FILE = SwiftFileElementTypes.SWIFT_FILE;

    IElementType CLASS_DECLARATION = SwiftTypes.CLASS_DECLARATION;
    IElementType STRUCT_DECLARATION = SwiftTypes.STRUCT_DECLARATION;
    IElementType ENUM_DECLARATION = SwiftTypes.ENUM_DECLARATION;
    IElementType PROTOCOL_DECLARATION = SwiftTypes.PROTOCOL_DECLARATION;
    IElementType ACTOR_DECLARATION = SwiftTypes.ACTOR_DECLARATION;
    IElementType TYPEALIAS_DECLARATION = SwiftTypes.TYPEALIAS_DECLARATION;
    IElementType PROTOCOL_ASSOCIATED_TYPE_DECLARATION = SwiftTypes.PROTOCOL_ASSOCIATED_TYPE_DECLARATION;
    IElementType EXTENSION_DECLARATION = SwiftTypes.EXTENSION_DECLARATION;
    IElementType FUNCTION_DECLARATION = SwiftTypes.FUNCTION_DECLARATION;
    IElementType PROTOCOL_METHOD_DECLARATION = SwiftTypes.PROTOCOL_METHOD_DECLARATION;
}
