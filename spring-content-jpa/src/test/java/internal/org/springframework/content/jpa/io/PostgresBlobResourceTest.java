package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
public class PostgresBlobResourceTest {

    private PostgresBlobResource resource;

    private String id;
    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    private DataSource ds;
    private Connection conn;
    private Statement statement;
    private PreparedStatement preparedStatement;
    private ResultSet rs;

    private InputStream in;

    private Object result;


    {
        Describe("PostgresBlobResource", () -> {
            BeforeEach(() -> {
                ds = mock(DataSource.class);
                template = new JdbcTemplate(ds);
                txnMgr = new DataSourceTransactionManager(ds);
            });
            Context("#exists", () -> {
                BeforeEach(() -> {
                    conn = mock(Connection.class);
                    statement = mock(Statement.class);
                    rs = mock(ResultSet.class);

                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.createStatement()).thenReturn(statement);
                    when(statement.executeQuery(anyObject())).thenReturn(rs);
                });
                JustBeforeEach(() -> {
                    resource = new PostgresBlobResource(id, template, txnMgr);
                    result = resource.exists();
                });
                Context("given the blob exists in the database", () -> {
                    BeforeEach(() -> {
                        when(rs.next()).thenReturn(true);
                        when(rs.getInt(1)).thenReturn(1);
                    });
                    It("should return true", () -> {
                        assertThat(result, is(true));
                    });
                });
                Context("given the blob does not exist in the database", () -> {
                    BeforeEach(() -> {
                        when(rs.next()).thenReturn(true);
                        when(rs.getInt(1)).thenReturn(0);
                    });
                    It("should return false", () -> {
                        assertThat(result, is(false));
                    });
                });
                Context("given no blobs exist in the database", () -> {
                    BeforeEach(() -> {
                        when(rs.next()).thenReturn(false);
                    });
                    It("should return false", () -> {
                        assertThat(result, is(false));
                    });
                });
            });
            Context("#getInputStream", () -> {
                BeforeEach(() -> {
                    conn = mock(Connection.class);
                    statement = mock(Statement.class);
                    rs = mock(ResultSet.class);

                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.createStatement()).thenReturn(statement);
                    when(statement.executeQuery(anyObject())).thenReturn(rs);
                });
                JustBeforeEach(() -> {
                    resource = new PostgresBlobResource(id, template, txnMgr);
                    result = resource.getInputStream();
                });
                Context("given the blob exists in the database", () -> {
                    BeforeEach(() -> {
                        when(rs.next()).thenReturn(true);
                        when(rs.getBinaryStream(1)).thenReturn(new ByteArrayInputStream("Hello Spring Content PostgreSQL BLOBby world!".getBytes()));
                    });
                    It("should be an ObservableInputStream with a file remover", () -> {
                        assertThat(result, instanceOf(ObservableInputStream.class));
                        assertThat(((ObservableInputStream)result).getObservers().size(), is(1));
                        assertThat(((ObservableInputStream)result).getObservers().get(0), is(instanceOf(FileRemover.class)));
                    });
                    It("should return the correct content", () -> {
                        InputStream expected = null;
                        try {
                            expected = new ByteArrayInputStream("Hello Spring Content PostgreSQL BLOBby world!".getBytes());
                            assertThat(IOUtils.contentEquals(expected, (InputStream) result), is(true));
                        } finally {
                            IOUtils.closeQuietly(expected);
                            IOUtils.closeQuietly((InputStream)result);
                        }
                    });
                });
                Context("given the blob does not exist in the database", () -> {
                    BeforeEach(() -> {
                        when(rs.next()).thenReturn(false);
                    });
                    It("should return false", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
            });
            Context("#getOutputStream", () -> {
                BeforeEach(() -> {
                    conn = mock(Connection.class);
                    statement = mock(Statement.class);
                    rs = mock(ResultSet.class);

                    when(ds.getConnection()).thenReturn(conn);
                    when(conn.createStatement()).thenReturn(statement);
                    when(statement.executeQuery(anyObject())).thenReturn(rs);
                });
                JustBeforeEach(() -> {
                    id = "999";
                    resource = new PostgresBlobResource(id, template, txnMgr);
                    result = resource.getOutputStream();
                });
                Context("given the blob exists in the database", () -> {
                    BeforeEach(() -> {
                        // exists
                        when(rs.next()).thenReturn(true);
                        when(rs.getInt(1)).thenReturn(1);

                        in = new ByteArrayInputStream("Hello Spring Content JPA PostreSQL World!".getBytes());

                        // update
                        preparedStatement = mock(PreparedStatement.class);
                        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);
                    });
                    JustBeforeEach(() ->{
                        IOUtils.copy(in, (OutputStream)result);

                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly((OutputStream)result);
                    });
                    It("should use update to overwrite the content", () -> {
                        verify(conn, timeout(100)).prepareStatement(argThat(containsString("UPDATE BLOBS")));

                        verify(preparedStatement, timeout(100)).setBinaryStream(eq(1), argThat(is(instanceOf(InputStream.class))));
                        verify(preparedStatement, timeout(100)).setInt(2, 999);
                        verify(preparedStatement, timeout(100)).executeUpdate();
                    });
                });
                Context("given the blob does not exist in the database", () -> {
                    BeforeEach(() -> {
                        // exists
                        when(rs.next()).thenReturn(true);
                        when(rs.getInt(1)).thenReturn(0);

                        in = new ByteArrayInputStream("Hello Spring Content JPA PostgreSQL World!".getBytes());

                        // insert
                        preparedStatement = mock(PreparedStatement.class);
                        when(conn.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);

                        // generated keys
                        ResultSet generatedKeys = mock(ResultSet.class);
                        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
                        when(generatedKeys.next()).thenReturn(true);
                        when(generatedKeys.getInt(1)).thenReturn(9999);
                    });
                    JustBeforeEach(() ->{
                        IOUtils.copy(in, (OutputStream)result);

                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly((OutputStream)result);
                    });
                    It("should use insert to add the content", () -> {
                        verify(conn, timeout(100)).prepareStatement(argThat(containsString("INSERT INTO BLOBS")), eq(Statement.RETURN_GENERATED_KEYS));
                        verify(preparedStatement, timeout(100)).setBinaryStream(eq(1), argThat(is(instanceOf(InputStream.class))));
                        verify(preparedStatement, timeout(100)).executeUpdate();

                        assertThat(resource.getId(), is(9999));
                    });
                    It("should update the ID of the resource from the ID returned by the database", () -> {
                        while (resource.getId().equals(9999) == false) {
                            Thread.sleep(100);
                        }
                        assertThat(resource.getId(), is(9999));
                    });
                });
            });
        });
    }
}
