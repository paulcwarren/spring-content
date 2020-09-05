package org.springframework.content.elasticsearch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.elasticsearch.ElasticsearchIT.Document;
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
public class CustomAttributesConfig {

    @Bean
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

    @Bean
    public RestHighLevelClient client() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    @Bean
    public AttributeProvider<Document> attributeProvider() {
        return new AttributeProvider<Document>() {

            @Override
            public Map<String, String> synchronize(Document entity) {

                Map<String, String> attrs = new HashMap<>();
                attrs.put("title", entity.getTitle());
                attrs.put("author", entity.getAuthor());
                return attrs;
            }
        };
    }

    @Bean
    public FilterQueryProvider filterQueryProvider() {
        return new FilterQueryProvider() {

            @Override
            public Map<String, Object> filterQueries(Class<?> entity) {
                return Collections.singletonMap("author", "Buck Rogers");
            }
        };
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
