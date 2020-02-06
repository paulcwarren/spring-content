package internal.org.springframework.content.rest.it.mysql;

import javax.sql.DataSource;

import internal.org.springframework.content.rest.it.SecurityConfiguration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(excludeFilters={
		@Filter(type = FilterType.REGEX,
				pattern = {
						".*MongoConfiguration", 
		})
})
public class Application {

	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
	
	@Configuration
    @Import({RestConfiguration.class, SecurityConfiguration.class})
	@EnableJpaRepositories(basePackages="internal.org.springframework.content.rest.support")
	@EnableTransactionManagement
	@EnableJpaStores(basePackages="internal.org.springframework.content.rest.it")
	public static class AppConfig {
        
        @Value("/org/springframework/content/jpa/schema-drop-mysql.sql")
        private ClassPathResource dropReopsitoryTables;

        @Value("/org/springframework/content/jpa/schema-mysql.sql")
        private ClassPathResource dataReopsitorySchema;

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
        
        @Value("#{environment.MYSQL_URL}")
        private String url;
        @Value("#{environment.MYSQL_USERNAME}")
        private String username;
        @Value("#{environment.MYSQL_PASSWORD}")
        private String password;

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            return ds;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.MYSQL);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("internal.org.springframework.content.rest.support");
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
}
