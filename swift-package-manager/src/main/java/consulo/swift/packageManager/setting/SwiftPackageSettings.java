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
package consulo.swift.packageManager.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.DelegatingExternalSystemSettingsListener;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.TreeSet;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
@State(name = "SwiftPackageSettings", storages = @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/swiftpm.xml"))
public class SwiftPackageSettings
    extends AbstractExternalSystemSettings<SwiftPackageSettings, SwiftPackageProjectSettings, SwiftPackageSettingsListener>
    implements PersistentStateComponent<SwiftPackageSettings.MyState> {

    @Inject
    public SwiftPackageSettings(Project project) {
        super(SwiftPackageSettingsListener.TOPIC, project);
    }

    public static SwiftPackageSettings getInstance(Project project) {
        return project.getInstance(SwiftPackageSettings.class);
    }

    @Override
    public void subscribe(ExternalSystemSettingsListener<SwiftPackageProjectSettings> listener) {
        getProject().getMessageBus().connect(getProject()).subscribe(SwiftPackageSettingsListener.TOPIC, new ListenerAdapter(listener));
    }

    @Override
    protected void copyExtraSettingsFrom(SwiftPackageSettings settings) {
    }

    @Override
    protected void checkSettings(SwiftPackageProjectSettings old, SwiftPackageProjectSettings current) {
    }

    @Override
    public MyState getState() {
        MyState state = new MyState();
        fillState(state);
        return state;
    }

    @Override
    public void loadState(MyState state) {
        super.loadState(state);
    }

    private static class ListenerAdapter extends DelegatingExternalSystemSettingsListener<SwiftPackageProjectSettings>
        implements SwiftPackageSettingsListener {
        ListenerAdapter(ExternalSystemSettingsListener<SwiftPackageProjectSettings> delegate) {
            super(delegate);
        }
    }

    public static class MyState implements State<SwiftPackageProjectSettings> {
        private final Set<SwiftPackageProjectSettings> myProjectSettings = new TreeSet<>();

        @Override
        @AbstractCollection(surroundWithTag = false, elementTypes = {SwiftPackageProjectSettings.class})
        public Set<SwiftPackageProjectSettings> getLinkedExternalProjectsSettings() {
            return myProjectSettings;
        }

        @Override
        public void setLinkedExternalProjectsSettings(@Nullable Set<SwiftPackageProjectSettings> settings) {
            if (settings != null) {
                myProjectSettings.addAll(settings);
            }
        }
    }
}
