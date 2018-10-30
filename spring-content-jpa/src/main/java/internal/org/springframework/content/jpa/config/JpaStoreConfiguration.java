package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.GenericBlobResource;
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

	@Bean
	public DelegatingBlobResourceLoader blobResourceLoader(DataSource ds,
			List<BlobResourceLoader> loaders) {
		return new DelegatingBlobResourceLoader(ds, loaders);
	}

	@Bean
	public BlobResourceLoader genericBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
		return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "GENERIC", (l, t, txn) -> { return new GenericBlobResource(l, t, txn); });
	}

	@Bean
	public BlobResourceLoader mysqlBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
		return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "MySQL", (l, t, txn) -> { return new MySQLBlobResource(l, t, txn); });
	}

	@Bean
	public BlobResourceLoader sqlServerBlobResourceLoader(DataSource ds, PlatformTransactionManager txnMgr) {
		return new CustomizableBlobResourceLoader(new JdbcTemplate(ds), txnMgr, "Microsoft SQL Server", (l, t, txn) -> { return new SQLServerBlobResource(l, t, txn); });
	}
}
