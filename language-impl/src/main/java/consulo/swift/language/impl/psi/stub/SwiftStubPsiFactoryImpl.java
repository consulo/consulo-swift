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

package consulo.swift.language.impl.psi.stub;

import consulo.annotation.component.ServiceImpl;
import consulo.language.ast.IElementType;
import consulo.language.psi.stub.IStubElementType;
import consulo.swift.language.impl.psi.SwiftActorDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftClassDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftEnumDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftExtensionDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftFunctionDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftProtocolAssociatedTypeDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftProtocolDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftProtocolMethodDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftStructDeclarationImpl;
import consulo.swift.language.impl.psi.SwiftTypealiasDeclarationImpl;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.SwiftTypes;
import consulo.swift.language.psi.stub.SwiftDeclarationStub;
import consulo.swift.language.psi.stub.SwiftStubPsiFactory;
import jakarta.inject.Singleton;

/**
 * PSI of the declarations that get stubs - see {@code SwiftStubElementTypeFactory}.
 */
@ServiceImpl
@Singleton
public class SwiftStubPsiFactoryImpl implements SwiftStubPsiFactory {
    @Override
    public SwiftDeclaration createPsi(SwiftDeclarationStub stub, IStubElementType<?, ?> elementType) {
        IElementType type = elementType;
        if (type == SwiftTypes.CLASS_DECLARATION) {
            return new SwiftClassDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.STRUCT_DECLARATION) {
            return new SwiftStructDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.ENUM_DECLARATION) {
            return new SwiftEnumDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.PROTOCOL_DECLARATION) {
            return new SwiftProtocolDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.ACTOR_DECLARATION) {
            return new SwiftActorDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.TYPEALIAS_DECLARATION) {
            return new SwiftTypealiasDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.PROTOCOL_ASSOCIATED_TYPE_DECLARATION) {
            return new SwiftProtocolAssociatedTypeDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.EXTENSION_DECLARATION) {
            return new SwiftExtensionDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.FUNCTION_DECLARATION) {
            return new SwiftFunctionDeclarationImpl(stub, elementType);
        }
        if (type == SwiftTypes.PROTOCOL_METHOD_DECLARATION) {
            return new SwiftProtocolMethodDeclarationImpl(stub, elementType);
        }
        throw new IllegalArgumentException("No stub PSI for " + elementType);
    }
}
