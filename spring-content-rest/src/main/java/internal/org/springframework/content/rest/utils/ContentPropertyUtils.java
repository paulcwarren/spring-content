package internal.org.springframework.content.rest.utils;

import org.springframework.data.mapping.PersistentProperty;

public class ContentPropertyUtils {

	public static Class<?> getContentPropertyType(PersistentProperty<?> prop) {
		Class<?> contentEntityClass = null;
		
		// null single-valued content property
		if (!PersistentEntityUtils.isPropertyMultiValued(prop)) {
			contentEntityClass = prop.getActualType();
		} 
		// null multi-valued content property
		else if (PersistentEntityUtils.isPropertyMultiValued(prop)) {
			if (prop.isArray()) {
				contentEntityClass = prop.getComponentType();
			}
			else if (prop.isCollectionLike()) {
				contentEntityClass = prop.getActualType();
			}
		}
		
		return contentEntityClass;
	}
	
	private ContentPropertyUtils() {}
	
}
