package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class DelegatingBlobResourceLoaderTest {

    private DelegatingBlobResourceLoader service;

    private DataSource ds;
    private List<BlobResourceLoader> loaders;

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    private Resource resource;

    {
        Describe("DelegatingBlobResourceLoader", ()->{
            Context("#getResource", () -> {
                JustBeforeEach(() -> {
                    service = new DelegatingBlobResourceLoader(ds, loaders);
                    resource = service.getResource("some-id");
                });
                Context("given a postgres datasource", () -> {
                    BeforeEach(() -> {
                        ds = mock(DataSource.class);
                        Connection conn = mock(Connection.class);
                        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                        when(ds.getConnection()).thenReturn(conn);
                        when(conn.getMetaData()).thenReturn(metadata);
                        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");

                        loaders = new ArrayList<>();
                        loaders.add(new PostgresBlobResourceLoader(mock(JdbcTemplate.class), mock(PlatformTransactionManager.class)));
                    });
                    It("should return a PostgresBlobResource", () -> {
                        assertThat(resource, instanceOf(PostgresBlobResource.class));
                    });
                });
                Context("given a datasource that doesn't have a matching blobresourceloader", () -> {
                    BeforeEach(() -> {
                        ds = mock(DataSource.class);
                        Connection conn = mock(Connection.class);
                        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                        when(ds.getConnection()).thenReturn(conn);
                        when(conn.getMetaData()).thenReturn(metadata);
                        when(metadata.getDatabaseProductName()).thenReturn("SomeOtherDatabase");

                        loaders = new ArrayList<>();
                        loaders.add(new GenericBlobResourceLoader(mock(JdbcTemplate.class), mock(PlatformTransactionManager.class)));
                    });
                    It("should return a GenericBlobResource", () -> {
                        assertThat(resource, instanceOf(GenericBlobResource.class));
                    });
                });
            });
        });
    }
}
