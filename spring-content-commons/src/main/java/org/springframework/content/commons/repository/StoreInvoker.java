package org.springframework.content.commons.repository;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.core.io.Resource;

public interface StoreInvoker {

	Class<?> getDomainClass();

	Class<? extends Serializable> getContentIdClass();

	InputStream invokeGetContent();

	Resource invokeGetResource();

}
