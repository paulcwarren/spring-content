package internal.org.springframework.content.mongo.repository;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.util.Assert;

import internal.org.springframework.content.mongo.MongoContentTemplate;

public class DefaultMongoContentRepositoryImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private MongoContentTemplate template;

	public DefaultMongoContentRepositoryImpl(MongoContentTemplate template) {
		this.template = template;
		Assert.notNull(this.template, "MongoContentTemplate cannot be null");
	}

	@Override
	public void setContent(S property, InputStream content) {
		this.template.setContent(property, content);
	}

	@Override
	public InputStream getContent(S property) {
		return this.template.getContent(property);
	}

	@Override
	public void unsetContent(S property) {
		this.template.unsetContent(property);
	}
}
