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
package consulo.swift.execution.test;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationKind;
import consulo.swift.language.psi.stub.SwiftDeclarationStubElementType;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * "Run tests" on {@code Tests} (all tests of the package), on {@code Tests/<Target>} (the tests of the target), or on a
 * test file (the test classes and suites declared or extended in it). SwiftPM's {@code --filter} matches {@code Target.Class/method}
 * of XCTest and {@code Target.Suite/test()} of swift-testing alike.
 */
@ExtensionImpl
public class SwiftTestRunConfigurationProducer extends RunConfigurationProducer<SwiftTestRunConfiguration> {
    private static final String TESTS_DIRECTORY = "Tests";

    private record Setup(Module module, String filter, String name) {
    }

    public SwiftTestRunConfigurationProducer() {
        super(SwiftTestRunConfigurationType.getInstance());
    }

    @RequiredReadAction
    @Override
    protected boolean setupConfigurationFromContext(SwiftTestRunConfiguration configuration,
                                                    ConfigurationContext context,
                                                    SimpleReference<PsiElement> sourceElement) {
        Setup setup = createSetup(context);
        if (setup == null) {
            return false;
        }
        configuration.setModule(setup.module());
        configuration.setFilter(setup.filter());
        configuration.setName(setup.name());
        PsiElement element = context.getPsiLocation();
        if (element != null) {
            sourceElement.set(element);
        }
        return true;
    }

    @RequiredReadAction
    @Override
    public boolean isConfigurationFromContext(SwiftTestRunConfiguration configuration, ConfigurationContext context) {
        Setup setup = createSetup(context);
        return setup != null
            && setup.module().equals(configuration.getConfigurationModule().getModule())
            && setup.filter().equals(configuration.getFilter());
    }

    @RequiredReadAction
    private static @Nullable Setup createSetup(ConfigurationContext context) {
        PsiElement element = context.getPsiLocation();
        Module module = context.getModule();
        if (element == null || module == null || module.getExtension(SwiftModuleExtension.class) == null) {
            return null;
        }
        Path packageDirectory = SwiftPackageManagerUtil.findPackageDirectory(module);
        PsiFileSystemItem item = element instanceof PsiFileSystemItem fileSystemItem ? fileSystemItem : element.getContainingFile();
        VirtualFile file = item == null ? null : item.getVirtualFile();
        if (packageDirectory == null || file == null || !file.isInLocalFileSystem() || !file.toNioPath().startsWith(packageDirectory)) {
            return null;
        }

        Path relative = packageDirectory.relativize(file.toNioPath());
        if (relative.getNameCount() == 0 || !TESTS_DIRECTORY.equals(relative.getName(0).toString())) {
            return null;
        }
        if (relative.getNameCount() == 1) {
            return new Setup(module, "", SwiftExecutionLocalize.testProducerAllTests(module.getName()).get());
        }

        String target = relative.getName(1).toString();
        String targetFilter = "^" + c99Name(target) + "\\.";
        if (file.isDirectory() || !(item instanceof PsiFile psiFile)) {
            return new Setup(module, targetFilter, SwiftExecutionLocalize.testProducerTarget(target).get());
        }

        Set<String> types = new LinkedHashSet<>();
        for (SwiftDeclaration declaration : PsiTreeUtil.getChildrenOfTypeAsList(psiFile, SwiftDeclaration.class)) {
            // test classes and suites, also extended in the file
            if (declaration.getNode().getElementType() instanceof SwiftDeclarationStubElementType type
                && (type.getKind() == SwiftDeclarationKind.TYPE || type.getKind() == SwiftDeclarationKind.EXTENSION)
                && declaration.getName() != null) {
                types.add(declaration.getName());
            }
        }
        if (types.isEmpty()) {
            return new Setup(module, targetFilter, SwiftExecutionLocalize.testProducerTarget(target).get());
        }
        String filter = targetFilter + "(" + String.join("|", types) + ")/";
        return new Setup(module, filter, SwiftExecutionLocalize.testProducerFile(file.getNameWithoutExtension()).get());
    }

    /**
     * Test ids use the module name of a target - its name with everything but letters, digits and {@code _} replaced.
     */
    private static String c99Name(String target) {
        StringBuilder builder = new StringBuilder(target.length());
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            builder.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
        }
        if (builder.length() > 0 && Character.isDigit(builder.charAt(0))) {
            builder.insert(0, '_');
        }
        return builder.toString();
    }
}
