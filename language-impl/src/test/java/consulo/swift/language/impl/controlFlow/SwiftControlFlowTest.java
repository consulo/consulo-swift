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
package consulo.swift.language.impl.controlFlow;

import consulo.language.controlFlow.Instruction;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.swift.language.SwiftFileType;
import consulo.swift.language.impl.psi.controlFlow.SwiftControlFlow;
import consulo.swift.language.impl.psi.controlFlow.SwiftInstruction;
import consulo.test.junit.impl.language.SimpleParsingTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The graphs of the flow owners of {@code flow/<name>.swift}, in the order of the file, against {@code flow/<name>.txt}:
 * a header per owner, then an instruction per line as {@code num(successors) element}, marked {@code !} when the
 * structure of the code does not reach it, {@code ~} when it does but the folded flow (a literal condition, a call never
 * returning) does not. A missing {@code .txt} is written to {@code src/test/resources/flow} and the test fails.
 *
 * @author VISTALL
 */
public class SwiftControlFlowTest extends SimpleParsingTest<Object> {
    public SwiftControlFlowTest() {
        super("flow", "swift");
    }

    @Test
    public void testBranches(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testLoops(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testSwitch(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testJumps(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testDefer(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testErrors(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testVariables(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testOwners(Context context) throws Exception {
        doTest(context, null);
    }

    @Override
    protected LanguageFileType getFileType(Context context, @Nullable Object testContext) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(Context context, @Nullable Object testContext, PsiFile file) throws Exception {
        Assertions.assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class).isEmpty(), "parse errors");
        String actual = dump(file);
        String expected = loadText(context, testContext, "txt");
        if (expected.isEmpty()) {
            Path path = Path.of("src/test/resources/flow", file.getName().replace(".swift", ".txt"));
            Files.writeString(path, actual, StandardCharsets.UTF_8);
            Assertions.fail("written " + path.toAbsolutePath());
        }
        Assertions.assertEquals(expected.replace("\r\n", "\n"), actual);
    }

    private static String dump(PsiFile file) {
        List<PsiElement> owners = new ArrayList<>();
        collectOwners(file, owners);
        StringBuilder builder = new StringBuilder();
        for (PsiElement owner : owners) {
            SwiftControlFlow flow = SwiftControlFlow.of(owner);
            if (flow == null) {
                continue;
            }
            builder.append("# ").append(header(owner)).append('\n');
            for (Instruction instruction : flow.getInstructions()) {
                String mark = !flow.isStructurallyReachable(instruction) ? "! " : !flow.isReachable(instruction) ? "~ " : "  ";
                builder.append(mark).append(instruction).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static void collectOwners(PsiElement element, List<PsiElement> owners) {
        if (SwiftControlFlow.isOwner(element)) {
            owners.add(element);
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            collectOwners(child, owners);
        }
    }

    private static String header(PsiElement owner) {
        if (owner instanceof PsiFile) {
            return "top-level code";
        }
        String text = SwiftInstruction.presentation(owner);
        int brace = text.indexOf('{');
        return brace > 0 ? text.substring(0, brace).trim() : text;
    }
}
