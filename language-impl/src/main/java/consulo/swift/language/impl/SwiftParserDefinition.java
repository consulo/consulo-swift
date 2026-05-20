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

package consulo.swift.language.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import consulo.swift.language.SwiftFileElementTypes;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.SwiftTokenSets;
import consulo.swift.language.impl.psi.SwiftFileImpl;
import consulo.swift.language.impl.psi.SwiftTypesFactory;
import consulo.swift.language.impl.syntax.SwiftSyntaxParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class SwiftParserDefinition implements ParserDefinition {
    @Nonnull
    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new SwiftLexer();
    }

    @Nonnull
    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return new SwiftSyntaxParser();
    }

    @Nonnull
    @Override
    public IFileElementType getFileNodeType() {
        return SwiftFileElementTypes.SWIFT_FILE;
    }

    @Nonnull
    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return SwiftTokenSets.COMMENTS;
    }

    @Nonnull
    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return SwiftTokenSets.STRING_LITERALS;
    }

    @Nonnull
    @Override
    public PsiElement createElement(@Nonnull ASTNode node) {
        return SwiftTypesFactory.createElement(node);
    }

    @Nonnull
    @Override
    public PsiFile createFile(@Nonnull FileViewProvider viewProvider) {
        return new SwiftFileImpl(viewProvider);
    }

    @Nonnull
    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(@Nullable ASTNode left, @Nullable ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
