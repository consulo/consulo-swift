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

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.impl.SwiftLexer;
import consulo.swift.language.psi.SwiftTypes;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import static consulo.codeEditor.DefaultLanguageHighlighterColors.*;

@ExtensionImpl
public class SwiftSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @Nonnull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return new SwiftSyntaxHighlighter();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    private static class SwiftSyntaxHighlighter extends SyntaxHighlighterBase {
        private static final Map<IElementType, TextAttributesKey> ourAttributes = new HashMap<>();

        static {
            // Keywords (declaration, statement, expression, modifiers)
            fillMap(ourAttributes, KEYWORD,
                SwiftTypes.IMPORT_KEYWORD, SwiftTypes.LET_KEYWORD, SwiftTypes.VAR_KEYWORD,
                SwiftTypes.TYPEALIAS_KEYWORD, SwiftTypes.FUNC_KEYWORD, SwiftTypes.ENUM_KEYWORD,
                SwiftTypes.STRUCT_KEYWORD, SwiftTypes.CLASS_KEYWORD, SwiftTypes.PROTOCOL_KEYWORD,
                SwiftTypes.INIT_KEYWORD, SwiftTypes.DEINIT_KEYWORD, SwiftTypes.EXTENSION_KEYWORD,
                SwiftTypes.SUBSCRIPT_KEYWORD, SwiftTypes.OPERATOR_KEYWORD,
                SwiftTypes.PRECEDENCEGROUP_KEYWORD, SwiftTypes.ASSOCIATEDTYPE_KEYWORD,
                SwiftTypes.INDIRECT_KEYWORD,
                SwiftTypes.IF_KEYWORD, SwiftTypes.ELSE_KEYWORD, SwiftTypes.GUARD_KEYWORD,
                SwiftTypes.SWITCH_KEYWORD, SwiftTypes.CASE_KEYWORD, SwiftTypes.DEFAULT_KEYWORD,
                SwiftTypes.FOR_KEYWORD, SwiftTypes.IN_KEYWORD, SwiftTypes.WHILE_KEYWORD,
                SwiftTypes.REPEAT_KEYWORD, SwiftTypes.DO_KEYWORD, SwiftTypes.DEFER_KEYWORD,
                SwiftTypes.BREAK_KEYWORD, SwiftTypes.CONTINUE_KEYWORD,
                SwiftTypes.FALLTHROUGH_KEYWORD, SwiftTypes.RETURN_KEYWORD,
                SwiftTypes.THROW_KEYWORD, SwiftTypes.TRY_KEYWORD, SwiftTypes.CATCH_KEYWORD,
                SwiftTypes.WHERE_KEYWORD, SwiftTypes.AS_KEYWORD, SwiftTypes.IS_KEYWORD,
                SwiftTypes.SELF_KEYWORD, SwiftTypes.SELF_TYPE_KEYWORD, SwiftTypes.SUPER_KEYWORD,
                SwiftTypes.NIL_KEYWORD, SwiftTypes.TRUE_KEYWORD, SwiftTypes.FALSE_KEYWORD,
                SwiftTypes.THROWS_KEYWORD, SwiftTypes.RETHROWS_KEYWORD, SwiftTypes.INOUT_KEYWORD,
                SwiftTypes.ANY_KEYWORD, SwiftTypes.TYPE_TYPE_KEYWORD, SwiftTypes.PROTOCOL_TYPE_KEYWORD,
                SwiftTypes.PRIVATE_KEYWORD, SwiftTypes.FILEPRIVATE_KEYWORD,
                SwiftTypes.INTERNAL_KEYWORD, SwiftTypes.PUBLIC_KEYWORD, SwiftTypes.OPEN_KEYWORD,
                SwiftTypes.STATIC_KEYWORD, SwiftTypes.FINAL_KEYWORD, SwiftTypes.LAZY_KEYWORD,
                SwiftTypes.WEAK_KEYWORD, SwiftTypes.UNOWNED_KEYWORD,
                SwiftTypes.MUTATING_KEYWORD, SwiftTypes.NONMUTATING_KEYWORD,
                SwiftTypes.OVERRIDE_KEYWORD, SwiftTypes.REQUIRED_KEYWORD,
                SwiftTypes.OPTIONAL_KEYWORD, SwiftTypes.DYNAMIC_KEYWORD,
                SwiftTypes.CONVENIENCE_KEYWORD, SwiftTypes.PREFIX_KEYWORD,
                SwiftTypes.INFIX_KEYWORD, SwiftTypes.POSTFIX_KEYWORD);

            fillMap(ourAttributes, METADATA,
                SwiftTypes.HASH_IF, SwiftTypes.HASH_ELSEIF, SwiftTypes.HASH_ELSE,
                SwiftTypes.HASH_ENDIF, SwiftTypes.HASH_SOURCELOCATION,
                SwiftTypes.HASH_AVAILABLE, SwiftTypes.HASH_SELECTOR, SwiftTypes.HASH_KEYPATH,
                SwiftTypes.HASH_FILE, SwiftTypes.HASH_LINE, SwiftTypes.HASH_COLUMN,
                SwiftTypes.HASH_FUNCTION, SwiftTypes.HASH_DSOHANDLE,
                SwiftTypes.HASH_COLOR_LITERAL, SwiftTypes.HASH_FILE_LITERAL,
                SwiftTypes.HASH_IMAGE_LITERAL);

            fillMap(ourAttributes, NUMBER,
                SwiftTypes.BINARY_LITERAL, SwiftTypes.OCTAL_LITERAL,
                SwiftTypes.HEXADECIMAL_LITERAL, SwiftTypes.DECIMAL_LITERAL,
                SwiftTypes.PURE_DECIMAL_DIGITS, SwiftTypes.FLOATING_POINT_LITERAL);

            fillMap(ourAttributes, STRING,
                SwiftTypes.STATIC_STRING_LITERAL, SwiftTypes.INTERPOLATED_STRING_LITERAL);

            fillMap(ourAttributes, LINE_COMMENT, SwiftTypes.LINE_COMMENT);
            fillMap(ourAttributes, BLOCK_COMMENT, SwiftTypes.BLOCK_COMMENT);

            fillMap(ourAttributes, OPERATION_SIGN,
                SwiftTypes.OPERATOR, SwiftTypes.DOT_OPERATOR,
                SwiftTypes.EQ, SwiftTypes.ARROW, SwiftTypes.LT, SwiftTypes.GT,
                SwiftTypes.QUESTION, SwiftTypes.EXCL, SwiftTypes.AMP,
                SwiftTypes.ELLIPSIS, SwiftTypes.HALF_OPEN_RANGE);

            fillMap(ourAttributes, BRACES, SwiftTypes.LBRACE, SwiftTypes.RBRACE);
            fillMap(ourAttributes, PARENTHESES, SwiftTypes.LPAREN, SwiftTypes.RPAREN);
            fillMap(ourAttributes, BRACKETS, SwiftTypes.LBRACK, SwiftTypes.RBRACK);
            fillMap(ourAttributes, COMMA, SwiftTypes.COMMA);
            fillMap(ourAttributes, SEMICOLON, SwiftTypes.SEMI);
            fillMap(ourAttributes, DOT, SwiftTypes.DOT);

            fillMap(ourAttributes, HighlighterColors.BAD_CHARACTER, TokenType.BAD_CHARACTER);
        }

        @Nonnull
        @Override
        public Lexer getHighlightingLexer() {
            return new SwiftLexer();
        }

        @Nonnull
        @Override
        public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
            return pack(ourAttributes.get(tokenType));
        }
    }
}
