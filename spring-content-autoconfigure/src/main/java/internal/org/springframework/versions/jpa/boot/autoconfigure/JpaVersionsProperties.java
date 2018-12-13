package internal.org.springframework.versions.jpa.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

@ConfigurationProperties("spring.versions.jpa")
public class JpaVersionsProperties {

	private String schema = "classpath:org/springframework/versions/jpa/schema-@@platform@@.sql";

	private final JpaVersionsProperties.Initializer initializer = new JpaVersionsProperties.Initializer();

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
