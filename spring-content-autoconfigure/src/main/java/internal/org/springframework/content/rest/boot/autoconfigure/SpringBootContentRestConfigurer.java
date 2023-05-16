package internal.org.springframework.content.rest.boot.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;

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

        config.setShortcutLinks(!properties.shortcutRequestMappings().disabled());

        if (properties.shortcutRequestMappings().excludes() != null) {
            String[] exclusions = properties.shortcutRequestMappings().excludes().split(":");
            for (String exclusion : exclusions) {
                String[] segments = exclusion.split("=");
                if (segments.length != 2) {
                    continue;
                }
                String method = segments[0];
                String values = segments[1];
                if (StringUtils.hasLength(values)) {
                    String[] mediaTypes = values.split(",");
                    for (String mediaType : mediaTypes) {
                        try {
                            config.shortcutExclusions().exclude(method, MediaType.parseMediaType(mediaType));
                        } catch (InvalidMediaTypeException imte) {}
                    }
                }
            }
        }

        config.setOverwriteExistingContent(properties.getOverwriteExistingContent());
    }
}
