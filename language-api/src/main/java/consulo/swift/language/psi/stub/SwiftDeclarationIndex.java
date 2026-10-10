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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.swift.language.psi.SwiftDeclaration;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lookups in the Swift declaration indexes.
 */
public final class SwiftDeclarationIndex {
    private SwiftDeclarationIndex() {
    }

    /**
     * class, struct, enum, protocol, actor, typealias and associatedtype declarations named {@code name}
     */
    @RequiredReadAction
    public static Collection<SwiftDeclaration> findTypes(String name, Project project, GlobalSearchScope scope) {
        return find(SwiftDeclarationKind.TYPE, name, project, scope);
    }

    /**
     * extensions of types with the simple name {@code name}
     */
    @RequiredReadAction
    public static Collection<SwiftDeclaration> findExtensions(String name, Project project, GlobalSearchScope scope) {
        return find(SwiftDeclarationKind.EXTENSION, name, project, scope);
    }

    /**
     * functions named {@code name} (without the argument labels), top-level and members
     */
    @RequiredReadAction
    public static Collection<SwiftDeclaration> findFunctions(String name, Project project, GlobalSearchScope scope) {
        return find(SwiftDeclarationKind.FUNCTION, name, project, scope);
    }

    /**
     * functions named {@code name} declared in a type or in an extension of a type with the simple name {@code typeName}
     */
    @RequiredReadAction
    public static List<SwiftDeclaration> findFunctionsOf(String typeName, String name, Project project, GlobalSearchScope scope) {
        List<SwiftDeclaration> result = new ArrayList<>();
        for (SwiftDeclaration function : findFunctions(name, project, scope)) {
            if (typeName.equals(getContainerName(function))) {
                result.add(function);
            }
        }
        return result;
    }

    /**
     * @return the name of the type or extension declaring {@code declaration}, {@code null} for top-level ones
     */
    @RequiredReadAction
    public static @Nullable String getContainerName(SwiftDeclaration declaration) {
        SwiftDeclaration container = PsiTreeUtil.getStubOrPsiParentOfType(declaration, SwiftDeclaration.class);
        return container == null ? null : container.getName();
    }

    @RequiredReadAction
    public static Collection<SwiftDeclaration> find(SwiftDeclarationKind kind, String name, Project project, GlobalSearchScope scope) {
        StubIndexKey<String, SwiftDeclaration> indexKey = kind.getIndexKey();
        return indexKey == null ? List.of() : StubIndex.getElements(indexKey, name, project, scope, SwiftDeclaration.class);
    }

    public static Collection<String> getAllNames(SwiftDeclarationKind kind, Project project) {
        StubIndexKey<String, SwiftDeclaration> indexKey = kind.getIndexKey();
        return indexKey == null ? List.of() : StubIndex.getInstance().getAllKeys(indexKey, project);
    }
}
