package org.springframework.content.jpa.io;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import internal.org.springframework.content.jpa.io.BlobResourceOutputStream;

public abstract class AbstractBlobResource implements BlobResource {

    private static Log logger = LogFactory.getLog(AbstractBlobResource.class);

    private Object id;
    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;

    public AbstractBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
        this.id = id;
        this.template = template;
        this.txnMgr = txnMgr;
    }

    @Override
    public Object getId() {
        synchronized (id) {
            return id;
        }
    }

    protected void setId(Object id) {
        synchronized (id) {
            this.id = id;
        }
    }

    protected JdbcTemplate getTemplate() {
        return template;
    }

    protected PlatformTransactionManager getTransactionManager() {
        return txnMgr;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(new BlobResourceOutputStream(this, template), 10);
    }

    @Override
    public boolean exists() {

        final Object id = this.id;

        String sql = "SELECT COUNT(id) FROM BLOBS WHERE id='" + this.id + "'";

        return this.template.query(sql, new ResultSetExtractor<Boolean>() {
            @Override
            public Boolean extractData(ResultSet rs) {
                try {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        return count == 1;
                    }
                } catch (SQLException sqle) {
                    logger.warn(format("checking existence of blob resource %s", id), sqle);
                }

                return false;
            }
        });
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        return null;
    }

    @Override
    public URI getURI() throws IOException {
        return null;
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public long contentLength() throws IOException {
        return 0;
    }

    @Override
    public long lastModified() throws IOException {
        return 0;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return null;
    }

    @Override
    public String getFilename() {
        return id.toString();
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        final Object id = this.id;

        String sql = getSelectBlobSQL(this.id);

        DataSource ds = this.template.getDataSource();
        Connection conn = DataSourceUtils.getConnection(ds);

        InputStream is = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                try {
                    rs.close();
                    stmt.close();
                    DataSourceUtils.releaseConnection(conn, ds);
                    return null;
                } catch (SQLException sqle) {
                    logger.debug(format("failed to release database connection getting input stream for blob resource %s", id), sqle);
                }
            }

            Blob b = rs.getBlob(2);
            is = b.getBinaryStream();
        }
        catch (SQLException e) {
            logger.error(format("getting input stream for blob resource %s", id), e);
            return null;
        }

        return new ClosingInputStream(id, is, rs, stmt, null, getTransactionManager(), conn, ds);
    }

    @Override
    public void delete() throws IOException {
        String sql = "DELETE FROM BLOBS WHERE id='" + this.id + "'";
        this.template.update(sql);
    }


    protected String getSelectBlobSQL(Object id) {
        return "SELECT id, content FROM BLOBS WHERE id='" + id + "'";
    }

    public class ClosingInputStream extends InputStream {

        private Object id;
        private InputStream actual;
        private ResultSet rs;
        private Statement stmt;
        private TransactionStatus txnStatus;
        private PlatformTransactionManager txnMgr;
        private Connection conn;
        private DataSource ds;

        public ClosingInputStream(Object id, InputStream actual, ResultSet rs, Statement stmt, TransactionStatus txnStatus, PlatformTransactionManager txnMgr, Connection conn, DataSource ds) {
            this.id = id;
            this.actual = actual;
            this.rs = rs;
            this.stmt = stmt;
            this.txnStatus = txnStatus;
            this.txnMgr = txnMgr;
            this.conn = conn;
            this.ds = ds;
        }

        @Override
        public int read() throws IOException {
            try {
                return actual.read();
            } catch (IOException ioe) {
              if (txnStatus != null && txnStatus.isCompleted() == false) {
                  txnMgr.rollback(txnStatus);
              }
              throw ioe;
            }
        }

        @Override
        public void close() {

            try {
                try {
                    try {
                        try {
                            try {
                                try {
                                }
                                finally {
                                    try {
                                        actual.close();
                                    }
                                    catch (IOException e) {
                                        logger.debug(format("closing stream for blob resource %s", id),
                                                e);
                                    }
                                }
                            }
                            finally {
                                try {
                                    rs.close();
                                }
                                catch (SQLException e) {
                                    logger.debug(format("closing resultset for blob resource %s", id),
                                            e);
                                }
                            }
                        }
                        finally {
                            try {
                                stmt.close();
                            }
                            catch (SQLException e) {
                                logger.debug(format("closing statement for blob resource %s", id), e);
                            }
                        }
                    }
                    finally {
                        if (txnStatus != null && txnStatus.isCompleted() == false) {
                            txnMgr.commit(txnStatus);
                        }
                    }
                }
                finally {
                    DataSourceUtils.releaseConnection(conn, ds);
                }
            }
            finally {
            }
        }
    }
}
