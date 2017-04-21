package org.springframework.content.mongo.config;

import org.springframework.core.convert.converter.Converter;

public interface MongoStoreConverter<S,T> extends Converter<S, T> {
	//
}
