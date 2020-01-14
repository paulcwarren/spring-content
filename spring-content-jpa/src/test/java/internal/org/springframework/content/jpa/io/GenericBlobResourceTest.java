package internal.org.springframework.content.jpa.io;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class GenericBlobResourceTest {

	private GenericBlobResource resource;

	private String id;
	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private DataSource ds;
	private Connection conn;
	private Statement statement;
	private ResultSet rs;

	private Object result;

	{
		Describe("GenericBlobResource", () -> {
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
					resource = new GenericBlobResource(id, template, txnMgr);
					result = resource.exists();
				});
				Context("given the resultset throws SQLException", () -> {
					BeforeEach(() -> {
						when(rs.next()).thenThrow(new SQLException("badness"));
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
					resource = new GenericBlobResource(id, template, txnMgr);
					result = resource.getInputStream();
				});
				Context("given a SQLException is thrown", () -> {
					BeforeEach(() -> {
						when(rs.next()).thenThrow(new SQLException("badness"));
					});
					It("should return null", () -> {
						assertThat(result, is(nullValue()));
					});
				});
			});
		});
	}
}
