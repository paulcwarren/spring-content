package internal.org.springframework.content.rest.jpa;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.hsql.AbstractRestIT;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class JpaRestIT extends AbstractRestIT {

//	@Configuration
//	@EnableTransactionManagement
//	public static class MySqlConfig {
//		
//	    @Value("/org/springframework/content/jpa/schema-drop-mysql.sql")
//	    private Resource dropReopsitoryTables;
//
//	    @Value("/org/springframework/content/jpa/schema-mysql.sql")
//	    private Resource dataReopsitorySchema;
//
//	    @Bean
//	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
//	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
//
//	        databasePopulator.addScript(dropReopsitoryTables);
//	        databasePopulator.addScript(dataReopsitorySchema);
//	        databasePopulator.setIgnoreFailedDrops(true);
//
//	        DataSourceInitializer initializer = new DataSourceInitializer();
//	        initializer.setDataSource(dataSource);
//	        initializer.setDatabasePopulator(databasePopulator);
//
//	        return initializer;
//	    }
//	    
//	    @Value("#{environment.MYSQL_URL}")
//	    private String url;
//	    @Value("#{environment.MYSQL_USERNAME}")
//	    private String username;			
//	    @Value("#{environment.MYSQL_PASSWORD}")
//	    private String password;				
//
//		@Bean
//		public DataSource dataSource() {
//			DriverManagerDataSource ds = new DriverManagerDataSource();
//	        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
//	        ds.setUrl(url);
//	        ds.setUsername(username);
//	        ds.setPassword(password);
//	        return ds;
//		}
//
//		@Bean
//		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//			vendorAdapter.setDatabase(Database.MYSQL);
//			vendorAdapter.setGenerateDdl(true);
//
//			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
//			factory.setJpaVendorAdapter(vendorAdapter);
//			factory.setPackagesToScan(getClass().getPackage().getName());
//			factory.setDataSource(dataSource());
//
//			return factory;
//		}
//
//		@Bean
//		public PlatformTransactionManager transactionManager() {
//			JpaTransactionManager txManager = new JpaTransactionManager();
//			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
//			return txManager;
//		}
//	}
	
//	@Configuration
//	@EnableTransactionManagement
//	public static class SqlServerConfig {
//		
//	    @Value("/org/springframework/content/jpa/schema-drop-sqlserver.sql")
//	    private Resource dropReopsitoryTables;
//
//	    @Value("/org/springframework/content/jpa/schema-sqlserver.sql")
//	    private Resource dataReopsitorySchema;
//
//	    @Bean
//	    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
//	        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
//
//	        databasePopulator.addScript(dropReopsitoryTables);
//	        databasePopulator.addScript(dataReopsitorySchema);
//	        databasePopulator.setIgnoreFailedDrops(true);
//
//	        DataSourceInitializer initializer = new DataSourceInitializer();
//	        initializer.setDataSource(dataSource);
//	        initializer.setDatabasePopulator(databasePopulator);
//
//	        return initializer;
//	    }
//	    
//	    @Value("#{environment.SQLSERVER_HOST}")
//	    private String sqlServerHost;
//
//	    @Value("#{environment.SQLSERVER_DB_NAME}")
//	    private String sqlServerDbName;
//
//	    @Value("#{environment.SQLSERVER_USERNAME}")
//	    private String username; 	
//
//	    @Value("#{environment.SQLSERVER_PASSWORD}")
//	    private String password;	
//
//		@Bean
//		public DataSource dataSource() {
//	        DriverManagerDataSource ds = new DriverManagerDataSource();
//	        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//	        String connectionString = String.format("jdbc:sqlserver://%s;databaseName=%s", sqlServerHost, sqlServerDbName);
//	        ds.setUrl(connectionString);
//	        ds.setUsername(username);
//	        ds.setPassword(password);
//	        return ds;
//		}
//
//		@Bean
//		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//			vendorAdapter.setDatabase(Database.SQL_SERVER);
//			vendorAdapter.setGenerateDdl(true);
//
//			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
//			factory.setJpaVendorAdapter(vendorAdapter);
//			factory.setPackagesToScan(getClass().getPackage().getName());
//			factory.setDataSource(dataSource());
//
//			return factory;
//		}
//
//		@Bean
//		public PlatformTransactionManager transactionManager() {
//			JpaTransactionManager txManager = new JpaTransactionManager();
//			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
//			return txManager;
//		}
//	}
}
