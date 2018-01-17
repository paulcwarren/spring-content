package internal.org.springframework.content.jpa.io;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegatingBlobResourceLoader implements ResourceLoader {

    private static Log logger = LogFactory.getLog(DelegatingBlobResourceLoader.class);

    private DataSource ds;
    private Map<String, BlobResourceLoader> loaders;

    private String database = null;

    @Autowired
    public DelegatingBlobResourceLoader(DataSource ds, List<BlobResourceLoader> loaders) {
        this.ds = ds;
        this.loaders = new HashMap<>();
        for (BlobResourceLoader loader : loaders) {
            String database = loader.getDatabaseName();
            if (database != null) {
                this.loaders.put(database,loader);
            }
        }
    }

    @Override
    public Resource getResource(String location) {
        if (database == null) {
            try {
                database = ds.getConnection().getMetaData().getDatabaseProductName();
            } catch (SQLException e) {
                database = "GENERIC";
                logger.error("Error fetching database name", e);
            }
        }
        return loaders.get(database).getResource(location);
    }

    @Override
    public ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }
}
