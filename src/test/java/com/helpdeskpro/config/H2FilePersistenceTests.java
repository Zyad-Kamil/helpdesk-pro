package com.helpdeskpro.config;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.helpdeskpro.HelpdeskProApplication;
import com.helpdeskpro.entity.Category;
import com.helpdeskpro.entity.Tag;
import com.helpdeskpro.entity.Technician;
import com.helpdeskpro.entity.TechnicianProfile;
import com.helpdeskpro.entity.Ticket;
import com.helpdeskpro.repository.CategoryRepository;
import com.helpdeskpro.repository.TagRepository;
import com.helpdeskpro.repository.TechnicianProfileRepository;
import com.helpdeskpro.repository.TechnicianRepository;
import com.helpdeskpro.repository.TicketRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbc.JdbcConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Starts the application with the production configuration, on a temporary database file.
 */
class H2FilePersistenceTests {

	@TempDir
	private Path directory;

	@Test
	void dataSurvivesAFullApplicationRestart() throws SQLException {
		Path database = this.directory.resolve("runtime").resolve("helpdesk");
		try (ConfigurableApplicationContext context = start(database)) {
			assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
			insertOneEntityOfEachType(context);
		}
		assertThat(this.directory.resolve("runtime").resolve("helpdesk.mv.db")).isNotEmptyFile();

		// Reopened without Spring nor Hibernate: the data really is in the file
		try (Connection connection = DriverManager.getConnection(
				"jdbc:h2:file:" + toUrlPath(database) + ";IFEXISTS=TRUE", "sa", "");
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("select title from ticket")) {
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString("title")).isEqualTo("Persistent ticket");
		}

		// Restarted with the same file: the schema update keeps the existing data
		try (ConfigurableApplicationContext context = start(database)) {
			assertThat(context.getBean(TechnicianProfileRepository.class).count()).isOne();
			context.getBean(TransactionTemplate.class).executeWithoutResult((status) -> {
				Ticket ticket = context.getBean(TicketRepository.class).findAll().getFirst();
				assertThat(ticket.getTechnician().getProfile().getSpecialization()).isEqualTo("Network");
				assertThat(ticket.getCategory().getName()).isEqualTo("Hardware");
				assertThat(ticket.getTags()).extracting(Tag::getName).containsExactly("urgent");
			});
		}
	}

	@Test
	void firstStartUsesTheDemoDatabaseAndFollowingStartsKeepLocalChanges() {
		Path demo = this.directory.resolve("demo").resolve("helpdesk");
		Path runtime = this.directory.resolve("runtime").resolve("helpdesk");
		try (ConfigurableApplicationContext context = start(demo)) {
			context.getBean(CategoryRepository.class).save(new Category("From demo", null));
		}

		try (ConfigurableApplicationContext context = startWithDemo(demo, runtime)) {
			CategoryRepository categories = context.getBean(CategoryRepository.class);
			assertThat(categories.findAll()).extracting(Category::getName).containsExactly("From demo");
			categories.save(new Category("Local change", null));
		}

		try (ConfigurableApplicationContext context = startWithDemo(demo, runtime)) {
			assertThat(context.getBean(CategoryRepository.class).findAll()).extracting(Category::getName)
				.containsExactlyInAnyOrder("From demo", "Local change");
		}
		try (ConfigurableApplicationContext context = start(demo)) {
			assertThat(context.getBean(CategoryRepository.class).findAll()).extracting(Category::getName)
				.containsExactly("From demo");
		}
	}

	@Test
	void insertsStillWorkOnceTheConnectionThatCreatedTheSchemaIsClosed() throws SQLException {
		Path database = this.directory.resolve("helpdesk");
		// With a single pooled connection, the one that created the schema is known. The other
		// connection keeps the database open, as the rest of the pool does in production.
		try (ConfigurableApplicationContext context = start(database,
				"--spring.datasource.hikari.maximum-pool-size=1");
				Connection other = DriverManager.getConnection(context.getEnvironment()
					.getRequiredProperty("spring.datasource.url"), "sa", "")) {
			assertThat(other.isValid(1)).isTrue();
			HikariDataSource dataSource = context.getBean(DataSource.class).unwrap(HikariDataSource.class);
			JdbcConnection creator;
			try (Connection connection = dataSource.getConnection()) {
				creator = connection.unwrap(JdbcConnection.class);
			}
			dataSource.getHikariPoolMXBean().softEvictConnections();
			await().atMost(Duration.ofSeconds(10)).until(creator::isClosed);

			insertOneEntityOfEachType(context);

			assertThat(context.getBean(TicketRepository.class).count()).isOne();
		}
	}

	private static void insertOneEntityOfEachType(ConfigurableApplicationContext context) {
		context.getBean(TransactionTemplate.class).executeWithoutResult((status) -> {
			Technician technician = new Technician("Demo", "Technician", "tech@example.com");
			technician.assignProfile(new TechnicianProfile("0100000000", "Network", null));
			context.getBean(TechnicianRepository.class).save(technician);
			Category category = context.getBean(CategoryRepository.class).save(new Category("Hardware", null));
			Tag tag = context.getBean(TagRepository.class).save(new Tag("urgent"));
			Ticket ticket = new Ticket("Persistent ticket", "Description", "Demo Requester", "requester@example.com");
			context.getBean(TicketRepository.class).save(ticket);
			ticket.assignTechnician(technician);
			ticket.assignCategory(category);
			ticket.addTag(tag);
		});
	}

	private ConfigurableApplicationContext start(Path runtime, String... args) {
		return startWithDemo(this.directory.resolve("no-demo"), runtime, args);
	}

	private static ConfigurableApplicationContext startWithDemo(Path demo, Path runtime, String... args) {
		List<String> allArgs = new ArrayList<>(List.of(args));
		// Only the main application.properties, without the test overrides
		allArgs.add("--spring.config.location=classpath:/application.properties");
		allArgs.add("--" + DemoDatabaseInstaller.DEMO_PATH_PROPERTY + "=" + toUrlPath(demo));
		allArgs.add("--" + DemoDatabaseInstaller.RUNTIME_PATH_PROPERTY + "=" + toUrlPath(runtime));
		return new SpringApplicationBuilder(HelpdeskProApplication.class).web(WebApplicationType.NONE)
			.listeners(new DemoDatabaseInstaller())
			.run(allArgs.toArray(String[]::new));
	}

	private static String toUrlPath(Path path) {
		return path.toString().replace('\\', '/');
	}

}
