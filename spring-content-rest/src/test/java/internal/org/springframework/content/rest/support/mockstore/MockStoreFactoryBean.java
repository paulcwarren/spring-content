package internal.org.springframework.content.rest.support.mockstore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

import org.springframework.content.commons.property.PropertyPath;

public class MockStoreFactoryBean extends AbstractStoreFactoryBean {

	private ContentStore mock;

	protected MockStoreFactoryBean(Class<? extends Store> storeInterface) {
		super(storeInterface);
	}

	public ContentStore getMock() {
		return mock;
	}

	@Override
	protected Object getContentStoreImpl() {
		mock = mock(ContentStore.class);
		when(mock.setContent(any(), any(InputStream.class))).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return args[0];
			}
		});
		when(mock.setContent(any(), any(Resource.class))).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return args[0];
			}
		});
		when(mock.setContent(any(), any(PropertyPath.class), any(Resource.class))).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return args[0];
			}
		});
		return mock;
	}
}
