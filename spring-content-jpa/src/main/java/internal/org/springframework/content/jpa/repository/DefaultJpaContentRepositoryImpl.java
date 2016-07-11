package internal.org.springframework.content.jpa.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.content.annotations.ContentId;
import org.springframework.content.annotations.ContentLength;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.utils.BeanUtils;

import internal.org.springframework.content.jpa.utils.InputStreamEx;

public class DefaultJpaContentRepositoryImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private EntityManager manager;
	private DataSource datasource;
	
	public DefaultJpaContentRepositoryImpl(EntityManager manager, DataSource datasource) {
		this.manager = manager;
		this.datasource = datasource;
	}

	@Override
	public void setContent(S property, InputStream content) {
		if (BeanUtils.getFieldWithAnnotation(property, ContentId.class) == null) {
			
			String sql = "INSERT INTO BLOBS VALUES(NULL, ?);";
			try {
				PreparedStatement pS = datasource.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				InputStreamEx in = new InputStreamEx(content);
				pS.setBinaryStream(1, in);
				pS.executeUpdate();
		        ResultSet set = pS.getGeneratedKeys();
		        set.next();
		        int id = set.getInt("ID");
		        BeanUtils.setFieldWithAnnotation(property, ContentId.class, id);
		        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, in.getLength());
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} else {
			String sql = "UPDATE BLOBS SET blob=? WHERE id=" + BeanUtils.getFieldWithAnnotation(property, ContentId.class);
			try {
				PreparedStatement pS = datasource.getConnection().prepareStatement(sql);
				InputStreamEx in = new InputStreamEx(content);
				pS.setBinaryStream(1, in);
				pS.executeUpdate();
		        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, in.getLength());
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void unsetContent(S property) {
		String sql = "DELETE FROM BLOBS WHERE id=" + BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		try {
			PreparedStatement pS = datasource.getConnection().prepareStatement(sql);
			pS.executeUpdate();
	        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
	        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public InputStream getContent(S property) {
		String sql = "SELECT blob FROM BLOBS WHERE id='" + BeanUtils.getFieldWithAnnotation(property, ContentId.class) + "'";
		ResultSet set = null;
		try {
	        set = datasource.getConnection().prepareCall(sql).executeQuery();
	        if(!set.next()) return null;
	        Blob b = set.getBlob("blob");           
	        return b.getBinaryStream();
	        
	        // todo: wrap input stream so we can 'free' the blob
	        //b.free();
	    } catch (SQLException e) {
			e.printStackTrace();
		} finally {
	    	if (set != null)
				try {
					set.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    }
		return null;
	}

}
