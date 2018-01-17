package internal.org.springframework.content.jpa.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InputStreamCallbackHandler implements PreparedStatementCallback<InputStream> {

    private static Log logger = LogFactory.getLog(InputStreamCallbackHandler.class);
    private final String columnName;
    private InputStream is;

    public InputStreamCallbackHandler(String columnName) {
        this.columnName = columnName;
    }

    public InputStream doInPreparedStatement(PreparedStatement ps) {
        ResultSet resultSet = null;
        try {
            resultSet = ps.executeQuery();
            if (!resultSet.next()) return null;
            Blob b = resultSet.getBlob(columnName);
            File tempFile = File.createTempFile("_sc_jpa_", null);
            FileOutputStream fos = new FileOutputStream(tempFile);
            InputStream bis = b.getBinaryStream();
            try {
                IOUtils.copyLarge(bis, fos);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(fos);
            }
            is = new ObservableInputStream(new FileInputStream(tempFile), new FileRemover(tempFile));
            return is;
        } catch (Exception e) {
            logger.error("Error fetchng blob from database", e);
            throw new StoreAccessException("Error fetching blob from database", e);
        } finally {
            if (resultSet != null)
                try {
                    resultSet.close();
                } catch (SQLException sqle) {
                    logger.error(String.format("Error closing resultset for blob"), sqle);
                }
        }
    }

    public InputStream getInputStream() {
        return is;
    }
}
