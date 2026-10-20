package com.windsmoke.dev;

import com.windsmoke.WindSmoke;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Dev-only harness for automated testing. When the JVM runs with
 * {@code -Dwindsmoke.testharness=true} (see the Loom run config), the server
 * watches {@code windsmoke-test.txt} in the game directory and executes every
 * line as a permission-level-4 command whenever the file changes. This lets a
 * test driver rebuild a scene with a file write instead of injecting
 * keystrokes into a window that may not be focused.
 */
public final class TestHarness {
	private static final Path COMMAND_FILE = Path.of("windsmoke-test.txt");
	private static long lastModified = -1L;

	private TestHarness() {
	}

	public static void register() {
		if (!Boolean.getBoolean("windsmoke.testharness")) {
			return;
		}

		WindSmoke.LOGGER.info("[windsmoke] test harness active, watching {}", COMMAND_FILE.toAbsolutePath());

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}

			try {
				if (!Files.isRegularFile(COMMAND_FILE)) {
					return;
				}

				long modified = Files.getLastModifiedTime(COMMAND_FILE).toMillis();

				if (modified == lastModified) {
					return;
				}

				lastModified = modified;
				List<String> lines = Files.readAllLines(COMMAND_FILE);

				for (String raw : lines) {
					String line = raw.trim();

					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}

					if (line.startsWith("/")) {
						line = line.substring(1);
					}

					WindSmoke.LOGGER.info("[windsmoke] harness: /{}", line);
					server.getCommands().performPrefixedCommand(
						server.createCommandSourceStack().withSuppressedOutput(), line);
				}
			} catch (IOException e) {
				WindSmoke.LOGGER.error("[windsmoke] harness failed to read {}", COMMAND_FILE, e);
			}
		});
	}
}
