package internal.org.springframework.content.jpa.io;

import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

public class MySQLBlobResourceLoader implements BlobResourceLoader {

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    public MySQLBlobResourceLoader(JdbcTemplate template, PlatformTransactionManager txnMgr) {
        this.template = template;
        this.txnMgr = txnMgr;
    }

    @Override
    public String getDatabaseName() {
        return "MySQL";
    }

    @Override
    public Resource getResource(String location) {
        return new MySQLBlobResource(location, template, txnMgr);
    }

    @Override
    public ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }
}
