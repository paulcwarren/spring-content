package internal.org.springframework.content.jpa.io;

import internal.org.springframework.content.jpa.utils.InputStreamEx;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCountCallbackHandler;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.sql.*;

public class MySQLBlobResource implements BlobResource {

    private static Log logger = LogFactory.getLog(MySQLBlobResource.class);

    private final String id;
    private final JdbcTemplate template;
    private JdbcTemplateServices templateServices;

    public MySQLBlobResource(String id, JdbcTemplate template) {
        this.id = id;
        this.template = template;
        this.templateServices = new JdbcTemplateServicesImpl();
    }

    public MySQLBlobResource(String id, JdbcTemplate template, JdbcTemplateServices templateServices) {
        this.id = id;
        this.template = template;
        this.templateServices = templateServices;
    }

    @Override
    public boolean exists() {
        String sql = "SELECT id FROM BLOBS WHERE id='" + this.id + "'";
        RowCountCallbackHandler counter = templateServices.newRowCountCallbackHandler();
        this.template.query(sql, counter);
        return (counter.getRowCount() == 1);
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getURI() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getFile() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long contentLength() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long lastModified() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFilename() {
        return this.id;
    }

    @Override
    public String getDescription() {
        return "blob [" + this.id + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStreamCallbackHandler handler = this.templateServices.newInputStreamCallbackHandler("blob");
        String sql = "SELECT blob FROM BLOBS WHERE id='" + this.id + "'";
        this.template.execute(sql, handler);
        return handler.getInputStream();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        final PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        final JdbcTemplate db = this.template;
        new Thread(
                new Runnable(){
                    public void run(){
                        String sql = "INSERT INTO BLOBS VALUES(NULL, ?);";
                        db.execute(
                        new PreparedStatementCreator() {
                            @Override
                            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                                return con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                            }
                        }, new PreparedStatementCallback<Integer>() {
                            @Override
                            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                                ResultSet set = null;
                                int id = 0;
                                int rc = 0;
                                try {
                                    InputStreamEx inex = new InputStreamEx(in);
                                    ps.setBinaryStream(1, inex);
                                    rc = ps.executeUpdate();
                                    set = ps.getGeneratedKeys();
                                    set.next();
                                    id = set.getInt("ID");
//                                    BeanUtils.setFieldWithAnnotation(metadata, ContentId.class, id);
//                                    BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, in.getLength());
                                    return rc;
                                } catch (SQLException sqle) {
                                    logger.error("Error inserting content", sqle);
                                } catch (Exception e) {
                                    logger.error("Error inserting content", e);
                                } catch (Throwable t) {
                                    logger.error("Error inserting content", t);
                                } finally {
                                    if (set != null) {
                                        try {
                                            set.close();
                                        } catch (SQLException e) {
                                            logger.error(String.format("Unexpected error closing result set for content id %s", id));
                                        }
                                    }
                                }
                                return rc;
                            }

                        });
                    }
                }
        ).start();

        return out;
    }
}
