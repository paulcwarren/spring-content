package internal.org.springframework.content.jpa.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public class MySQLBlobResource extends AbstractBlobResource implements BlobResource {

    private static Log logger = LogFactory.getLog(MySQLBlobResource.class);

    public MySQLBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        super(id, template, txnMgr);
    }
}
