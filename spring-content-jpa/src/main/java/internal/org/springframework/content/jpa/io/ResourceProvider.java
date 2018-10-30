package internal.org.springframework.content.jpa.io;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public interface ResourceProvider {

    Resource getResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr);

}
