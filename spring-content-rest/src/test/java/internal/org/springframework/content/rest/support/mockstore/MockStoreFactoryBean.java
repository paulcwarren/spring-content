package internal.org.springframework.content.rest.support.mockstore;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

import java.io.InputStream;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockStoreFactoryBean extends AbstractStoreFactoryBean {

	private ContentStore mock;

	public ContentStore getMock() {
		return mock;
	}

	@Override
	protected Object getContentStoreImpl() {
		mock = mock(ContentStore.class);
		when(mock.setContent(anyObject(), (InputStream)anyObject())).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return args[0];
			}
		});
		when(mock.setContent(anyObject(), (Resource)anyObject())).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return args[0];
			}
		});
		return mock;
	}
}
