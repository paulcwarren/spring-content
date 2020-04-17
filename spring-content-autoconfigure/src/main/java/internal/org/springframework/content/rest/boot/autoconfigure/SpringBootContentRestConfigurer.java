package internal.org.springframework.content.rest.boot.autoconfigure;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.annotation.Order;

@Order(0)
public class SpringBootContentRestConfigurer implements ContentRestConfigurer {

    @Autowired
    private ContentRestProperties properties;

    public SpringBootContentRestConfigurer() {
    }

    public SpringBootContentRestConfigurer(ContentRestProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configure(RestConfiguration config) {

        if (properties == null) {
            return;
        }

        if (properties.getBaseUri() != null) {
            config.setBaseUri(properties.getBaseUri());
        }

        config.setFullyQualifiedLinks(properties.fullyQualifiedLinks());
    }
}
