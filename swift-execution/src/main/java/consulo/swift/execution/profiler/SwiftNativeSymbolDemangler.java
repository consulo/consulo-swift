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
package consulo.swift.execution.profiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.SdkTable;
import consulo.nativeDev.profiler.NativeSymbolDemangler;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.swift.toolchain.SwiftSdkType;
import consulo.swift.toolchain.SwiftToolchain;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swift symbols in profiles ({@code $s4Demo5PointV1xSivg} -> {@code Point.x.getter}) through {@code swift-demangle} of
 * the most recent Swift toolchain.
 * <p>
 * {@code swift-demangle} does not flush per line when piped, so it cannot stay running as a filter; it is started once
 * per distinct symbol (a few milliseconds) and the result is cached.
 */
@ExtensionImpl
public class SwiftNativeSymbolDemangler implements NativeSymbolDemangler {
    /**
     * Swift 5+ ({@code $s}), Swift 4.2 ({@code $S}), Embedded Swift ({@code $e}), Swift 4 ({@code _T0}); Mach-O adds {@code _}.
     */
    private static final List<String> MANGLING_PREFIXES = List.of("$s", "_$s", "$S", "_$S", "$e", "_$e", "_T0");
    private static final int TIMEOUT_MS = 5_000;

    private final SdkTable mySdkTable;
    private final Map<String, String> myCache = new ConcurrentHashMap<>();

    @Inject
    public SwiftNativeSymbolDemangler(SdkTable sdkTable) {
        mySdkTable = sdkTable;
    }

    @Override
    public @Nullable String demangle(String symbol) {
        if (!isSwiftSymbol(symbol)) {
            return null;
        }
        String cached = myCache.get(symbol);
        if (cached != null) {
            return cached;
        }
        String demangled = runDemangler(symbol);
        myCache.put(symbol, demangled);
        return demangled;
    }

    private static boolean isSwiftSymbol(String symbol) {
        for (String prefix : MANGLING_PREFIXES) {
            if (symbol.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String runDemangler(String symbol) {
        SwiftToolchain toolchain = SwiftToolchain.fromSdk(mySdkTable.findMostRecentSdkOfType(SwiftSdkType.getInstance()));
        if (toolchain == null) {
            return symbol;
        }
        Path demangler = toolchain.getExecutable("swift-demangle");
        if (!Files.isRegularFile(demangler)) {
            return symbol;
        }

        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withExecutablePath(demangler);
        commandLine.addParameters("--compact", "--simplified", symbol);
        commandLine.withPlatform(toolchain.getPlatform());
        toolchain.configureEnvironment(commandLine.getEnvironment());
        try {
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine, TIMEOUT_MS);
            String demangled = output.getStdout().trim();
            return output.getExitCode() != 0 || demangled.isEmpty() ? symbol : demangled;
        }
        catch (ExecutionException e) {
            return symbol;
        }
    }
}
