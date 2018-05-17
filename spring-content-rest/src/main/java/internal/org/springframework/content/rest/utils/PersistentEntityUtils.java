package internal.org.springframework.content.rest.utils;

import org.springframework.data.mapping.PersistentProperty;

public final class PersistentEntityUtils {

	private PersistentEntityUtils() {}
	
	public static boolean isPropertyMultiValued(PersistentProperty<?> prop) {
		return (prop.isArray() || prop.isCollectionLike());
	}
}
