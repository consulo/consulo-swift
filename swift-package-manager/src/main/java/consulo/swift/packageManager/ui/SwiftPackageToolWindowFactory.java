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
package consulo.swift.packageManager.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ui.AbstractExternalSystemToolWindowFactory;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * The "Swift Package Manager" tool window: linked packages with their modules and tasks (build, test, clean, resolve,
 * update), refresh, link and unlink. Shown once a package is linked.
 */
@ExtensionImpl
public class SwiftPackageToolWindowFactory extends AbstractExternalSystemToolWindowFactory {
    public SwiftPackageToolWindowFactory() {
        super(SwiftPackageManagerConstants.SYSTEM_ID);
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return SwiftIconGroup.swift();
    }

    /**
     * Also for Swift modules whose package is not linked yet - the tool window is where it gets linked.
     */
    @Override
    public boolean validate(Project project) {
        return super.validate(project) || ModuleExtensionHelper.getInstance(project).hasModuleExtension(SwiftModuleExtension.class);
    }
}
