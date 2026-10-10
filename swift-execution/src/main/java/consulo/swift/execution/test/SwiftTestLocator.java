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
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.test.sm.FileUrlProvider;
import consulo.execution.test.sm.runner.SMTestLocator;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Locations of test tree nodes:
 * <ul>
 *     <li>{@code file://path:line} - swift-testing reports where every test is</li>
 *     <li>{@code swift-xctest://Class} and {@code swift-xctest://Class/method} - XCTest only reports names, which are
 *     looked up in the Swift declaration indexes</li>
 * </ul>
 */
public final class SwiftTestLocator implements SMTestLocator {
    public static final SwiftTestLocator INSTANCE = new SwiftTestLocator();

    public static final String XCTEST_PROTOCOL = "swift-xctest";

    private SwiftTestLocator() {
    }

    @RequiredReadAction
    @Override
    public List<Location> getLocation(String protocol, String path, Project project, GlobalSearchScope scope) {
        if (!XCTEST_PROTOCOL.equals(protocol)) {
            return FileUrlProvider.INSTANCE.getLocation(protocol, path, project, scope);
        }

        int slash = path.indexOf('/');
        String className = slash < 0 ? path : path.substring(0, slash);
        String method = slash < 0 ? null : path.substring(slash + 1);

        Collection<SwiftDeclaration> declarations = method == null
            ? SwiftDeclarationIndex.findTypes(className, project, scope)
            : SwiftDeclarationIndex.findFunctionsOf(className, method, project, scope);
        List<Location> locations = new ArrayList<>();
        for (SwiftDeclaration declaration : declarations) {
            locations.add(PsiLocation.fromPsiElement(project, declaration));
        }
        return locations;
    }
}
