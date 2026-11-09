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
package consulo.swift.language.editor.impl.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.impl.psi.controlFlow.SwiftControlFlow;
import consulo.swift.language.impl.psi.controlFlow.SwiftFlowDiagnostics;
import org.jspecify.annotations.Nullable;

/**
 * The flow diagnostics of swiftc ({@link SwiftFlowDiagnostics}) - computed once per flow owner, when it is annotated
 *
 * @author VISTALL
 */
@ExtensionImpl
public class SwiftFlowAnnotator implements AnnotatorFactory {
    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    @Override
    public @Nullable Annotator createAnnotator() {
        return new FlowAnnotator();
    }

    private static class FlowAnnotator implements Annotator {
        @RequiredReadAction
        @Override
        public void annotate(PsiElement element, AnnotationHolder holder) {
            if (!SwiftControlFlow.isOwner(element)) {
                return;
            }
            for (SwiftFlowDiagnostics.Problem problem : SwiftFlowDiagnostics.of(element)) {
                holder.newAnnotation(problem.error() ? HighlightSeverity.ERROR : HighlightSeverity.WARNING, problem.message())
                    .range(problem.range())
                    .create();
            }
        }
    }
}
