package org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.jpa.io.GenericBlobResource;
import org.junit.runner.RunWith;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import org.springframework.core.io.Resource;
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
public class CustomizableBlobResourceLoaderTest {

	private CustomizableBlobResourceLoader loader;

	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private DataSource ds;
	private Connection conn;
	private Statement stmt;

	private Resource customDBResource;

	private Object result;

	{
		Describe("CustomizableBlobResourceLoader", () -> {
			JustBeforeEach(() -> {
				loader = new CustomizableBlobResourceLoader(template, txnMgr);
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
			Context("given a resource provider", () -> {
				BeforeEach(()->{
					customDBResource = mock(Resource.class);
				});
				JustBeforeEach(() -> {
					loader = new CustomizableBlobResourceLoader(template, txnMgr, "CUSTOM_DB", (l, t, txn) -> { return customDBResource; });
				});
				Context("#getResource", () -> {
					BeforeEach(() -> {
						ds = mock(DataSource.class);
						template = new JdbcTemplate(ds);
						txnMgr = new DataSourceTransactionManager(ds);
					});
					JustBeforeEach(() -> {
						result = loader.getResource("some-id");
					});
					It("should return the resource providers custom resource", () -> {
						assertThat(result, is(customDBResource));
					});
				});
			});
		});
	}
}
