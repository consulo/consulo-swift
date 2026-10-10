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
package consulo.swift.packageManager.project;

import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.service.project.AbstractExternalEntityData;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Swift specifics of an imported module - a child of its {@link ProjectKeys#MODULE} node.
 */
public class SwiftModuleData extends AbstractExternalEntityData {
    private static final long serialVersionUID = 1L;

    /**
     * Processed after the module and its content roots exist.
     */
    public static final Key<SwiftModuleData> KEY = Key.create(SwiftModuleData.class, ProjectKeys.CONTENT_ROOT.getProcessingWeight() + 1);

    private final @Nullable String myToolchainName;
    private final List<String> myExecutableProducts;
    private final boolean myHasTests;

    public SwiftModuleData(@Nullable String toolchainName, List<String> executableProducts, boolean hasTests) {
        super(SwiftPackageManagerConstants.SYSTEM_ID);
        myToolchainName = toolchainName;
        myExecutableProducts = List.copyOf(executableProducts);
        myHasTests = hasTests;
    }

    /**
     * @return the Swift bundle the module is bound to; {@code null} leaves the module's choice as it is
     */
    public @Nullable String getToolchainName() {
        return myToolchainName;
    }

    public List<String> getExecutableProducts() {
        return myExecutableProducts;
    }

    public boolean hasTests() {
        return myHasTests;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof SwiftModuleData that) || !super.equals(o)) {
            return false;
        }
        return Objects.equals(myToolchainName, that.myToolchainName)
            && myExecutableProducts.equals(that.myExecutableProducts)
            && myHasTests == that.myHasTests;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(myToolchainName, myExecutableProducts, myHasTests);
    }
}
