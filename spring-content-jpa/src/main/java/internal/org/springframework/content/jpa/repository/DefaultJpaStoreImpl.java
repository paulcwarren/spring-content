package internal.org.springframework.content.jpa.repository;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.jpa.operations.JpaContentTemplate;

public class DefaultJpaStoreImpl<S, SID extends Serializable> implements ContentStore<S,SID> {

	private JpaContentTemplate template;
	
	public DefaultJpaStoreImpl(JpaContentTemplate template) {
		this.template = template;
	}

	@Override
	public void setContent(S metadata, InputStream content) {
		this.template.setContent(metadata, content);
	}

	@Override
	public void unsetContent(S metadata) {
		this.template.unsetContent(metadata);
	}

	@Override
	public InputStream getContent(S metadata) {
		return this.template.getContent(metadata);
	}

}
