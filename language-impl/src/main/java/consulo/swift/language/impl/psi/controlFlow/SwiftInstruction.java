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

import consulo.language.ast.ASTNode;
import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.base.InstructionImpl;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * A node of a Swift control flow graph - a statement, a closure created, or a marker named in the dump: the entry, the end
 * of the body, the exit, a join of branches, a call that never returns, a try that may throw, the next element of a for-in,
 * the dispatch of the errors thrown in a do to its catches, the end of the else body of a guard
 *
 * @author VISTALL
 */
public class SwiftInstruction extends InstructionImpl {
    public static final String ENTRY = "entry";
    public static final String END = "end";
    public static final String EXIT = "exit";
    public static final String JOIN = "join";
    public static final String NO_RETURN = "noreturn";
    public static final String THROWS = "throws";
    public static final String NEXT = "next";
    public static final String CATCH = "catch";
    public static final String GUARD_END = "guard end";
    public static final String CASE_BODY = "case body";
    // a use of self in an initializer - its stored properties must be initialized
    public static final String SELF_USE = "self";
    // super.init(...) - the stored properties of the class must be initialized
    public static final String SUPER_INIT = "super.init";

    private final @Nullable String myMarker;

    public SwiftInstruction(ControlFlowBuilder builder, @Nullable PsiElement element, @Nullable String marker) {
        super(builder, element);
        myMarker = marker;
    }

    public boolean is(String marker) {
        return marker.equals(myMarker);
    }

    public @Nullable String getMarker() {
        return myMarker;
    }

    @Override
    public String getElementPresentation() {
        PsiElement element = getElement();
        if (myMarker == null) {
            return presentation(element);
        }
        return element == null || END.equals(myMarker) ? myMarker : myMarker + " " + presentation(element);
    }

    /**
     * {@code IF_STATEMENT if b { return 1 }} - the element type and the text on one line, cut after 50 characters
     */
    public static String presentation(@Nullable PsiElement element) {
        if (element == null) {
            return "null";
        }
        ASTNode node = element.getNode();
        String text = element.getText().replaceAll("\\s+", " ").trim();
        if (text.length() > 50) {
            text = text.substring(0, 50) + "...";
        }
        return (node == null ? element.getClass().getSimpleName() : node.getElementType().toString()) + " " + text;
    }
}
