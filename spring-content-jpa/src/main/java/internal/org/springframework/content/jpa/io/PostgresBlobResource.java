package internal.org.springframework.content.jpa.io;

import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public class PostgresBlobResource extends AbstractBlobResource implements BlobResource {

    public PostgresBlobResource(String id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        super(id, template, txnMgr);
    }
}
