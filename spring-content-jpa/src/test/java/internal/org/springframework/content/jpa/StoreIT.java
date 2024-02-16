package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.testsupport.stores.DocumentStore;
import net.bytebuddy.utility.RandomString;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class StoreIT {

	private static Class<?>[] CONFIG_CLASSES = new Class[]{
			H2Config.class,
			HSQLConfig.class,
			MySqlConfig.class,
			PostgresConfig.class,
			SqlServerConfig.class,
			OracleConfig.class
	};

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
	
	// for postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;

    private DocumentStore store;

    private Resource r;
    private Exception e;

    private TransactionStatus status;

    {
        Describe("Store", () -> {

			for (Class<?> configClass : CONFIG_CLASSES) {
				
				Context(getContextName(configClass), () -> {
			
					BeforeEach(() -> {
						context = new AnnotationConfigApplicationContext();
						context.register(TestConfig.class);
						context.register(configClass);
						context.refresh();
						
						ptm = context.getBean(PlatformTransactionManager.class);
						store = context.getBean(DocumentStore.class);
						
						if (ptm == null) {
							ptm = mock(PlatformTransactionManager.class);
						}
					});
		
		            Context("within a transaction", () -> {
		                BeforeEach(() -> {
		                    if (ptm == null) {
		                        ptm = mock(PlatformTransactionManager.class);
		                    }
		
		                    status = ptm.getTransaction(new DefaultTransactionDefinition());
		                });
		                AfterEach(() -> {
		                    ptm.commit(status);
		                });
		                Context("given a new resource", () -> {
		                    BeforeEach(() -> {
		                        r = store.getResource(getId());
		                    });
		                    It("should not exist", () -> {
		                        assertThat(r.exists(), is(false));
		                    });
		                    Context("given content is added to that resource", () -> {
		                        BeforeEach(() -> {
		                            InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
		                            OutputStream os = ((WritableResource) r).getOutputStream();
		
		                            try {
		                                IOUtils.copy(is, os);
		                            } finally {
		                                is.close();
		                                os.close();
		                            }
		                        });
		                        AfterEach(() -> {
		                            try {
		                                ((DeletableResource) r).delete();
		                            } catch (Exception e) {
		                                // do nothing
		                            }
		                        });
		                        It("should store that content", () -> {
		                            try {
		                                assertThat(r.exists(), is(true));
		                            } catch (Throwable t) {
		                                t.printStackTrace();
		
		                                throw t;
		                            }
		
		                            boolean matches = false;
		                            InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
		                            InputStream actual = null;
		                            try {
		                                actual = r.getInputStream();
		                                matches = IOUtils.contentEquals(expected, actual);
		                            } catch (IOException e) {
		                            } finally {
		                                IOUtils.closeQuietly(expected);
		                                IOUtils.closeQuietly(actual);
		                            }
		                            assertThat(matches, Matchers.is(true));
		
		                        });
		                        Context("given that resource is then updated", () -> {
		                            BeforeEach(() -> {
		                                InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes());
		                                OutputStream os = ((WritableResource)r).getOutputStream();
		                                IOUtils.copy(is, os);
		                                is.close();
		                                os.close();
		                            });
		                            It("should store that updated content", () -> {
		                                assertThat(r.exists(), is(true));
		
		                                boolean matches = false;
		                                InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes());
		                                InputStream actual = null;
		                                try {
		                                    actual = r.getInputStream();
		                                    matches = IOUtils.contentEquals(expected, actual);
		                                } catch (IOException e) {
		                                } finally {
		                                    IOUtils.closeQuietly(expected);
		                                    IOUtils.closeQuietly(actual);
		                                }
		                                assertThat(matches, Matchers.is(true));
		                            });
		                        });
		                        Context("given that resource is then deleted", () -> {
		                            BeforeEach(() -> {
		                                try {
		                                    ((DeletableResource) r).delete();
		                                } catch (Exception e) {
		                                    this.e = e;
		                                }
		                            });
		                            It("should not exist", () -> {
		                                assertThat(e, is(nullValue()));
		                            });
		                        });
		                    });
		                });
		            });
				});
			}
        });
    }

    protected String getId() {
        RandomString random  = new RandomString(5);
        return "/store-tests/" + random.nextString();
    }
    
	public static String getContextName(Class<?> configClass) {
		return configClass.getSimpleName().replaceAll("Config", "");
	}
	
	@Configuration
	@EnableJpaRepositories(considerNestedRepositories=true)
	@EnableJpaStores
	public static class TestConfig {}
	
	@Configuration
	@EnableTransactionManagement
	public static class H2Config {
		
	    @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
	    private Resource dropReopsitoryTables;

	    @Value("/org/springframework/content/jpa/schema-h2.sql")
	    private Resource dataReopsitorySchema;

	    @Bean
	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

	        databasePopulator.addScript(dropReopsitoryTables);
	        databasePopulator.addScript(dataReopsitorySchema);
	        databasePopulator.setIgnoreFailedDrops(true);

	        DataSourceInitializer initializer = new DataSourceInitializer();
	        initializer.setDataSource(dataSource);
	        initializer.setDatabasePopulator(databasePopulator);

	        return initializer;
	    }
	    
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.H2);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}

	@Configuration
	@EnableTransactionManagement
	public static class HSQLConfig {
		
	    @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
	    private Resource dropReopsitoryTables;

	    @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
	    private Resource dataReopsitorySchema;

	    @Bean
	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

	        databasePopulator.addScript(dropReopsitoryTables);
	        databasePopulator.addScript(dataReopsitorySchema);
	        databasePopulator.setIgnoreFailedDrops(true);

	        DataSourceInitializer initializer = new DataSourceInitializer();
	        initializer.setDataSource(dataSource);
	        initializer.setDatabasePopulator(databasePopulator);

	        return initializer;
	    }
	    
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}
	
	@Configuration
	@EnableTransactionManagement
	public static class MySqlConfig {
		
	    @Value("/org/springframework/content/jpa/schema-drop-mysql.sql")
	    private Resource dropReopsitoryTables;

	    @Value("/org/springframework/content/jpa/schema-mysql.sql")
	    private Resource dataReopsitorySchema;

	    @Bean
	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

	        databasePopulator.addScript(dropReopsitoryTables);
	        databasePopulator.addScript(dataReopsitorySchema);
	        databasePopulator.setIgnoreFailedDrops(true);

	        DataSourceInitializer initializer = new DataSourceInitializer();
	        initializer.setDataSource(dataSource);
	        initializer.setDatabasePopulator(databasePopulator);

	        return initializer;
	    }

		@Bean
		public DataSource dataSource() {
			DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setUrl("jdbc:tc:mysql:5.7.34:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true&emulateLocators=true");
	        ds.setUsername("test");
	        ds.setPassword("test");
	        return ds;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.MYSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource);

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager(
				LocalContainerEntityManagerFactoryBean entityManagerFactory) {

			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory.getObject());
			return txManager;
		}
	}
	
	@Configuration
	@EnableTransactionManagement
	public static class PostgresConfig {
		
	    @Value("/org/springframework/content/jpa/schema-drop-postgresql.sql")
	    private Resource dropReopsitoryTables;

	    @Value("/org/springframework/content/jpa/schema-postgresql.sql")
	    private Resource dataReopsitorySchema;

	    @Bean
	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

	        databasePopulator.addScript(dropReopsitoryTables);
	        databasePopulator.addScript(dataReopsitorySchema);
	        databasePopulator.setIgnoreFailedDrops(true);

	        DataSourceInitializer initializer = new DataSourceInitializer();
	        initializer.setDataSource(dataSource);
	        initializer.setDatabasePopulator(databasePopulator);

	        return initializer;
	    }

		@Bean
		public DataSource dataSource() {
	        DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setUrl("jdbc:tc:postgresql:12:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true");
			ds.setUsername("test");
			ds.setPassword("test");
			return ds;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.POSTGRESQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}

	@Configuration
	@EnableTransactionManagement
	public static class SqlServerConfig {
		
	    @Value("/org/springframework/content/jpa/schema-drop-sqlserver.sql")
	    private Resource dropReopsitoryTables;

	    @Value("/org/springframework/content/jpa/schema-sqlserver.sql")
	    private Resource dataReopsitorySchema;

	    @Bean
	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

	        databasePopulator.addScript(dropReopsitoryTables);
	        databasePopulator.addScript(dataReopsitorySchema);
	        databasePopulator.setIgnoreFailedDrops(true);

	        DataSourceInitializer initializer = new DataSourceInitializer();
	        initializer.setDataSource(dataSource);
	        initializer.setDatabasePopulator(databasePopulator);

	        return initializer;
	    }

		@Bean
		public DataSource dataSource() {
			DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setUrl("jdbc:tc:sqlserver:///databasename?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true");
			ds.setUsername("SA");
			ds.setPassword("A_Str0ng_Required_Password");
			return ds;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.SQL_SERVER);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}

	@Configuration
	@EnableTransactionManagement
	public static class OracleConfig {

		@Value("/org/springframework/content/jpa/schema-drop-oracle.sql")
		private Resource dropRepositoryTables;

		@Value("/org/springframework/content/jpa/schema-oracle.sql")
		private Resource dataRepositorySchema;

		@Bean
		DataSourceInitializer datasourceInitializer(DataSource dataSource) {
			ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

			databasePopulator.addScript(dropRepositoryTables);
			databasePopulator.addScript(dataRepositorySchema);
			databasePopulator.setIgnoreFailedDrops(true);

			DataSourceInitializer initializer = new DataSourceInitializer();
			initializer.setDataSource(dataSource);
			initializer.setDatabasePopulator(databasePopulator);

			return initializer;
		}

		@Bean
		public DataSource dataSource() {
			// Timezone is not set in github containers, need this for connections to work
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

			DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setUrl("jdbc:tc:oracle:///databasename?TC_TMPFS=/testtmpfs:rw?TC_DAEMON=true");
			ds.setUsername("system");
			ds.setPassword("oracle");
			return ds;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.ORACLE);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource);

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager(
				LocalContainerEntityManagerFactoryBean entityManagerFactory) {

			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory.getObject());
			return txManager;
		}
	}
}
