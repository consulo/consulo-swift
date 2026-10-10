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
package consulo.swift.toolchain.module.extension;

import consulo.content.bundle.SdkType;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionWithSdkBase;
import consulo.swift.toolchain.SwiftSdkType;
import consulo.swift.toolchain.SwiftToolchain;
import org.jspecify.annotations.Nullable;

/**
 * Marks a module as Swift and binds it to a {@link SwiftSdkType Swift toolchain}.
 */
public class SwiftModuleExtension extends ModuleExtensionWithSdkBase<SwiftModuleExtension> {
    public SwiftModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Override
    public Class<? extends SdkType> getSdkTypeClass() {
        return SwiftSdkType.class;
    }

    public @Nullable SwiftToolchain getToolchain() {
        return SwiftToolchain.fromSdk(getSdk());
    }

    public static @Nullable SwiftToolchain findToolchain(@Nullable Module module) {
        if (module == null) {
            return null;
        }
        SwiftModuleExtension extension = module.getExtension(SwiftModuleExtension.class);
        return extension == null ? null : extension.getToolchain();
    }
}
