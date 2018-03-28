package internal.org.springframework.content.jpa.config;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.repository.DefaultJpaStoreImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.util.Assert;

@SuppressWarnings("rawtypes")
public class JpaStoreFactoryBean extends AbstractStoreFactoryBean {

	@Autowired
	private DelegatingBlobResourceLoader blobResourceLoader;

	@Override
	protected Object getContentStoreImpl() {
		Assert.notNull(blobResourceLoader, "blobResourceLoader cannot be null");
		return new DefaultJpaStoreImpl(blobResourceLoader);
	}

}
