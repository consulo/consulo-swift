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
package consulo.swift.execution.profiler;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.profiler.model.NativeCall;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.nativeDev.profiler.NavigatableSymbolSearcher;
import consulo.project.Project;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * From a profiler frame to the Swift function - or, for initializers and accessors, the type - it was in.
 */
@ExtensionImpl
public class SwiftNavigatableSymbolSearcher implements NavigatableSymbolSearcher {
    @RequiredReadAction
    @Override
    public NavigatablePsiElement[] findNavigatableSymbols(NativeCall call, Project project) {
        String symbol = call.getClassName().isEmpty() ? call.getMethodOrFunction() : call.getClassName() + "." + call.getMethodOrFunction();
        SwiftSymbolName name = SwiftSymbolName.parse(symbol);
        if (name == null) {
            return NavigatablePsiElement.EMPTY_ARRAY;
        }

        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        List<SwiftDeclaration> result = new ArrayList<>();
        String typeName = name.typeName();
        String memberName = name.memberName();
        if (name.function() && memberName != null) {
            Collection<SwiftDeclaration> functions = typeName == null
                ? SwiftDeclarationIndex.findFunctions(memberName, project, scope)
                : SwiftDeclarationIndex.findFunctionsOf(typeName, memberName, project, scope);
            result.addAll(functions);
        }
        if (result.isEmpty() && typeName != null) {
            result.addAll(SwiftDeclarationIndex.findTypes(typeName, project, scope));
        }
        return result.toArray(NavigatablePsiElement.EMPTY_ARRAY);
    }
}
