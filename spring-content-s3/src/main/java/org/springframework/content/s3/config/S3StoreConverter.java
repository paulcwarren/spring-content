package org.springframework.content.s3.config;

import org.springframework.core.convert.converter.Converter;

public interface S3StoreConverter<S, T> extends Converter<S, T> {
	//
}
