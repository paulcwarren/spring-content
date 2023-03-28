package org.springframework.content.commons.repository;

import java.lang.reflect.Method;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;

/**
 * @deprecated This class is deprecated. Use fragments instead.
 */
public interface StoreExtension {
	Set<Method> getMethods();

	Object invoke(MethodInvocation invocation, StoreInvoker invoker);
}
