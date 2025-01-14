package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.StoreIT.SqlServerConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1) // required
public class TransactionIT {

    private static Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
//            SqlServerConfig.class,
//            StoreIT.OracleConfig.class
    };

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	// for postgres (large object api operations must be in a transaction)
	private PlatformTransactionManager ptm;

	private TestEntityRepository repo = null;
	private TestEntityContentRepository store = null;
	private DbService dbService = null;

	private TestEntity te = null;


	{
		Describe("TransactionTest", () -> {

			for (Class<?> configClass : CONFIG_CLASSES) {

				Context(getContextName(configClass), () -> {
					Context("given a context with a repository and a store", () -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(TestConfig.class);
							context.register(configClass);
							context.refresh();

							repo = context.getBean(TestEntityRepository.class);
							store = context.getBean(TestEntityContentRepository.class);
							dbService = context.getBean(DbService.class);
							ptm = context.getBean(PlatformTransactionManager.class);

							te = new TestEntity();
							te = repo.save(te);
							assertThat(te.getId(), is(not(nullValue())));
							assertThat(te.getContentId(), is(nullValue()));
						});
						AfterEach(() -> {
							context.close();
						});
						Context("given an exception is thrown causing a rollback", () -> {
							It("should not commit changes to content", () -> {

								try {
									te = dbService.doSomeDbStuff(store, te);
								} catch (Exception e) {
									ContentStoreIT.doInTransaction(ptm, () -> {
										try (InputStream result = store.getContent(te)) {
											assertThat(result, is(nullValue()));
										} catch (IOException e1) {}
										return null;
									});
								}
							});
						});
					});
				});
			}
		});
	}

	@Test
	public void noop() {}

	private static String getContextName(Class<?> configClass) {
		return configClass.getSimpleName().replaceAll("Config", "");
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories=true)
	@EnableJpaStores
	public static class TestConfig {

		@Bean
		public DbService dbService() {
			return new DbService();
		}
	}


	@Component
	public static class DbService {

		@Transactional
		public TestEntity doSomeDbStuff(TestEntityContentRepository store, TestEntity te) throws Exception {
			te = store.setContent(te, new ByteArrayInputStream("Spring Content World!".getBytes()));
			throw new RuntimeException("badness");
		}
	}

	@Entity
	@Getter
	@Setter
	@NoArgsConstructor
	@Table(name="test_entities")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
