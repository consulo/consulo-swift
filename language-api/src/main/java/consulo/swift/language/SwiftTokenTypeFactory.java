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

package consulo.swift.language;

import consulo.language.ast.IElementType;

import java.util.Set;

/**
 * Custom token-type factory wired into Grammar-Kit via the BNF
 * {@code tokenTypeFactory} attribute. Returns {@link SwiftContextualKeywordElementType}
 * instances for the subset of tokens that are Swift contextual keywords —
 * the lexer emits them as {@code IDENTIFIER} and {@code SwiftParserUtil}
 * remaps them on demand, so this marker class lets the annotator highlight
 * the remapped occurrences as keywords.
 */
public final class SwiftTokenTypeFactory {
    private static final Set<String> CONTEXTUAL_KEYWORDS = Set.of(
        // Type-position markers.
        "SOME_KEYWORD",
        "ANY_TYPE_KEYWORD",
        // Concurrency.
        "ASYNC_KEYWORD",
        "AWAIT_KEYWORD",
        "ACTOR_KEYWORD",
        "NONISOLATED_KEYWORD",
        "ISOLATED_KEYWORD",
        // Ownership / misc modern.
        "BORROWING_KEYWORD",
        "CONSUMING_KEYWORD",
        "DISTRIBUTED_KEYWORD",
        "MACRO_KEYWORD",
        "PACKAGE_KEYWORD",
        // Declaration modifiers.
        "FINAL_KEYWORD",
        "LAZY_KEYWORD",
        "WEAK_KEYWORD",
        "UNOWNED_KEYWORD",
        "MUTATING_KEYWORD",
        "NONMUTATING_KEYWORD",
        "OVERRIDE_KEYWORD",
        "REQUIRED_KEYWORD",
        "OPTIONAL_KEYWORD",
        "DYNAMIC_KEYWORD",
        "CONVENIENCE_KEYWORD",
        "PREFIX_KEYWORD",
        "INFIX_KEYWORD",
        "POSTFIX_KEYWORD",
        "INDIRECT_KEYWORD",
        // Property accessors.
        "GET_KEYWORD",
        "SET_KEYWORD",
        "WILLSET_KEYWORD",
        "DIDSET_KEYWORD",
        // Precedencegroup body.
        "LEFT_KEYWORD",
        "RIGHT_KEYWORD",
        "NONE_KEYWORD",
        "ASSOCIATIVITY_KEYWORD",
        "HIGHERTHAN_KEYWORD",
        "LOWERTHAN_KEYWORD",
        "ASSIGNMENT_KEYWORD"
    );

    private SwiftTokenTypeFactory() {
    }

    public static IElementType getTokenType(String name) {
        if (CONTEXTUAL_KEYWORDS.contains(name)) {
            return new SwiftContextualKeywordElementType(name);
        }
        return new SwiftTokenType(name);
    }
}
