package internal.org.springframework.content.jpa.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AbstractDatabaseInitializer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

public class ContentJpaDatabaseInitializer extends AbstractDatabaseInitializer {

    private ContentJpaProperties properties;

    public ContentJpaDatabaseInitializer(DataSource ds, ResourceLoader resourceLoader, ContentJpaProperties properties) {
        super(ds, resourceLoader);
        Assert.notNull(properties, "ContentJpaProperties must not be null");
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        super.initialize();
    }

    @Override
    protected boolean isEnabled() {
        return properties.getInitializer().isEnabled();
    }

    @Override
    protected String getSchemaLocation() {
        return properties.getSchema();
    }
}
