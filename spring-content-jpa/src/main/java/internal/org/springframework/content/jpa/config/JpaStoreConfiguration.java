package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.GenericBlobResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class JpaStoreConfiguration {

    @Autowired
    private DataSource dataSource;

//    @Bean
//    public JpaStoreSchemaManager jpaStoreSchemaManager(DataSource ds) {
//        return new JpaStoreSchemaManager(ds);
//    }
//
//    @PostConstruct
//    public void schemaSetup() {
//        jpaStoreSchemaManager(dataSource).create();
//    }

    @Bean
    public DelegatingBlobResourceLoader blobResourceLoader(DataSource ds, List<BlobResourceLoader> loaders) {
        return new DelegatingBlobResourceLoader(ds, loaders);
    }

    @Bean
    public BlobResourceLoader genericBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
        return new GenericBlobResourceLoader(new JdbcTemplate(ds), txnMgr);
    }

//    @Bean
//    public BlobResourceLoader postgresBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
//        return new PostgresBlobResourceLoader(new JdbcTemplate(ds), txnMgr);
//    }
//
//    @Bean
//    public BlobResourceLoader mysqlBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
//        return new MySQLBlobResourceLoader(new JdbcTemplate(ds), txnMgr);
//    }
}
