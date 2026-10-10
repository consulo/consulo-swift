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

package consulo.swift.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.util.IncorrectOperationException;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.SwiftNamedElement;
import consulo.swift.language.psi.SwiftTypes;
import consulo.swift.language.psi.stub.SwiftDeclarationStub;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Base of all declaration PSI: stub-based, named by {@link SwiftDeclarationNames}.
 */
public abstract class SwiftStubbedElementImpl<S extends SwiftDeclarationStub> extends StubBasedPsiElementBase<S> implements SwiftNamedElement {
    public SwiftStubbedElementImpl(ASTNode node) {
        super(node);
    }

    public SwiftStubbedElementImpl(S stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @RequiredReadAction
    @Override
    public @Nullable String getName() {
        S stub = getGreenStub();
        if (stub != null) {
            return stub.getName();
        }
        return SwiftDeclarationNames.getName(getNode());
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNameIdentifier() {
        ASTNode nameNode = SwiftDeclarationNames.findNameNode(getNode());
        return nameNode == null ? null : nameNode.getPsi();
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Renaming Swift declarations is not supported yet");
    }

    @RequiredReadAction
    @Override
    public int getTextOffset() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getTextOffset() : super.getTextOffset();
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @RequiredReadAction
            @Override
            public @Nullable String getPresentableText() {
                return getName();
            }

            @RequiredReadAction
            @Override
            public @Nullable String getLocationString() {
                SwiftDeclaration container = getStubOrPsiParentOfType(SwiftDeclaration.class);
                String containerName = container == null ? null : container.getName();
                if (containerName != null) {
                    return containerName;
                }
                PsiFile file = getContainingFile();
                return file == null ? null : file.getName();
            }

            @Override
            public @Nullable Image getIcon() {
                return iconOf(getElementType());
            }
        };
    }

    private static Image iconOf(IElementType type) {
        if (type == SwiftTypes.PROTOCOL_DECLARATION) {
            return PlatformIconGroup.nodesInterface();
        }
        if (type == SwiftTypes.ENUM_DECLARATION) {
            return PlatformIconGroup.nodesEnum();
        }
        if (type == SwiftTypes.FUNCTION_DECLARATION || type == SwiftTypes.PROTOCOL_METHOD_DECLARATION) {
            return PlatformIconGroup.nodesFunction();
        }
        if (type == SwiftTypes.VARIABLE_DECLARATION || type == SwiftTypes.CONSTANT_DECLARATION
            || type == SwiftTypes.PROTOCOL_PROPERTY_DECLARATION) {
            return PlatformIconGroup.nodesProperty();
        }
        return PlatformIconGroup.nodesClass();
    }

    /**
     * Kept in the form of {@code SwiftElementImpl} - the parser tests compare PSI dumps.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getElementType() + ")";
    }
}
