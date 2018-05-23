package internal.org.springframework.content.jpa.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

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
        private DataSourceInitializationMode initializeSchema;

        public DataSourceInitializationMode getInitializeSchema() {
            return this.initializeSchema;
        }

        public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
            this.initializeSchema = initializeSchema;
        }
    }
}
