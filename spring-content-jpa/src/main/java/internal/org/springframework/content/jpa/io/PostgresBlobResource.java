package internal.org.springframework.content.jpa.io;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class PostgresBlobResource extends AbstractBlobResource {

	private static Log logger = LogFactory.getLog(PostgresBlobResource.class);

	public PostgresBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
		super(id, template, txnMgr);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		final Object id = this.getId();

		String sql = getSelectBlobSQL(id);

		TransactionStatus status = null;
		if (getTransactionManager() != null) {
		    status = getTransactionManager().getTransaction(new DefaultTransactionDefinition());
		}

		DataSource ds = this.getTemplate().getDataSource();
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
					logger.debug(format("failed to release database connection while fetching content %s", id), sqle);
				}
			}

			LargeObjectManager lobj = conn.unwrap(org.postgresql.PGConnection.class).getLargeObjectAPI();
			long oid = rs.getLong(2);
			LargeObject obj = lobj.open(oid, LargeObjectManager.READ);
			is = obj.getInputStream(-1);
		}
		catch (SQLException e) {
			logger.error(format("getting content %s", id), e);
			return null;
		}

		return new ClosingInputStream(id, is, rs, stmt, status, getTransactionManager(), conn, ds);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new BufferedOutputStream(new PostgresBlobResourceOutputStream(this, this.getTemplate()), 10);
	}

    @Override
    public void delete()
        throws IOException {

        final Object id = this.getId();
        String sql = getSelectBlobSQL(id);

        DataSource ds = this.getTemplate().getDataSource();
        Connection conn = DataSourceUtils.getConnection(ds);

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
                    return;
                } catch (SQLException sqle) {
                    logger.debug(format("failed to release database connection while fetching content %s", id), sqle);
                }
            }

            LargeObjectManager lobj = conn.unwrap(org.postgresql.PGConnection.class).getLargeObjectAPI();
            long oid = rs.getLong(2);
            lobj.delete(oid);
        }
        catch (SQLException e) {
            logger.error(format("deleting content %s", id), e);
            return;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                logger.error(format("closing resources after deleting content %s", id), e);
            }
        }

        super.delete();
    }
}
