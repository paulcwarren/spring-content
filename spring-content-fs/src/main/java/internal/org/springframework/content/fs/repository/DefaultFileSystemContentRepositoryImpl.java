package internal.org.springframework.content.fs.repository;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;

public class DefaultFileSystemContentRepositoryImpl<S, SID extends Serializable> /*extends AstractResourceContentRepositoryImpl<S,SID>*/  implements ContentStore<S,SID> {

	private FileResourceTemplate template;

	public DefaultFileSystemContentRepositoryImpl(FileResourceTemplate template) {
		this.template = template;
	}

	@Override
	public void setContent(S property, InputStream content) {
		this.template.setContent(property, content);
	}

	@Override
	public void unsetContent(S property) {
		this.template.unsetContent(property);
	}

	@Override
	public InputStream getContent(S property) {
		return this.template.getContent(property);
	}
}
