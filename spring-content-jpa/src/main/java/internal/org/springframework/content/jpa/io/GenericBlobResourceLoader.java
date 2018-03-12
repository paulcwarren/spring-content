package internal.org.springframework.content.jpa.io;

import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

public class GenericBlobResourceLoader implements BlobResourceLoader {

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    public GenericBlobResourceLoader(JdbcTemplate template, PlatformTransactionManager txnMgr) {
        this.template = template;
        this.txnMgr = txnMgr;
    }

    @Override
    public String getDatabaseName() {
        return "GENERIC";
    }

    @Override
    public Resource getResource(String location) {
        return new GenericBlobResource(location, template, txnMgr);
    }

    @Override
    public ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }
}
