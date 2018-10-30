package internal.org.springframework.content.jpa.io;

import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;

public class SQLServerBlobResource extends AbstractBlobResource {

	private static Log logger = LogFactory.getLog(SQLServerBlobResource.class);

	public SQLServerBlobResource(Object id, JdbcTemplate template, PlatformTransactionManager txnMgr) {
		super(id, template, txnMgr);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		final Object id = getId();

		String sql = getSelectBlobSQL(getId());

		DataSource ds = getTemplate().getDataSource();
		Connection conn = DataSourceUtils.getConnection(ds);
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			logger.error(format("setting autocommit to false whilst getting content %s", id), e);
		}
		InputStream is = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

			if (stmt.isWrapperFor(com.microsoft.sqlserver.jdbc.SQLServerStatement.class)) {
				SQLServerStatement SQLstmt = stmt.unwrap(com.microsoft.sqlserver.jdbc.SQLServerStatement.class);
				SQLstmt.setResponseBuffering("adaptive");
			}
			rs = stmt.executeQuery(sql);

			if (!rs.next())
				return null;
			is = rs.getBinaryStream(2);
		}
		catch (SQLException e) {
			logger.error(format("getting content %s", id), e);
		}
		return new ClosingInputStream(id, is, rs, stmt, conn, ds);
	}
}
