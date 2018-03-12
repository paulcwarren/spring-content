package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.Random;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

//@RunWith(PowerMockRunner.class)
//@PowerMockRunnerDelegate(Ginkgo4jRunner.class)
//@Ginkgo4jConfiguration(threads=1)
//@PrepareForTest(RowCountCallbackHandler.class)
public class MySQLBlobResourceTest {

    private MySQLBlobResource resource;

    private String id;
    private JdbcTemplate template;
    private JdbcTemplateServices templateServices;
    private PlatformTransactionManager manager;
    private Object result;

    private PreparedStatementCallback handler;

    {
        Describe("MySQLBlobResource", () -> {
            BeforeEach(() -> {
                template = mock(JdbcTemplate.class);
                templateServices = mock(JdbcTemplateServices.class);

                id = "12345";
            });
            JustBeforeEach(() -> {
                resource = new MySQLBlobResource(id, template, manager);
            });
            Context("#exists", () -> {
                JustBeforeEach(() -> {
                    result = resource.exists();
                });
                Context("when the content does not exist in the database", () -> {
                    BeforeEach(() -> {
                        RowCountCallbackHandler counter = mock(RowCountCallbackHandler.class);
                        when(templateServices.newRowCountCallbackHandler()).thenReturn(counter);
                        when(counter.getRowCount()).thenReturn(0);
                    });
                    It("should return false", () -> {
                        verify(template).query(org.mockito.Matchers.<String>eq("SELECT id FROM BLOBS WHERE id='" + id + "'"), org.mockito.Matchers.<RowCountCallbackHandler>any(RowCountCallbackHandler.class));

                        assertThat(result, is(false));
                    });
                });
                Context("when the content does exist in the database", () -> {
                    BeforeEach(() -> {
                        RowCountCallbackHandler counter = mock(RowCountCallbackHandler.class);
                        doReturn(1).when(counter).getRowCount();
                        when(templateServices.newRowCountCallbackHandler()).thenReturn(counter);
                    });
                    It( "should return true", () -> {
                        verify(template).query(org.mockito.Matchers.<String>eq("SELECT id FROM BLOBS WHERE id='" + id + "'"), org.mockito.Matchers.<RowCountCallbackHandler>any(RowCountCallbackHandler.class));

                        assertThat(result, is(true));
                    });
                });
            });
            Context("#isReadable", () -> {
                JustBeforeEach(() -> {
                    result = resource.isReadable();
                });
                It("should return true", () -> {
                    assertThat(result, is(true));
                });
            });
            Context("#isOpen", () -> {
                JustBeforeEach(() -> {
                    result = resource.isOpen();
                });
                It("should return true", () -> {
                    assertThat(result, is(false));
                });
            });
            Context("#getFilename", () -> {
                JustBeforeEach(() -> {
                    result = resource.getFilename();
                });
                It("should return the blob resource's ID", () -> {
                    assertThat(result, is(this.id));
                });
            });
            Context("#getDescription", () -> {
                JustBeforeEach(() -> {
                    result = resource.getDescription();
                });
                It("should return the blob resource's ID", () -> {
                    assertThat((String)result, containsString(this.id));
                });
            });
            Context("#getInputStream", () -> {
                BeforeEach(() -> {
                    handler = mock(InputStreamCallbackHandler.class);
                    when(templateServices.newInputStreamCallbackHandler("blob")).thenReturn((InputStreamCallbackHandler) handler);
                });
                JustBeforeEach(() -> {
                    result = resource.getInputStream();
                });
                Context("when the content does not exist in the database", () -> {
                    It("should return null", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
                Context("when the content exists in the database", () -> {
                    BeforeEach(() -> {
                        when(((InputStreamCallbackHandler)handler).getInputStream()).thenReturn(mock(InputStream.class));
                    });
                    It("should return non-null InputStream", () -> {
                        verify(template).execute(eq("SELECT blob FROM BLOBS WHERE id='12345'"), eq(handler));

                        assertThat(result, is(not(nullValue())));
                        assertThat(result, instanceOf(InputStream.class));
                    });
                });
            });
            Context("#getOutputStream", () -> {
                BeforeEach(() -> {
                });
                JustBeforeEach(() -> {
//                    result = resource.getOutputStream();
                });

                FIt("set data on the database", () -> {
                    DriverManagerDataSource ds = new DriverManagerDataSource();
                    ds.setDriverClassName("org.postgresql.Driver");
                    ds.setUrl("jdbc:postgresql://localhost:5432/sctest");
                    ds.setUsername("warrep");
                    ds.setPassword("");

                    template = new JdbcTemplate(ds);

                    template.execute("DELETE FROM BLOBS");

                    DataSourceTransactionManager manager = new DataSourceTransactionManager(ds);
                    final TransactionTemplate txn = new TransactionTemplate(manager);

                    BlobResource r = new PostgresBlobResource("32", template, manager);
                    assertThat(r.exists(), is(false));

                    byte[] testData = new byte[256];
                    new Random(new java.util.Date().getTime()).nextBytes(testData);
                    InputStream is = new ByteArrayInputStream(testData);

                    OutputStream os = r.getOutputStream();
                    IOUtils.copy(is, os);
//                    IOUtils.closeQuietly(is);
//                    IOUtils.closeQuietly(os);

                    assertThat(r.exists(), is(false));

//                    System.out.println("waiting");
//                    Thread.sleep(5000);
//                    System.out.println("waited ");

                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);

                    while (r.exists() == false) {
                        System.out.println("sleeping");
//                        Thread.sleep(1);
                    }
                    assertThat(r.exists(), is(true));

//                    final JdbcTemplate db = this.template;

                });

                Context("when the content does not exist in the database", () -> {
                    It("should write content to the database", () -> {
                        OutputStream os = (OutputStream)result;
                        byte[] testData = new byte[5000];
                        new Random().nextBytes(testData);
                        ByteArrayInputStream is = new ByteArrayInputStream(testData);
                        IOUtils.copy(is, os);
                    });
                });
            });
        });
    }
}
