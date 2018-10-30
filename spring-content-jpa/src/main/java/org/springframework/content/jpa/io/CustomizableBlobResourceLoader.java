package org.springframework.content.jpa.io;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import internal.org.springframework.content.jpa.io.ResourceProvider;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import java.util.function.Function;

public class CustomizableBlobResourceLoader implements BlobResourceLoader {

	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;
	private String databaseName;
	private ResourceProvider resourceProvider;

	public CustomizableBlobResourceLoader(JdbcTemplate template, PlatformTransactionManager txnMgr) {
		this.template = template;
		this.txnMgr = txnMgr;
		this.databaseName = "GENERIC";
		this.resourceProvider = (l, t, txn) -> { return new GenericBlobResource(l, t, txn);};
	}

	public CustomizableBlobResourceLoader(JdbcTemplate template, PlatformTransactionManager txnMgr, String databaseName, ResourceProvider resourceProvider) {
		this.template = template;
		this.txnMgr = txnMgr;
		this.databaseName = databaseName;
		this.resourceProvider = resourceProvider;
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public Resource getResource(String location) {
		return resourceProvider.getResource(location, template, txnMgr);
	}

	@Override
	public ClassLoader getClassLoader() {
		return ClassUtils.getDefaultClassLoader();
	}
}
