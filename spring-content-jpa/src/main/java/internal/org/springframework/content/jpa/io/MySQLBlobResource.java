package internal.org.springframework.content.jpa.io;

import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public class MySQLBlobResource extends AbstractBlobResource {

	public MySQLBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
		super(id, template, txnMgr);
	}

	@Override
	protected String getSelectBlobSQL(Object id) {
		return "SELECT id, 'content' as content FROM BLOBS WHERE id='" + id + "'";
	}
}
