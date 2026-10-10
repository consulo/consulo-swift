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
package consulo.swift.execution.debugger;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.nativeDev.debugger.NativeDebuggerLanguageSupport;
import consulo.project.Project;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationIndex;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Swift expressions in the evaluate/watch editors of a native debug session.
 */
@ExtensionImpl
public class SwiftNativeDebuggerLanguageSupport implements NativeDebuggerLanguageSupport {
    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    @Override
    public String getDebuggerLanguage() {
        return "swift";
    }

    @Override
    public PsiFile createExpressionCodeFragment(Project project, String text, @Nullable PsiElement context, boolean isPhysical) {
        return PsiFileFactory.getInstance(project).createFileFromText("fragment.swift", SwiftLanguage.INSTANCE, text, isPhysical, false);
    }

    /**
     * LLDB names types with their module and generic arguments - {@code Demo.Box<Swift.Int>} is {@code Box}.
     */
    @RequiredReadAction
    @Override
    public @Nullable PsiElement findTypeDeclaration(Project project, PsiElement context, String typeName) {
        String name = typeName.trim();
        int generics = name.indexOf('<');
        if (generics >= 0) {
            name = name.substring(0, generics);
        }
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        if (name.isEmpty()) {
            return null;
        }
        Collection<SwiftDeclaration> types = SwiftDeclarationIndex.findTypes(name, project, GlobalSearchScope.allScope(project));
        return types.isEmpty() ? null : types.iterator().next();
    }
}
