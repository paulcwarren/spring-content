package internal.org.springframework.versions.jpa.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

@ConfigurationProperties("spring.versions.jpa")
public class JpaVersionsProperties {

	private String schema = "optional:classpath:org/springframework/versions/jpa/schema-@@platform@@.sql";

	private final JpaVersionsProperties.Initializer initializer = new JpaVersionsProperties.Initializer();

	public Initializer getInitializer() {
		return initializer;
	}

	public String getSchema() {
		return schema;
	}

	public class Initializer {
		private DatabaseInitializationMode initializeSchema;

		public DatabaseInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}
	}
}
