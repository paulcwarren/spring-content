package internal.org.springframework.content.jpa.io;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class BlobResourceOutputStream extends OutputStream {

	private static Log logger = LogFactory.getLog(BlobResourceOutputStream.class);

	private BlobResource blobResource;
	private JdbcTemplate template;

	private Connection con;
	private PreparedStatement ps;
	private ResultSet rs;
	private OutputStream os;
	private Blob blob;
	private boolean insert;
	
	public BlobResourceOutputStream(BlobResource blobResource, JdbcTemplate template) {
		this.blobResource = blobResource;
		this.template = template;
	}

	protected BlobResource getResource() {
		return blobResource;
	}
	
	protected JdbcTemplate getTemplate() {
		return template;
	}
	
	@Override
	public void write(int b) throws IOException {

		if (os == null) {
			os = initializeStream();
		}
		
		os.write(b);
	}
	
	protected OutputStream initializeStream() {
		
		final Object rid = blobResource.getId();
		con = DataSourceUtils.getConnection(template.getDataSource());

		String sql = "SELECT id, content FROM BLOBS WHERE id=?";
		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ps.setString(1, rid.toString());
			rs = ps.executeQuery();
			

			if (!rs.next()) {
				insert = true;
				rs.moveToInsertRow();
			} else { 
			}
			
			blob = ps.getConnection().createBlob(); 
			return blob.setBinaryStream(1L);
			
		} catch (SQLException e) {
			logger.error(String.format("intializing stream for blob resource %s", rid), e);
		}
		
		return null;
	}

	@Override
	public void close() throws IOException {
		
		IOUtils.closeQuietly(os);
		
		try {
			
			rs.updateString(1, blobResource.getId().toString());
			rs.updateBlob(2, blob);
			
			if (!insert) {
				rs.updateRow();
			} else {
				rs.insertRow();
			}
		} catch (SQLException e) {
			logger.error(String.format("closing stream for blob resource %s", blobResource.getId()), e);
		} finally {
			try {
				rs.close();
				ps.close();
				DataSourceUtils.releaseConnection(con, template.getDataSource());
			} catch (SQLException e) {
				logger.error(String.format("closing resources for blob resource %s", blobResource.getId()), e);
			}
		}
		
		super.close();
	}
	
	

	public Blob getBlob() { 
		String sql = "SELECT id, content FROM BLOBS WHERE id='" + blobResource.getId() + "'";
		return this.template.query(sql, new ResultSetExtractor<Blob>() {
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
