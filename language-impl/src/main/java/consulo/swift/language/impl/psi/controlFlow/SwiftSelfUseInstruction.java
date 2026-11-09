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
package consulo.swift.language.impl.psi.controlFlow;

import consulo.document.util.TextRange;
import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * A use of self in an initializer before its stored properties may be initialized: a method called, a computed
 * property read (by its name), self itself (no name)
 *
 * @author VISTALL
 */
public class SwiftSelfUseInstruction extends SwiftInstruction {
    public enum Kind {
        METHOD,
        PROPERTY,
        SELF
    }

    private final Kind myKind;
    private final @Nullable String myName;
    private final TextRange myRange;

    public SwiftSelfUseInstruction(ControlFlowBuilder builder, PsiElement element, Kind kind, @Nullable String name, TextRange range) {
        super(builder, element, SELF_USE);
        myKind = kind;
        myName = name;
        myRange = range;
    }

    public Kind getKind() {
        return myKind;
    }

    public @Nullable String getName() {
        return myName;
    }

    public TextRange getRange() {
        return myRange;
    }

    @Override
    public String getElementPresentation() {
        return SELF_USE + " " + myKind + (myName == null ? "" : " " + myName);
    }
}
