package internal.org.springframework.content.jpa.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

@ConfigurationProperties("spring.content.jpa")
public class ContentJpaProperties {

	private String schema = "optional:classpath:org/springframework/content/jpa/schema-@@platform@@.sql";

	private final ContentJpaProperties.Initializer initializer = new ContentJpaProperties.Initializer();

	private int copyBufferSize = 4096;

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

	public void setCopyBufferSize(int copyBufferSize) {
		this.copyBufferSize = copyBufferSize;
	}

	public int getCopyBufferSize() {
		return this.copyBufferSize;
	}
}
