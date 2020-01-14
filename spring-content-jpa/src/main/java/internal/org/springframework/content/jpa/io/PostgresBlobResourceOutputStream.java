package internal.org.springframework.content.jpa.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.content.jpa.io.AbstractBlobResource;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class PostgresBlobResourceOutputStream extends BlobResourceOutputStream {

	private static Log logger = LogFactory.getLog(PostgresBlobResourceOutputStream.class);

	private Connection con;
	private PreparedStatement ps;
	private ResultSet rs;
	private OutputStream os;
	private LargeObject lo;
	private boolean insert;
	
	public PostgresBlobResourceOutputStream(BlobResource blobResource, JdbcTemplate template) {
		super(blobResource, template);
	}
	
	@Override
	protected OutputStream initializeStream() {
		
		final Object rid = this.getResource().getId();
		con = DataSourceUtils.getConnection(this.getTemplate().getDataSource());

		String sql = "SELECT id, content FROM BLOBS WHERE id=?";
		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ps.setString(1, rid.toString());
			rs = ps.executeQuery();

			if (!rs.next()) {
				insert = true;
				rs.moveToInsertRow();
			} 
			
			LargeObjectManager lobj = ((org.postgresql.PGConnection)con).getLargeObjectAPI();
			long oid = lobj.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
			lo = lobj.open(oid);
			return lo.getOutputStream();
			
		} catch (SQLException e) {
			logger.error(String.format("initializing postgres blob output stream for resource: %s", rid), e);
		}
		
		return null;
	}

	@Override
	public void close() throws IOException {
		
		Object rid = this.getResource().getId();
		
		IOUtils.closeQuietly(os);
		
		try {
			lo.close();
		} catch (SQLException e) {
			logger.error(String.format("closing large object for resource %s", rid));
		}
		
		Long oidToBeRemoved = null;
		try {
			
			rs.updateString(1, rid.toString());
			
			if (!insert) {
				oidToBeRemoved = rs.getLong(2);
			}
				
			rs.updateLong(2,  lo.getOID());
			
			if (!insert) {
				rs.updateRow();
			} else {
				rs.insertRow();
			}
		} catch (SQLException e) {
			logger.error(String.format("updating large object for resource %s", rid));
		} finally {
			try {
				rs.close();
				ps.close();
				
				if (oidToBeRemoved != null) {
					deleteReplacedLO(con, oidToBeRemoved);
				}
				
				DataSourceUtils.releaseConnection(con, this.getTemplate().getDataSource());
			} catch (SQLException e) {
				logger.error(String.format("finalizing large object for resource %s", rid));
			}
			
		}
	}

	
	private void deleteReplacedLO(Connection con2, Long oidToBeRemoved) {
		LargeObjectManager lobj;
		try {
			lobj = ((org.postgresql.PGConnection)con).getLargeObjectAPI();
			lobj.delete(oidToBeRemoved);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Blob getBlob() { 
		String sql = "SELECT id, content FROM BLOBS WHERE id='" + this.getResource().getId() + "'";
		return this.getTemplate().query(sql, new ResultSetExtractor<Blob>() {
			@Override
			public Blob extractData(ResultSet rs)
					throws SQLException, DataAccessException {
				if (rs.next()) {
					return rs.getBlob(1);
				}
				else {
					return null;
				}
			}
		});
	}
}
