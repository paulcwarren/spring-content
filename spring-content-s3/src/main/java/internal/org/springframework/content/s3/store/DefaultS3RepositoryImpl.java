	package internal.org.springframework.content.s3.store;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.s3.operations.S3ResourceTemplate;

public class DefaultS3RepositoryImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private S3ResourceTemplate template;

	public DefaultS3RepositoryImpl(S3ResourceTemplate template) {
		this.template = template;
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
