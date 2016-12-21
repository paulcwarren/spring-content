package org.springframework.content.mongo;

import java.io.InputStream;

public interface MongoContentOperations {

	void setContent(Object property, InputStream content);
	void unsetContent(Object property);
	InputStream getContent(Object property);

}
