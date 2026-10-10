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

package consulo.swift.language.editor.impl.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigation.GotoClassOrTypeContributor;
import consulo.language.psi.PsiElement;
import consulo.navigation.NavigationItem;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationKind;
import org.jspecify.annotations.Nullable;

/**
 * Go to Class: classes, structs, enums, protocols, actors and type aliases.
 */
@ExtensionImpl
public class SwiftGotoClassContributor extends SwiftGotoContributorBase implements GotoClassOrTypeContributor {
    public SwiftGotoClassContributor() {
        super(SwiftDeclarationKind.TYPE);
    }

    /**
     * {@code Outer.Inner} for nested types.
     */
    @RequiredReadAction
    @Override
    public @Nullable String getQualifiedName(NavigationItem item) {
        if (!(item instanceof SwiftDeclaration declaration)) {
            return null;
        }
        StringBuilder builder = new StringBuilder(String.valueOf(declaration.getName()));
        for (PsiElement parent = declaration.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof SwiftDeclaration container && container.getName() != null) {
                builder.insert(0, container.getName() + ".");
            }
        }
        return builder.toString();
    }

    @Override
    public String getQualifiedNameSeparator() {
        return ".";
    }
}
