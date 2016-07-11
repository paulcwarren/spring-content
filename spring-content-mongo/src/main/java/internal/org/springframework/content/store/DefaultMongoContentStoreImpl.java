package internal.org.springframework.content.store;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.springframework.content.annotations.ContentId;
import org.springframework.content.annotations.ContentLength;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import com.mongodb.gridfs.GridFSFile;

public class DefaultMongoContentStoreImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private GridFsOperations gridOps;

	public DefaultMongoContentStoreImpl(GridFsOperations gridOps) {
		this.gridOps = gridOps;
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		// delete any existing content object (gridfsoperations doesn't support replace)
		gridOps.delete(query(whereFilename().is(contentId.toString())));

		GridFSFile savedContentFile = gridOps.store(content, contentId.toString());

		BeanUtils.setFieldWithAnnotation(property, ContentLength.class, savedContentFile.getLength());
	}

	@Override
	public InputStream getContent(S property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;
		GridFsResource resource = gridOps.getResource(contentId.toString());
		if (resource != null)
			try {
				return resource.getInputStream();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		return null;
	}

	@Override
	public void unsetContent(S property) {
		if (property == null)
			return;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return;

		// delete any existing content object
		gridOps.delete(query(whereFilename().is(contentId.toString())));

		// reset content fields
        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);

	}
}
