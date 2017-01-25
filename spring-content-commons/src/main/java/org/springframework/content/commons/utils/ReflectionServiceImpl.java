package org.springframework.content.commons.utils;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

public class ReflectionServiceImpl implements ReflectionService {

	public ReflectionServiceImpl() {
	}

	@Override
	public Object invokeMethod(Method method, Object target, Object... args) {
		return ReflectionUtils.invokeMethod(method, target, args);
	}
}
