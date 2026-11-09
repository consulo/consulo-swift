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

/**
 * A read or a write of a variable at a reference or a declaration, or a declaration without a value - the variable is
 * uninitialized again, each time its scope is entered (a local of a loop body). The range is where the compiler reports
 * a problem of the access: the name read, the {@code =} of an assignment, the operator of {@code x += 1}.
 *
 * @author VISTALL
 */
public class SwiftReadWriteInstruction extends SwiftInstruction {
    public enum Access {
        READ,
        WRITE,
        // x += 1, &x - read and written
        INOUT,
        // the base of a member assigned, x.y = 1 - read, not reported when uninitialized
        MEMBER,
        DECLARE
    }

    private final PsiElement myVariable;
    private final Access myAccess;
    private final TextRange myRange;

    public SwiftReadWriteInstruction(ControlFlowBuilder builder, PsiElement element, PsiElement variable, Access access, TextRange range) {
        super(builder, element, null);
        myVariable = variable;
        myAccess = access;
        myRange = range;
    }

    public PsiElement getVariable() {
        return myVariable;
    }

    public Access getAccess() {
        return myAccess;
    }

    public TextRange getRange() {
        return myRange;
    }

    public boolean isRead() {
        return myAccess == Access.READ || myAccess == Access.INOUT || myAccess == Access.MEMBER;
    }

    public boolean isWrite() {
        return myAccess == Access.WRITE || myAccess == Access.INOUT;
    }

    @Override
    public String getElementPresentation() {
        return myAccess + " " + (SwiftFlowVariables.isProperty(myVariable) ? "self." : "") + SwiftFlowVariables.name(myVariable);
    }
}
