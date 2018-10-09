package org.springframework.content.gcs.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.springframework.content.gcs.GCSObjectIdResolver;

public class GCSObjectIdResolvers extends ArrayList<GCSObjectIdResolver> {

	private static final long serialVersionUID = 1L;

	public GCSObjectIdResolver getResolverFor(Class<?> idOrEntityType) {
		if (idOrEntityType == null) {
			return null;
		}

		for (GCSObjectIdResolver candidate : this) {
			Type[] types = candidate.getClass().getGenericInterfaces();

			for (Type t : types) {
				if (t instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) t;
					types = pt.getActualTypeArguments();
					if (types.length == 1) {
						if (types[0].equals(idOrEntityType)) {
							return candidate;
						}
					}
				}
			}
		}
		return null;
	}

}
