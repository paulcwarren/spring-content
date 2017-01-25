package org.springframework.content.commons.utils;

import java.lang.reflect.Method;

public interface ReflectionService {
	Object invokeMethod(Method method, Object target, Object... args);
}
