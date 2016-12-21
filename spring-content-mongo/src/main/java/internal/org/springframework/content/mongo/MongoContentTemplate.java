package internal.org.springframework.content.mongo;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.mongo.MongoContentOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.mongodb.gridfs.GridFSFile;

public class MongoContentTemplate implements MongoContentOperations {

	private GridFsTemplate gridFs;

	@Autowired
	public void setGridFs(GridFsTemplate gridFs) {
		this.gridFs = gridFs;
	}

	@Override
	public void setContent(Object property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId.toString());
		}

		// delete any existing content object (gridfsoperations doesn't support replace)
		gridFs.delete(query(whereFilename().is(contentId.toString())));

		GridFSFile savedContentFile = gridFs.store(content, contentId.toString());

		BeanUtils.setFieldWithAnnotation(property, ContentLength.class, savedContentFile.getLength());
	}

	@Override
	public InputStream getContent(Object property) {
		if (property == null)
			return null;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return null;
		GridFsResource resource = gridFs.getResource(contentId.toString());
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
	public void unsetContent(Object property) {
		if (property == null)
			return;
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null)
			return;

		// delete any existing content object
		gridFs.delete(query(whereFilename().is(contentId.toString())));

		// reset content fields
        BeanUtils.setFieldWithAnnotation(property, ContentId.class, null);
        BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
	}
}
