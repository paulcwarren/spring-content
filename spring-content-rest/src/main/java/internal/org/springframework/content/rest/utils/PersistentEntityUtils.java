package internal.org.springframework.content.rest.utils;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

public final class PersistentEntityUtils {

	private PersistentEntityUtils() {}
	
	public static PersistentEntity<?,?> findPersistentEntity(Repositories repositories, 
													  Class<?> domainType) {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(domainType);
		return persistentEntity;
	}
	
	public static PersistentProperty<?> getPersistentProperty(PersistentEntity<?,?> entity, String propertyName) {
		
		PersistentProperty<?> prop = entity.getPersistentProperty(propertyName);
		if (null == prop)
			throw new ResourceNotFoundException();
		
		return prop;
	}
	
	public static boolean isPropertyMultiValued(PersistentProperty<?> prop) {
		return (prop.isArray() || prop.isCollectionLike());
	}
}
