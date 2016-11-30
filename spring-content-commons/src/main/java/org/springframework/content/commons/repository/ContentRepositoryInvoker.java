package org.springframework.content.commons.repository;

import java.io.InputStream;

public interface ContentRepositoryInvoker {

	InputStream invokeGetContent();

}
