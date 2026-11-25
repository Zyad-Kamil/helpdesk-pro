package com.helpdeskpro.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDatabaseInstallerTests {

	@TempDir
	private Path directory;

	private final DemoDatabaseInstaller installer = new DemoDatabaseInstaller();

	private Path demo;

	private Path runtime;

	@BeforeEach
	void setUp() {
		this.demo = this.directory.resolve("demo.mv.db");
		this.runtime = this.directory.resolve("runtime").resolve("helpdesk.mv.db");
	}

	@Test
	void copiesDemoDatabaseWhenRuntimeDatabaseIsMissing() throws IOException {
		Files.writeString(this.demo, "demo");

		assertThat(this.installer.install(this.demo, this.runtime)).isTrue();
		assertThat(this.runtime).hasContent("demo");
		assertThat(this.demo).hasContent("demo");
		assertThat(this.runtime.getParent()).isDirectoryNotContaining("glob:**.tmp");
	}

	@Test
	void neverOverwritesAnExistingRuntimeDatabase() throws IOException {
		Files.writeString(this.demo, "demo");
		Files.createDirectories(this.runtime.getParent());
		Files.writeString(this.runtime, "local data");

		assertThat(this.installer.install(this.demo, this.runtime)).isFalse();
		assertThat(this.runtime).hasContent("local data");
	}

	@Test
	void doesNothingWithoutDemoDatabase() {
		assertThat(this.installer.install(this.demo, this.runtime)).isFalse();
		assertThat(this.runtime).doesNotExist();
	}

}
