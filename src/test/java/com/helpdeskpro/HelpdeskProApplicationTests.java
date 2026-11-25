package com.helpdeskpro;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HelpdeskProApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
		assertThat(this.context.getBean(HelpdeskProApplication.class)).isNotNull();
	}

	@Test
	void testsRunOnJava21() {
		assertThat(Runtime.version().feature()).isEqualTo(21);
	}

	@Test
	void testsUseAnIsolatedInMemoryDatabase() throws SQLException {
		try (Connection connection = this.dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		}
	}

}
