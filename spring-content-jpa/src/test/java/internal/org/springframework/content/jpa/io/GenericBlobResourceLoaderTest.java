package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class GenericBlobResourceLoaderTest {

    private GenericBlobResourceLoader loader;

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    private DataSource ds;
    private Connection conn;
    private Statement stmt;

    private Object result;

    {
        Describe("GenericBlobResourceLoader", () -> {
            JustBeforeEach(() -> {
                loader = new GenericBlobResourceLoader(template, txnMgr);
            });
            Context("#getDatabaseName", () -> {
                JustBeforeEach(() -> {
                    result = loader.getDatabaseName();
                });
                It("should return 'GENERIC'", () -> {
                    assertThat(result.toString(), is("GENERIC"));
                });
            });
            Context("#getResource", () -> {
                BeforeEach(() -> {
                    ds = mock(DataSource.class);
                    template = new JdbcTemplate(ds);
                    txnMgr = new DataSourceTransactionManager(ds);

                    conn = mock(Connection.class);
                    when(ds.getConnection()).thenReturn(conn);
                    stmt = mock(Statement.class);
                    when(conn.createStatement()).thenReturn(stmt);
                });
                JustBeforeEach(() -> {
                    result = loader.getResource("some-id");
                });
                It("should return a GenericBlobResource", () -> {
                    assertThat(result, instanceOf(GenericBlobResource.class));
                });
            });
            Context("#getClassLoader", () -> {
                JustBeforeEach(() -> {
                    result = loader.getClassLoader();
                });
                It("should return a class loader", () -> {
                    assertThat(result, instanceOf(ClassLoader.class));
                });
            });
        });
    }
}
