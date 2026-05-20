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
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.psi.SwiftTypes;

import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class SwiftPairedBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = new BracePair[]{
        new BracePair(SwiftTypes.LBRACE, SwiftTypes.RBRACE, true),
        new BracePair(SwiftTypes.LPAREN, SwiftTypes.RPAREN, false),
        new BracePair(SwiftTypes.LBRACK, SwiftTypes.RBRACK, false),
    };

    @Override
    public Language getLanguage() {
        return SwiftLanguage.INSTANCE;
    }

    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }
}
