package internal.org.springframework.content.jpa.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.content.jpa")
public class ContentJpaProperties {

    private String schema = "classpath:org/springframework/content/jpa/schema-@@platform@@.sql";

    private final ContentJpaProperties.Initializer initializer = new ContentJpaProperties.Initializer();

    public Initializer getInitializer() {
        return initializer;
    }

    public String getSchema() {
        return schema;
    }

    public class Initializer {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
