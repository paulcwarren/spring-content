package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.repository.StoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class InputStreamCallbackHandlerTest {

    private InputStreamCallbackHandler handler;

    private String columnName;

    private PreparedStatement ps;

    private ResultSet resultSet;
    private Exception e;

    private InputStream result;

    {
        Describe("InputStreamCallbackHandler", () -> {

            BeforeEach(() -> {
                columnName = "some-column";
            });
            JustBeforeEach(() -> {
                handler = new InputStreamCallbackHandler(columnName);
                try {
                    result = handler.doInPreparedStatement(ps);
                } catch (Exception e) {
                    this.e = e;
                }
            });

            Context("when the prepared statement is null", () -> {
                It("should return null", () -> {
                    assertThat(result, is(nullValue()));
                });
            });

            Context("when there is a blob in the database", () -> {
                BeforeEach(() -> {
                    ps = mock(PreparedStatement.class);
                    resultSet = mock(ResultSet.class);
                    when(ps.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(true);
                    Blob b = mock(Blob.class);
                    when(resultSet.getBlob(columnName)).thenReturn(b);
                    InputStream is = new ByteArrayInputStream("some content".getBytes());
                    when(b.getBinaryStream()).thenReturn(is);
                });

                It("should return an ObservableInputStream", () -> {
                    assertThat(result, is(not(nullValue())));
                    assertThat(result, is(instanceOf(ObservableInputStream.class)));
                    assertThat(((ObservableInputStream)result).getObservers(), hasItem(instanceOf(FileRemover.class)));
                });

                It("should close the resultSet", ()->{
                    verify(resultSet).close();
                });

                Context("when the blob can't be fetched from the resultSet", () -> {
                    BeforeEach(() -> {
                        when(resultSet.getBlob(columnName)).thenThrow(new SQLException());
                    });
                    It("should throw a StoreAccessException", () -> {
                        assertThat(e, instanceOf(StoreAccessException.class));
                    });
                    It("should close the resultSet", ()->{
                        verify(resultSet).close();
                    });
                });
            });

            Context("when there isnt a blob in the database", () -> {
                BeforeEach(() -> {
                    ps = mock(PreparedStatement.class);
                    resultSet = mock(ResultSet.class);
                    when(ps.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(false);
                    when(resultSet.getBlob(columnName)).thenThrow(new SQLException());
                });
                It("should return null", () -> {
                   assertThat(result, is(nullValue()));
                   assertThat(e, is(nullValue()));
                });
                It("should close the resultSet", ()->{
                    verify(resultSet).close();
                });
            });

            Context("when database cursor cant be advanced", () -> {
               BeforeEach(() -> {
                   ps = mock(PreparedStatement.class);
                   resultSet = mock(ResultSet.class);
                   when(ps.executeQuery()).thenReturn(resultSet);
                   when(resultSet.next()).thenThrow(new SQLException());
               });
               It("should throw a StoreAccessException", () -> {
                   assertThat(e, instanceOf(StoreAccessException.class));
               });
                It("should close the resultSet", ()->{
                    verify(resultSet).close();
                });
            });

            Context("when query fails", () -> {
                BeforeEach(() -> {
                    ps = mock(PreparedStatement.class);
                    when(ps.executeQuery()).thenThrow(new SQLException());
                });
                It("should throw a StoreAccessException", () -> {
                    assertThat(e, instanceOf(StoreAccessException.class));
                });
            });
        });
    }
}
