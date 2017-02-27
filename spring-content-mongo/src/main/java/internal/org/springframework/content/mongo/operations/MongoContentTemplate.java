package internal.org.springframework.content.mongo.operations;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.operations.AbstractResourceTemplate;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

public class MongoContentTemplate extends AbstractResourceTemplate {

	private GridFsTemplate gridFs;

	@Autowired
	public MongoContentTemplate(GridFsTemplate gridFs) {
		super(gridFs);
		this.gridFs = gridFs;
	}

	public Resource create(String location, InputStream content) {
		gridFs.store(content, location);
		return this.get(location);
	}

	@Override
	public String getLocation(Object contentId) {
		return contentId.toString();
	}

	@Override
	protected void deleteResource(Resource resource) throws Exception {
		gridFs.delete(query(whereFilename().is(resource.getFilename())));
	}
}
