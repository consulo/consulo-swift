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

import jakarta.annotation.Nonnull;

/**
 * Marker token type for Swift contextual keywords like {@code some},
 * {@code async}, {@code actor}, {@code final}, {@code lazy} etc. — words
 * that the lexer emits as {@code IDENTIFIER} but the parser remaps to a
 * dedicated keyword token when the grammar expects it in a specific
 * position. An annotator can use {@code instanceof} on this class to apply
 * keyword highlighting after parsing.
 */
public class SwiftContextualKeywordElementType extends SwiftTokenType {
    public SwiftContextualKeywordElementType(@Nonnull String debugName) {
        super(debugName);
    }
}
