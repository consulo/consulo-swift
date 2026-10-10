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

import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executor;

/**
 * Follows the JSON event stream swift-testing writes to {@code --event-stream-output-path} while {@code swift test} runs,
 * and hands every event line to the process output (as {@link ProcessOutputTypes#SYSTEM}, which the test console buffers
 * apart from stdout) for {@link SwiftTestEventsConverter}.
 * <p>
 * Must be added to the process handler before the test console, so that the rest of the stream is read when the
 * process ends - before the console finishes the test run.
 */
public class SwiftTestEventStreamTailer implements ProcessListener {
    private static final Logger LOG = Logger.getInstance(SwiftTestEventStreamTailer.class);
    private static final long POLL_MS = 100;

    private final Path myFile;
    private final ProcessHandler myProcessHandler;
    private final ByteArrayOutputStream myPartialLine = new ByteArrayOutputStream();
    private long myPosition;
    private volatile boolean myStopped;

    public SwiftTestEventStreamTailer(Path file, ProcessHandler processHandler) {
        myFile = file;
        myProcessHandler = processHandler;
    }

    public void start(Executor executor) {
        myProcessHandler.addProcessListener(this);
        executor.execute(() -> {
            while (!myStopped) {
                drain();
                try {
                    Thread.sleep(POLL_MS);
                }
                catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    @Override
    public void processTerminated(ProcessEvent event) {
        myStopped = true;
        drain();
        try {
            Files.deleteIfExists(myFile);
        }
        catch (IOException e) {
            LOG.debug(e);
        }
    }

    private synchronized void drain() {
        try (FileChannel channel = FileChannel.open(myFile, StandardOpenOption.READ)) {
            if (channel.size() <= myPosition) {
                return;
            }
            channel.position(myPosition);
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            int read;
            while ((read = channel.read(buffer)) > 0) {
                myPosition += read;
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    if (b == '\n') {
                        String line = myPartialLine.toString(StandardCharsets.UTF_8);
                        myPartialLine.reset();
                        if (!line.isBlank()) {
                            String event = SwiftTestEventsConverter.EVENT_PREFIX + line + "\n";
                            myProcessHandler.notifyTextAvailable(event, ProcessOutputTypes.SYSTEM);
                        }
                    }
                    else {
                        myPartialLine.write(b);
                    }
                }
                buffer.clear();
            }
        }
        catch (NoSuchFileException e) {
            // swift-testing has not started yet, or there are no swift-testing tests
        }
        catch (IOException e) {
            LOG.warn("Cannot read swift-testing events from " + myFile, e);
        }
    }
}
