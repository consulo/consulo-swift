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
package consulo.swift.execution.debugger;

import consulo.application.Application;
import consulo.nativeDev.debugger.NativeDebuggerInstallation;
import consulo.nativeDev.debugger.NativeDebuggerProvider;
import consulo.nativeDev.debugger.dap.LldbDapNativeDebuggerProvider;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.toolchain.SwiftToolchain;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Prefers the LLDB of the toolchain the program was built with: a stock LLDB stops at breakpoints and steps fine, but
 * has no Swift type system - Swift variables, {@code po} and expression evaluation need the Swift toolchain's LLDB.
 * <p>
 * When that one cannot start, a stock {@code lldb-dap} is preferred instead - "Auto" would otherwise go by priority,
 * which on Linux is GDB.
 */
public class SwiftDebuggerSetup implements NativeDebuggerSetup {
    private final List<NativeDebuggerInstallation> myPreferredDebuggers;

    private SwiftDebuggerSetup(List<NativeDebuggerInstallation> preferredDebuggers) {
        myPreferredDebuggers = preferredDebuggers;
    }

    public static SwiftDebuggerSetup create(Project project, SwiftToolchain toolchain) {
        Path adapter = toolchain.findDebugAdapter();
        if (adapter == null) {
            return new SwiftDebuggerSetup(List.of());
        }
        SwiftDebugAdapters adapters = SwiftDebugAdapters.getInstance();
        String problem = adapters.findStartProblem(toolchain, adapter);
        if (problem == null) {
            return new SwiftDebuggerSetup(List.of(new NativeDebuggerInstallation(LldbDapNativeDebuggerProvider.ID, adapter)));
        }

        NativeDebuggerInstallation fallback = findStockLldbDap(adapter);
        if (adapters.markReported(adapter)) {
            String text = fallback == null
                ? SwiftExecutionLocalize.debuggerToolchainLldbBroken(adapter, problem).get()
                : SwiftExecutionLocalize.debuggerToolchainLldbBrokenFallback(adapter, problem, fallback.executable()).get();
            Notification notification = new Notification(
                SwiftExecutionNotificationGroupContributor.GROUP,
                SwiftExecutionLocalize.debuggerToolchainLldbTitle().get(),
                text,
                NotificationType.WARNING
            );
            Notifications.Bus.notify(notification, project);
        }
        return new SwiftDebuggerSetup(fallback == null ? List.of() : List.of(fallback));
    }

    /**
     * {@code lldb-dap} found on {@code PATH} that is not the broken one.
     */
    private static @Nullable NativeDebuggerInstallation findStockLldbDap(Path broken) {
        LldbDapNativeDebuggerProvider provider =
            Application.get().getExtensionPoint(NativeDebuggerProvider.class).findExtension(LldbDapNativeDebuggerProvider.class);
        if (provider == null) {
            return null;
        }
        for (NativeDebuggerInstallation installation : provider.findInstallations()) {
            if (!installation.executable().toAbsolutePath().normalize().equals(broken.toAbsolutePath().normalize())) {
                return installation;
            }
        }
        return null;
    }

    @Override
    public List<String> getSourceLanguages() {
        return List.of("swift");
    }

    @Override
    public List<NativeDebuggerInstallation> getToolchainDebuggers() {
        return myPreferredDebuggers;
    }
}
