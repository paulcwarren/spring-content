package internal.org.springframework.content.commons.repository.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import internal.org.springframework.content.commons.repository.StoreInvokerImpl;
import lombok.Getter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.events.AfterAssociateEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnassociateEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeAssociateEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnassociateEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static java.lang.String.format;

public class StoreMethodInterceptor implements MethodInterceptor {

	private Map<Method, StoreExtension> extensions;
	private ContentStore<Object, Serializable> store = null;
	private ApplicationEventPublisher publisher;
	private StoreFragments storeFragments;

	// Store methods
	private static Method getContentMethod;
	private static Method setContentMethod;
	private static Method unsetContentMethod;
	private static Method getResourceMethod;
	private static Method associativeGetResourceMethod;
	private static Method associateResourceMethod;
	private static Method unassociateResourceMethod;
	private static Method toStringMethod;

	// ContentStoreAware methods
	private static Method setContentStoreMethod;

	private Class<?> domainClass = null;
	private Class<? extends Serializable> contentIdClass = null;

	static {
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent",
				Object.class);
		Assert.notNull(getContentMethod);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent",
				Object.class, InputStream.class);
		Assert.notNull(setContentMethod);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class,
				"unsetContent", Object.class);
		Assert.notNull(unsetContentMethod);
		getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource",
				Serializable.class);
		Assert.notNull(getResourceMethod);
		associativeGetResourceMethod = ReflectionUtils.findMethod(AssociativeStore.class,
				"getResource", Object.class);
		Assert.notNull(associativeGetResourceMethod);
		associateResourceMethod = ReflectionUtils.findMethod(AssociativeStore.class,
				"associate", Object.class, Serializable.class);
		Assert.notNull(getResourceMethod);
		unassociateResourceMethod = ReflectionUtils.findMethod(AssociativeStore.class,
				"unassociate", Object.class);
		Assert.notNull(getResourceMethod);
		toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
		Assert.notNull(toStringMethod);

		setContentStoreMethod = ReflectionUtils.findMethod(ContentStoreAware.class, "setContentStore", ContentStore.class);
		Assert.notNull(setContentStoreMethod);
	}

	public StoreMethodInterceptor(ContentStore<Object, Serializable> store,
			Class<?> domainClass, Class<? extends Serializable> contentIdClass,
			Map<Method, StoreExtension> extensions, ApplicationEventPublisher publisher) {
		if (extensions == null) {
			extensions = Collections.<Method, StoreExtension>emptyMap();
		}
		this.store = store;
		this.domainClass = domainClass;
		this.contentIdClass = contentIdClass;
		this.extensions = extensions;
		this.publisher = publisher;
	}

	public void setStoreFragments(StoreFragments storeFragments) {
		this.storeFragments = storeFragments;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		if (!isStoreMethod(invocation) && storeFragments != null) {
			Optional<StoreFragment> fragment = storeFragments.stream()
					.filter(it -> it.hasMethod(invocation.getMethod()))
					.findFirst();

			fragment.orElseThrow(() -> new IllegalStateException(format("No fragment found for method %s", invocation.getMethod())));

			StoreFragment f = fragment.get();
			if (f.hasImplementationMethod(setContentStoreMethod)) {
				ReflectionUtils.invokeMethod(setContentStoreMethod, f.getImplementation(), invocation.getThis());
			}

			return invocation.getMethod().invoke(fragment.get().getImplementation(), invocation.getArguments());
		} else {
			Method method = invocation.getMethod();
			StoreExtension extension = extensions.get(method);
			if (extension != null) {
				return extension.invoke(invocation,
						new StoreInvokerImpl(domainClass, contentIdClass, invocation));
			}
			else {
				if (!isStoreMethod(invocation)) {
					throw new StoreAccessException(format("No implementation found for %s", method.getName()));
				}
			}
		}

		StoreEvent before = null;
		AfterStoreEvent after = null;

		File tmpStreamFile = null;
		TeeInputStream eventStream = null;

		if (getContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				before = new BeforeGetContentEvent(invocation.getArguments()[0], store);
				after = new AfterGetContentEvent(invocation.getArguments()[0], store);
			}
		}
		else if (setContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				tmpStreamFile = Files.createTempFile("sc", "bsce").toFile();
				eventStream = new TeeInputStream((InputStream) invocation.getArguments()[1], new FileOutputStream(tmpStreamFile), true);
				before = new BeforeSetContentEvent(invocation.getArguments()[0], store, eventStream);
				after = new AfterSetContentEvent(invocation.getArguments()[0], store);
			}
		}
		else if (unsetContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0
					&& invocation.getArguments()[0] != null) {
				before = new BeforeUnsetContentEvent(invocation.getArguments()[0], store);
				after = new AfterUnsetContentEvent(invocation.getArguments()[0], store);
			}
		}
		else if (getResourceMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0
					&& invocation.getArguments()[0] != null) {
				before = new BeforeGetResourceEvent(invocation.getArguments()[0], store);
				after = new AfterGetResourceEvent(invocation.getArguments()[0], store);
			}
		}
		else if (associativeGetResourceMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0
					&& invocation.getArguments()[0] != null) {
				before = new BeforeGetResourceEvent(invocation.getArguments()[0], store);
				after = new AfterGetResourceEvent(invocation.getArguments()[0], store);
			}
		}
		else if (associateResourceMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0
					&& invocation.getArguments()[0] != null) {
				before = new BeforeAssociateEvent(invocation.getArguments()[0], store);
				after = new AfterAssociateEvent(invocation.getArguments()[0], store);
			}
		}
		else if (unassociateResourceMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0
					&& invocation.getArguments()[0] != null) {
				before = new BeforeUnassociateEvent(invocation.getArguments()[0], store);
				after = new AfterUnassociateEvent(invocation.getArguments()[0], store);
			}
		}

		if (before != null) {
			publisher.publishEvent(before);

			if (before instanceof BeforeSetContentEvent) {

				if (eventStream.isDirty()) {
					while (eventStream.read(new byte[4096]) != -1) {}
					eventStream.close();
					invocation.getArguments()[1] = new ObservableInputStream(new FileInputStream(tmpStreamFile), new FileRemover(tmpStreamFile));
				}
			}

		}
		Object result;
		try {
			result = invocation.proceed();
		}
		catch (Exception e) {
			throw e;
		}

		if (after != null) {
			after.setResult(result);
			publisher.publishEvent(after);
		}
		return result;
	}

	private boolean isStoreMethod(MethodInvocation invocation) {
		if (getContentMethod.equals(invocation.getMethod())
		 || setContentMethod.equals(invocation.getMethod())
		 || unsetContentMethod.equals(invocation.getMethod())
		 || getResourceMethod.equals(invocation.getMethod())
		 || associativeGetResourceMethod.equals(invocation.getMethod())
		 || associateResourceMethod.equals(invocation.getMethod())
		 || unassociateResourceMethod.equals(invocation.getMethod())
		 || toStringMethod.equals(invocation.getMethod())) {
			return true;
		}
		return false;
	}

	@Getter
	private static class TeeInputStream extends org.apache.commons.io.input.TeeInputStream {

		private boolean isDirty = false;

		public TeeInputStream(InputStream input, OutputStream branch, boolean closeBranch) {
			super(input, branch, closeBranch);
		}

		@Override
		public int read() throws IOException {
			isDirty = true;
			return super.read();
		}

		@Override
		public int read(byte[] bts, int st, int end) throws IOException {
			isDirty = true;
			return super.read(bts, st, end);
		}

		@Override
		public int read(byte[] bts) throws IOException {
			isDirty = true;
			return super.read(bts);
		}
	}
}
