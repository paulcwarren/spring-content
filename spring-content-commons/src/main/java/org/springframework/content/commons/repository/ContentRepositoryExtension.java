package org.springframework.content.commons.repository;

import java.lang.reflect.Method;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;

public interface ContentRepositoryExtension {
	Set<Method> getMethods();
	Object invoke(MethodInvocation invocation, ContentRepositoryInvoker invoker);
}
