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

import consulo.document.util.TextRange;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.swift.language.SwiftFileType;
import consulo.swift.language.impl.psi.controlFlow.SwiftControlFlow;
import consulo.swift.language.impl.psi.controlFlow.SwiftFlowDiagnostics;
import consulo.test.junit.impl.language.SimpleParsingTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The flow diagnostics of {@code diagnostics/<name>.swift} against its markers, in the format of the verifier of swiftc -
 * the same file passes {@code swiftc -frontend -emit-sil -verify <name>.swift}:
 * {@code // expected-error@+1:9 {{message}}} - the line relative to the marker (the same without one), the column (any
 * without one); {@code expected-note} markers are only for swiftc.
 *
 * @author VISTALL
 */
public class SwiftFlowDiagnosticsTest extends SimpleParsingTest<Object> {
    private static final Pattern MARKER = Pattern.compile("expected-(error|warning)(?:@([+-]?\\d+)?(?::(\\d+))?)?\\s*\\{\\{(.*?)}}");

    public SwiftFlowDiagnosticsTest() {
        super("diagnostics", "swift");
    }

    @Test
    public void testReachability(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testInitialization(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testUnused(Context context) throws Exception {
        doTest(context, null);
    }

    @Override
    protected LanguageFileType getFileType(Context context, @Nullable Object testContext) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(Context context, @Nullable Object testContext, PsiFile file) throws Exception {
        Assertions.assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class).isEmpty(), "parse errors");
        String text = file.getText();
        String[] lines = text.split("\n", -1);

        Set<String> expected = new TreeSet<>();
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = MARKER.matcher(lines[i]);
            while (matcher.find()) {
                int line = i + 1 + (matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)));
                String column = matcher.group(3) == null ? "*" : matcher.group(3);
                expected.add(line + ":" + column + ": " + matcher.group(1) + ": " + matcher.group(4));
            }
        }

        List<PsiElement> owners = new ArrayList<>();
        collectOwners(file, owners);
        Set<String> actual = new TreeSet<>();
        for (PsiElement owner : owners) {
            for (SwiftFlowDiagnostics.Problem problem : SwiftFlowDiagnostics.of(owner)) {
                TextRange range = problem.range();
                int line = 1;
                int lineStart = 0;
                for (int offset = 0; offset < range.getStartOffset(); offset++) {
                    if (text.charAt(offset) == '\n') {
                        line++;
                        lineStart = offset + 1;
                    }
                }
                String kind = problem.error() ? "error" : "warning";
                String message = format(problem.message().get());
                String exact = line + ":" + (range.getStartOffset() - lineStart + 1) + ": " + kind + ": " + message;
                String any = line + ":*: " + kind + ": " + message;
                actual.add(expected.contains(any) ? any : exact);
            }
        }
        Assertions.assertEquals(String.join("\n", expected), String.join("\n", actual));
    }

    /**
     * {@code [consulo.swift.language.impl.SwiftFlowLocalize@missing.return](global function, Int)} - the text of the key in
     * the yaml formatted; the localizations are not loaded in a parsing test. Only the last argument may have commas (a type)
     */
    private static String format(String value) throws Exception {
        Matcher matcher = Pattern.compile("\\[([\\w.]+)@([\\w.]+)](?:\\((.*)\\))?").matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String path = "/LOCALIZE-LIB/en_US/" + matcher.group(1) + ".yaml";
        String yaml;
        try (InputStream stream = SwiftFlowDiagnosticsTest.class.getResourceAsStream(path)) {
            Assertions.assertNotNull(stream, path);
            yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher text = Pattern.compile("(?m)^" + Pattern.quote(matcher.group(2)) + ":\\s*\\n\\s+text:\\s*\"(.*)\"$").matcher(yaml);
        Assertions.assertTrue(text.find(), matcher.group(2));
        String pattern = text.group(1);
        int arity = (int) Pattern.compile("\\{\\d}").matcher(pattern).results().count();
        Object[] arguments = matcher.group(3) == null ? new Object[0] : matcher.group(3).split(", ", Math.max(arity, 1));
        return MessageFormat.format(pattern, arguments);
    }

    private static void collectOwners(PsiElement element, List<PsiElement> owners) {
        if (SwiftControlFlow.isOwner(element)) {
            owners.add(element);
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            collectOwners(child, owners);
        }
    }
}
