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

package consulo.swift.language.impl.parsing;

import consulo.language.file.LanguageFileType;
import consulo.swift.language.SwiftFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

public class SwiftParsingTest extends SimpleParsingTest<Object> {
    public SwiftParsingTest() {
        super("parsing", "swift");
    }

    @Test public void testHelloWorld(Context context) throws Exception { doTest(context, null); }
    @Test public void testImports(Context context) throws Exception { doTest(context, null); }
    @Test public void testVariables(Context context) throws Exception { doTest(context, null); }
    @Test public void testFunctions(Context context) throws Exception { doTest(context, null); }
    @Test public void testClasses(Context context) throws Exception { doTest(context, null); }
    @Test public void testStructs(Context context) throws Exception { doTest(context, null); }
    @Test public void testEnums(Context context) throws Exception { doTest(context, null); }
    @Test public void testProtocols(Context context) throws Exception { doTest(context, null); }
    @Test public void testExtensions(Context context) throws Exception { doTest(context, null); }
    @Test public void testClosures(Context context) throws Exception { doTest(context, null); }
    @Test public void testControlFlow(Context context) throws Exception { doTest(context, null); }
    @Test public void testLoops(Context context) throws Exception { doTest(context, null); }
    @Test public void testGenerics(Context context) throws Exception { doTest(context, null); }
    @Test public void testErrorHandling(Context context) throws Exception { doTest(context, null); }
    @Test public void testOperators(Context context) throws Exception { doTest(context, null); }
    @Test public void testPatterns(Context context) throws Exception { doTest(context, null); }
    @Test public void testStringLiterals(Context context) throws Exception { doTest(context, null); }
    @Test public void testNumericLiterals(Context context) throws Exception { doTest(context, null); }
    @Test public void testCompilerDirectives(Context context) throws Exception { doTest(context, null); }
    @Test public void testAttributes(Context context) throws Exception { doTest(context, null); }
    @Test public void testComments(Context context) throws Exception { doTest(context, null); }

    // ---- Focused fixtures (one per BNF rule) ----
    @Test public void testTypealias(Context context) throws Exception { doTest(context, null); }
    @Test public void testSubscript(Context context) throws Exception { doTest(context, null); }
    @Test public void testSelector(Context context) throws Exception { doTest(context, null); }
    @Test public void testKeyPath(Context context) throws Exception { doTest(context, null); }
    @Test public void testPlaygroundLiterals(Context context) throws Exception { doTest(context, null); }
    @Test public void testDictionaryLiteral(Context context) throws Exception { doTest(context, null); }
    @Test public void testDictionaryType(Context context) throws Exception { doTest(context, null); }
    @Test public void testProtocolComposition(Context context) throws Exception { doTest(context, null); }
    @Test public void testMetatype(Context context) throws Exception { doTest(context, null); }
    @Test public void testImplicitlyUnwrappedOptional(Context context) throws Exception { doTest(context, null); }
    @Test public void testOptionalPattern(Context context) throws Exception { doTest(context, null); }
    @Test public void testInOutExpression(Context context) throws Exception { doTest(context, null); }
    @Test public void testTernary(Context context) throws Exception { doTest(context, null); }
    @Test public void testNilLiteral(Context context) throws Exception { doTest(context, null); }
    @Test public void testDynamicType(Context context) throws Exception { doTest(context, null); }
    @Test public void testImplicitParameter(Context context) throws Exception { doTest(context, null); }
    @Test public void testBacktickIdentifier(Context context) throws Exception { doTest(context, null); }
    @Test public void testCaseCondition(Context context) throws Exception { doTest(context, null); }
    @Test public void testLabeledStatements(Context context) throws Exception { doTest(context, null); }
    @Test public void testAnyType(Context context) throws Exception { doTest(context, null); }
    @Test public void testSelfType(Context context) throws Exception { doTest(context, null); }
    @Test public void testRangeOperators(Context context) throws Exception { doTest(context, null); }
    @Test public void testSettingsView(Context context) throws Exception { doTest(context, null); }
    @Test public void testSomeContextual(Context context) throws Exception { doTest(context, null); }
    @Test public void testModernSwift(Context context) throws Exception { doTest(context, null); }

    @Nonnull
    @Override
    protected LanguageFileType getFileType(@Nonnull Context context, @Nullable Object o) {
        return SwiftFileType.INSTANCE;
    }
}
