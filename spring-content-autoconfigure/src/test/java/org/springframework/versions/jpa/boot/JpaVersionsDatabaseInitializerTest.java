//package org.springframework.versions.jpa.boot;
//
//import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
//import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaDatabaseInitializer;
//import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaProperties;
//import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
//import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsProperties;
//import org.junit.runner.RunWith;
//import org.springframework.boot.sql.init.DatabaseInitializationMode;
//import org.springframework.boot.sql.init.DatabaseInitializationMode;
//import org.springframework.core.io.DefaultResourceLoader;
//import org.springframework.core.io.ResourceLoader;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.Statement;
//
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
//import static org.hamcrest.Matchers.containsString;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.atLeastOnce;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.mockito.hamcrest.MockitoHamcrest.argThat;
//
//@RunWith(Ginkgo4jRunner.class)
//public class JpaVersionsDatabaseInitializerTest {
//
//	private JpaVersionsDatabaseInitializer initializer;
//
//	private DataSource ds;
//	private ResourceLoader resourceLoader;
//	private JpaVersionsProperties props;
//
//	// mocks
//	private Statement stmt;
//
//	{
//		Describe("ContentJpaDatabaseInitializer", () -> {
//			JustBeforeEach(() -> {
//				initializer = new JpaVersionsDatabaseInitializer(ds, resourceLoader, props);
//			});
//			BeforeEach(() -> {
//				ds = mock(DataSource.class);
//				resourceLoader = new DefaultResourceLoader();
//				props = new JpaVersionsProperties();
//			});
//			Context("#initialize", () -> {
//				BeforeEach(() -> {
//					Connection conn = mock(Connection.class);
//					when(ds.getConnection()).thenReturn(conn);
//					stmt = mock(Statement.class);
//					when(conn.createStatement()).thenReturn(stmt);
//					DatabaseMetaData metadata = mock(DatabaseMetaData.class);
//					when(conn.getMetaData()).thenReturn(metadata);
//					when(metadata.getDatabaseProductName()).thenReturn("h2");
//				});
//				Context("when initialization is enabled", () -> {
//					JustBeforeEach(() -> {
//						initializer.initialize();
//					});
//					It("should execute CREATE TABLE statements on the database", () -> {
//						verify(stmt, atLeastOnce()).execute(argThat(containsString("CREATE TABLE")));
//					});
//				});
//				Context("when initialization is disabled", () -> {
//					BeforeEach(() -> {
//						props.getInitializer().setInitializeSchema(DatabaseInitializationMode.NEVER);
//					});
//					It("should not execute any statements on the database", () -> {
//						verify(stmt, never()).execute(anyString());
//					});
//				});
//			});
//		});
//	}
//}
