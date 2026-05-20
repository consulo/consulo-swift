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

import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.swift.language.psi.SwiftTypes;

public final class SwiftTokenSets {
    public static final TokenSet COMMENTS = TokenSet.create(SwiftTypes.LINE_COMMENT, SwiftTypes.BLOCK_COMMENT);
    public static final TokenSet STRING_LITERALS = TokenSet.create(SwiftTypes.STATIC_STRING_LITERAL, SwiftTypes.INTERPOLATED_STRING_LITERAL);
    public static final TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

    private SwiftTokenSets() {
    }
}
