package internal.org.springframework.content.commons.repository.factory;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class StoreMethodInterceptor implements MethodInterceptor {

	private static final Log LOGGER = LogFactory.getLog(StoreMethodInterceptor.class);

	private StoreFragments storeFragments;
	private Map<Method, Method> methodCache = new ConcurrentReferenceHashMap<>();

	// ContentStoreAware methods
	private static Method setContentStoreMethod;

	static {
		setContentStoreMethod = ReflectionUtils.findMethod(ContentStoreAware.class, "setContentStore", ContentStore.class);
		Assert.notNull(setContentStoreMethod);
	}

	public StoreMethodInterceptor() {
	}

	public void setStoreFragments(StoreFragments storeFragments) {
		this.storeFragments = storeFragments;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		if (storeFragments != null) {
			Optional<StoreFragment> fragment = storeFragments.stream()
					.filter(it -> it.hasMethod(invocation.getMethod()))
					.findFirst();

			fragment.orElseThrow(() -> new IllegalStateException(format("No fragment found for method %s", invocation.getMethod())));

			StoreFragment f = fragment.get();
			if (f.hasImplementationMethod(setContentStoreMethod)) {
				ReflectionUtils.invokeMethod(setContentStoreMethod, f.getImplementation(), invocation.getThis());
			}

			return getMethod(invocation.getMethod(), f).invoke(fragment.get().getImplementation(), invocation.getArguments());
		}

		String msg = format("No fragment implementation found for invoked method %s", invocation);
		LOGGER.error(msg, new UnsupportedOperationException(msg));
		return null;
	}

	/* package */ Method getMethod(Method invokedMethod, StoreFragment fragment) {
		return methodCache.computeIfAbsent(invokedMethod,
				key -> resolveImplementationMethod(invokedMethod, fragment));
	}

	private Method resolveImplementationMethod(Method invokedMethod, StoreFragment fragment) {

		if (invokedMethod.getName().equals("getResource") && Serializable.class.isAssignableFrom(invokedMethod.getParameterTypes()[0])) {
			return ReflectionUtils.findMethod(fragment.getImplementation().getClass(), "getResource", Serializable.class);
		}

		for (Method candidate : fragment.getImplementation().getClass().getMethods()) {

			if (invokedMethod.getName().equals(candidate.getName()) &&
				parametersMatch(invokedMethod, candidate)
			) {
				return candidate;
			}
		}

		return null;
	}

	private boolean parametersMatch(Method invokedMethod, Method candidate) {

		if (invokedMethod.getParameterCount() != candidate.getParameterCount()) {
			return false;
		}

		Class<?>[] invokedMethodTypes = invokedMethod.getParameterTypes();
		Class<?>[] candidateMethodTypes = candidate.getParameterTypes();

		for (int i=0; i < invokedMethod.getParameterCount(); i++) {
			if (!candidateMethodTypes[i].isAssignableFrom(invokedMethodTypes[i])) {
				return false;
			}
		}

		return true;
	}
}
