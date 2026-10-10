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
package consulo.swift.packageManager.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtensionImpl;
import consulo.externalSystem.service.module.extension.ExternalSystemMutableModuleExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Marks modules imported from a package - its id must be the id of the {@link SwiftPackageManagerConstants#SYSTEM_ID system}.
 */
@ExtensionImpl
public class SwiftPackageModuleExtensionProvider implements ModuleExtensionProvider<ExternalSystemModuleExtensionImpl> {
    @Override
    public String getId() {
        return SwiftPackageManagerConstants.SYSTEM_ID.getId();
    }

    @Override
    public @Nullable String getParentId() {
        return null;
    }

    @Override
    public boolean isSystemOnly() {
        return true;
    }

    @Override
    public LocalizeValue getName() {
        return SwiftPackageManagerLocalize.systemName();
    }

    @Override
    public Image getIcon() {
        return SwiftIconGroup.swift();
    }

    @Override
    public ModuleExtension<ExternalSystemModuleExtensionImpl> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new ExternalSystemModuleExtensionImpl(getId(), moduleRootLayer, SwiftPackageManagerConstants.SYSTEM_ID);
    }

    @Override
    public MutableModuleExtension<ExternalSystemModuleExtensionImpl> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new ExternalSystemMutableModuleExtensionImpl(getId(), moduleRootLayer, SwiftPackageManagerConstants.SYSTEM_ID);
    }
}
