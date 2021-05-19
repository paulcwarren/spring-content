package org.springframework.content.elasticsearch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.sql.DataSource;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableElasticsearchFulltextIndexing
@EnableJpaRepositories(considerNestedRepositories = true)
@EnableJpaStores
public class ElasticsearchConfig {

    @Bean
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

    @Bean
    public RestHighLevelClient client() {
        return ElasticsearchTestContainer.client();
    }

    @Bean
    public RenditionProvider resourceConverter() {
        return new RenditionProvider() {

            @Override
            public String consumes() {
                return "image/png";
            }

            @Override
            public String[] produces() {
                return new String[] {"text/plain"};
            }

            @Override
            public InputStream convert(InputStream fromInputSource, String toMimeType) {
                return new ByteArrayInputStream("It was the best of times, the worst of times, it was the age of wisdom, it was the age of foolishness".getBytes());
            }
        };
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
        factory.setPackagesToScan("org.springframework.content.elasticsearch");
        factory.setDataSource(dataSource());

        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return txManager;
    }

    @Value("/org/springframework/content/jpa/schema-drop-hsqldb.sql")
    private Resource dropReopsitoryTables;

    @Value("/org/springframework/content/jpa/schema-hsqldb.sql")
    private Resource dataReopsitorySchema;

    @Bean
    DataSourceInitializer datasourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator databasePopulator =
                new ResourceDatabasePopulator();

        databasePopulator.addScript(dropReopsitoryTables);
        databasePopulator.addScript(dataReopsitorySchema);
        databasePopulator.setIgnoreFailedDrops(true);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator);

        return initializer;
    }
}
