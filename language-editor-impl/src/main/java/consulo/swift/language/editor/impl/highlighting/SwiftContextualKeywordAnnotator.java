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
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.swift.language.SwiftContextualKeywordElementType;
import consulo.swift.language.SwiftLanguage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Applies keyword highlighting to Swift contextual keywords AFTER the parser
 * remaps them. The lexer emits these tokens as {@code IDENTIFIER}, so a
 * lexer-driven {@code SyntaxHighlighter} can't distinguish them; the parser
 * remap (via {@code SwiftParserUtil}) yields PSI leaves whose element type
 * is {@link SwiftContextualKeywordElementType}, which this annotator detects
 * and re-colors as a keyword.
 */
@ExtensionImpl
public class SwiftContextualKeywordAnnotator implements AnnotatorFactory, DumbAware {
    @Nonnull
    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    @Nullable
    @Override
    public Annotator createAnnotator() {
        return new ContextualKeywordAnnotator();
    }

    private static class ContextualKeywordAnnotator implements Annotator {
        @RequiredReadAction
        @Override
        public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
            ASTNode node = element.getNode();
            if (node != null && node.getElementType() instanceof SwiftContextualKeywordElementType) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
                    .create();
            }
        }
    }
}
