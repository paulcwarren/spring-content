package internal.org.springframework.content.jpa.boot.autoconfigure;

import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import java.sql.DatabaseMetaData;

import javax.sql.DataSource;
import java.util.Collections;

public class ContentJpaDatabaseInitializer extends SqlDataSourceScriptDatabaseInitializer {

	public ContentJpaDatabaseInitializer(DataSource ds, ContentJpaProperties properties) {
		super(ds, getSettings(properties, ds));
	}

    private static DatabaseInitializationSettings getSettings(ContentJpaProperties properties, DataSource dataSource) {
        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(Collections.singletonList(properties.getSchema().replace("@@platform@@", getDatabaseName(dataSource))));
//        settings.setDataLocations(scriptLocations(this.properties.getDataLocations(), "data", this.properties.getPlatform()));
        settings.setContinueOnError(false);
        settings.setMode(properties.getInitializer().getInitializeSchema());
        return settings;
    }

    private static String getDatabaseName(DataSource dataSource) {
        try {
            String productName = JdbcUtils.commonDatabaseName(JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName));
            DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(productName);
            if (databaseDriver == DatabaseDriver.UNKNOWN) {
                throw new IllegalStateException("Unable to detect database type");
            }
            return databaseDriver.getId();
        }
        catch (MetaDataAccessException ex) {
            throw new IllegalStateException("Unable to detect database type", ex);
        }
    }
}
