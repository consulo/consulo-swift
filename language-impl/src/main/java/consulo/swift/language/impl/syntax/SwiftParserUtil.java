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

package consulo.swift.language.impl.syntax;

import consulo.language.ast.IElementType;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.language.parser.PsiBuilder;
import consulo.swift.language.psi.SwiftTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom parser utility for Swift contextual keywords.
 * <p>
 * Many Swift modifier, accessor, precedence-group, and type-position names —
 * {@code some}, {@code final}, {@code lazy}, {@code mutating}, {@code get},
 * {@code set}, {@code willSet}, {@code didSet}, {@code left}, {@code right},
 * {@code none}, {@code associativity}, {@code higherThan}, {@code indirect},
 * etc. — are emitted as {@link SwiftTypes#IDENTIFIER} by the lexer. This util
 * overrides {@code consumeToken} / {@code nextTokenIs} so the parser can match
 * them as their dedicated keyword token (e.g. {@link SwiftTypes#SOME_KEYWORD})
 * only when the grammar explicitly expects it. The same text remains usable
 * as a plain identifier in every other position
 * ({@code let some = 5}, {@code func lazy() {}}, {@code var get: Int = 0}).
 */
public class SwiftParserUtil extends GeneratedParserUtilBase {
    private static final Map<IElementType, String> CONTEXTUAL_KEYWORDS = new HashMap<>();
    private static final Set<IElementType> CONTEXTUAL_KEYWORD_SET = new HashSet<>();

    static {
        // Type-position keyword (Swift 5.1+).
        register(SwiftTypes.SOME_KEYWORD, "some");

        // Declaration modifiers.
        register(SwiftTypes.FINAL_KEYWORD,       "final");
        register(SwiftTypes.LAZY_KEYWORD,        "lazy");
        register(SwiftTypes.WEAK_KEYWORD,        "weak");
        register(SwiftTypes.UNOWNED_KEYWORD,     "unowned");
        register(SwiftTypes.MUTATING_KEYWORD,    "mutating");
        register(SwiftTypes.NONMUTATING_KEYWORD, "nonmutating");
        register(SwiftTypes.OVERRIDE_KEYWORD,    "override");
        register(SwiftTypes.REQUIRED_KEYWORD,    "required");
        register(SwiftTypes.OPTIONAL_KEYWORD,    "optional");
        register(SwiftTypes.DYNAMIC_KEYWORD,     "dynamic");
        register(SwiftTypes.CONVENIENCE_KEYWORD, "convenience");
        register(SwiftTypes.PREFIX_KEYWORD,      "prefix");
        register(SwiftTypes.INFIX_KEYWORD,       "infix");
        register(SwiftTypes.POSTFIX_KEYWORD,     "postfix");
        register(SwiftTypes.INDIRECT_KEYWORD,    "indirect");

        // Property accessors.
        register(SwiftTypes.GET_KEYWORD,     "get");
        register(SwiftTypes.SET_KEYWORD,     "set");
        register(SwiftTypes.WILLSET_KEYWORD, "willSet");
        register(SwiftTypes.DIDSET_KEYWORD,  "didSet");

        // Precedence-group body keywords.
        register(SwiftTypes.LEFT_KEYWORD,          "left");
        register(SwiftTypes.RIGHT_KEYWORD,         "right");
        register(SwiftTypes.NONE_KEYWORD,          "none");
        register(SwiftTypes.ASSOCIATIVITY_KEYWORD, "associativity");
        register(SwiftTypes.HIGHERTHAN_KEYWORD,    "higherThan");
        register(SwiftTypes.LOWERTHAN_KEYWORD,     "lowerThan");
        register(SwiftTypes.ASSIGNMENT_KEYWORD,    "assignment");
    }

    private static void register(IElementType token, String text) {
        CONTEXTUAL_KEYWORDS.put(token, text);
        CONTEXTUAL_KEYWORD_SET.add(token);
    }

    private static boolean isContextualKeyword(IElementType token) {
        return CONTEXTUAL_KEYWORDS.containsKey(token);
    }

    private static boolean isContextualKeywordMatch(PsiBuilder b, IElementType token) {
        String expectedText = CONTEXTUAL_KEYWORDS.get(token);
        if (expectedText == null) {
            return false;
        }
        IElementType current = b.getTokenType();
        if (current == token) {
            return true;  // already remapped on a previous attempt
        }
        return current == SwiftTypes.IDENTIFIER && expectedText.equals(b.getTokenText());
    }

    public static boolean consumeToken(PsiBuilder b, IElementType token) {
        // Parser expects a contextual keyword.
        if (isContextualKeyword(token)) {
            if (isContextualKeywordMatch(b, token)) {
                b.remapCurrentToken(token);
                b.advanceLexer();
                return true;
            }
            return false;
        }

        // Parser expects IDENTIFIER — also accept already-remapped contextual keywords.
        if (token == SwiftTypes.IDENTIFIER) {
            IElementType current = b.getTokenType();
            if (current == SwiftTypes.IDENTIFIER || CONTEXTUAL_KEYWORD_SET.contains(current)) {
                b.advanceLexer();
                return true;
            }
            return false;
        }

        return GeneratedParserUtilBase.consumeToken(b, token);
    }

    /**
     * {@link GeneratedParserUtilBase#consumeTokens} dispatches to its OWN
     * static {@code consumeToken} via invokestatic, bypassing the override
     * here. Reimplement so each token in the sequence goes through our
     * contextual-keyword-aware {@link #consumeToken(PsiBuilder, IElementType)}.
     */
    public static boolean consumeTokens(PsiBuilder b, int pin, IElementType... tokens) {
        boolean result = true;
        boolean pinned = false;
        for (int i = 0; i < tokens.length; i++) {
            if (pin > 0 && i == pin) {
                pinned = result;
            }
            if (result || pinned) {
                if (!consumeToken(b, tokens[i])) {
                    result = false;
                    if (!pinned) {
                        break;
                    }
                }
            }
        }
        return result || pinned;
    }

    public static boolean nextTokenIs(PsiBuilder b, IElementType token) {
        if (isContextualKeyword(token)) {
            return isContextualKeywordMatch(b, token);
        }
        if (token == SwiftTypes.IDENTIFIER) {
            IElementType current = b.getTokenType();
            return current == SwiftTypes.IDENTIFIER || CONTEXTUAL_KEYWORD_SET.contains(current);
        }
        return GeneratedParserUtilBase.nextTokenIs(b, token);
    }

    public static boolean nextTokenIs(PsiBuilder b, String frameName, IElementType... tokens) {
        IElementType current = b.getTokenType();
        for (IElementType token : tokens) {
            if (isContextualKeyword(token)) {
                if (isContextualKeywordMatch(b, token)) {
                    return true;
                }
            }
            else if (token == SwiftTypes.IDENTIFIER) {
                if (current == SwiftTypes.IDENTIFIER || CONTEXTUAL_KEYWORD_SET.contains(current)) {
                    return true;
                }
            }
            else if (current == token) {
                return true;
            }
        }
        return false;
    }
}
