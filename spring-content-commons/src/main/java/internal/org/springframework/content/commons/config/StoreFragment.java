package internal.org.springframework.content.commons.config;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

@Getter
@Setter
public class StoreFragment<T> {

	private final Class<T> iface;
	private final T implementation;

	public StoreFragment(Class<T> iface, T implementation) {
		this.iface = iface;
		this.implementation = implementation;
	}

	public boolean hasMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return ReflectionUtils.findMethod(iface, method.getName(), method.getParameterTypes()) != null;
	}

	public boolean hasImplementationMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return ReflectionUtils.findMethod(implementation.getClass(), method.getName(), method.getParameterTypes()) != null;
	}
}
