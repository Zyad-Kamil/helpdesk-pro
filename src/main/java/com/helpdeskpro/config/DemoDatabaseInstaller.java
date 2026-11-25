package com.helpdeskpro.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Copies the versioned demo database to the runtime location on first start, before any
 * connection is opened. An existing runtime database is never overwritten.
 */
public class DemoDatabaseInstaller implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	static final String DEMO_PATH_PROPERTY = "helpdesk.database.demo-path";

	static final String RUNTIME_PATH_PROPERTY = "helpdesk.database.runtime-path";

	private static final String H2_FILE_SUFFIX = ".mv.db";

	private static final Log logger = LogFactory.getLog(DemoDatabaseInstaller.class);

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Environment environment = event.getEnvironment();
		Path demo = databaseFile(environment.getRequiredProperty(DEMO_PATH_PROPERTY));
		Path runtime = databaseFile(environment.getRequiredProperty(RUNTIME_PATH_PROPERTY));
		if (install(demo, runtime)) {
			logger.info("Demo database copied to " + runtime);
		}
	}

	/**
	 * Copies the demo database unless it is missing or a runtime database already exists.
	 * @return {@code true} if the copy was made
	 */
	boolean install(Path demo, Path runtime) {
		if (Files.exists(runtime) || Files.notExists(demo)) {
			return false;
		}
		try {
			Path directory = runtime.toAbsolutePath().getParent();
			Files.createDirectories(directory);
			// An interrupted copy must not be mistaken for a valid database on the next start
			Path partialCopy = Files.createTempFile(directory, "demo-", ".tmp");
			try {
				Files.write(partialCopy, Files.readAllBytes(demo));
				Files.move(partialCopy, runtime);
			}
			finally {
				Files.deleteIfExists(partialCopy);
			}
			return true;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Unable to copy the demo database to " + runtime, ex);
		}
	}

	private static Path databaseFile(String databasePath) {
		return Path.of(databasePath + H2_FILE_SUFFIX);
	}

}
