package internal.org.springframework.content.rest.utils;

import org.atteo.evo.inflector.English;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.util.StringUtils;

public final class RepositoryUtils {

	private RepositoryUtils() {}

	public static String repositoryPath(RepositoryInformation info) {
		Class<?> clazz = info.getRepositoryInterface();
		RepositoryRestResource annotation = AnnotationUtils.findAnnotation(clazz, RepositoryRestResource.class);
		String path = annotation == null ? null : annotation.path().trim();
		path = StringUtils.hasText(path) ? path : English.plural(StringUtils.uncapitalize(info.getDomainType().getSimpleName()));
		return path;
	}
	
}
