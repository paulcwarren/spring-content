package internal.org.springframework.content.jpa.io;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes = PostgresBlobResourceIT.PostgresConfig.class)
@Ginkgo4jConfiguration(threads = 1)
public class PostgresBlobResourceIT {

    @Autowired
    private DataSource ds;

    @Autowired
    private PlatformTransactionManager txn;

    private JdbcTemplate template;

    private String entityId = null;
    private long lobId;

    private PostgresBlobResource r = null;

    {
        Describe("PostgresBlobResource", () -> {

            BeforeEach(() -> {

                entityId = UUID.randomUUID().toString();
                template = new JdbcTemplate(ds);

                r = new PostgresBlobResource(entityId, template, txn);
            });

            Context("given there is content", () -> {

                BeforeEach(() -> {

                    DataSource ds = this.template.getDataSource();
                    Connection conn = DataSourceUtils.getConnection(ds);

                    TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

                    try (OutputStream os = r.getOutputStream()) {
                        os.write("Hello Spring Content World!".getBytes());
                    }

                    txn.commit(status);

                    // assert associated lob resource exist
                    {
                        String sql = "SELECT id, content FROM BLOBS WHERE id='" + entityId + "'";

                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql);
                        assertThat(rs.next(), is(true));
                        lobId = rs.getLong(2);
                        rs.close();
                        stmt.close();

                        sql = "SELECT * from pg_largeobject where loid = " + lobId;
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery(sql);
                        assertThat(rs.next(), is(true));
                        rs.close();
                        stmt.close();

                        sql = "SELECT * from pg_largeobject_metadata where oid = " + lobId;
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery(sql);
                        assertThat(rs.next(), is(true));
                        rs.close();
                        stmt.close();
                    }

                });

                Context("when the content is deleted", () -> {

                    BeforeEach(()-> {
                        r.delete();
                    });

                    It("should delete the associated lob resources", () -> {

                        DataSource ds = this.template.getDataSource();
                        Connection conn = DataSourceUtils.getConnection(ds);

                        String sql = "SELECT * from pg_largeobject where loid = " + lobId;
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql);
                        assertThat(rs.next(), is(false));
                        rs.close();
                        stmt.close();

                        sql = "SELECT * from pg_largeobject_metadata where oid = " + lobId;
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery(sql);
                        assertThat(rs.next(), is(false));
                        rs.close();
                        stmt.close();
                    });
                });
            });
        });
    }

    @Configuration
    @EnableTransactionManagement
    public static class PostgresConfig {

        @Value("/org/springframework/content/jpa/schema-drop-postgresql.sql")
        private Resource dropStoreTables;

        @Value("/org/springframework/content/jpa/schema-postgresql.sql")
        private Resource dataStoreSchema;

        @Bean
        DataSourceInitializer datasourceInitializer(DataSource dataSource) {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropStoreTables);
            databasePopulator.addScript(dataStoreSchema);
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

    @Test
    public void noop() {}
}
