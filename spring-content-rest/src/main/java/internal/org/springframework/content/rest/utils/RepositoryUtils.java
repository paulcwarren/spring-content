package internal.org.springframework.content.rest.utils;

import org.atteo.evo.inflector.English;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.util.StringUtils;

import java.util.Optional;

public final class RepositoryUtils {

	private RepositoryUtils() {
	}

	public static String repositoryPath(RepositoryInformation info) {
		Class<?> clazz = info.getRepositoryInterface();
		RepositoryRestResource annotation = AnnotationUtils.findAnnotation(clazz,
				RepositoryRestResource.class);
		String path = annotation == null ? null : annotation.path().trim();
		path = StringUtils.hasText(path) ? path : English
				.plural(StringUtils.uncapitalize(info.getDomainType().getSimpleName()));
		return path;
	}

	public static RepositoryInformation findRepositoryInformation(
			Repositories repositories, String repository) {
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			Optional<RepositoryInformation> candidate = repositories
					.getRepositoryInformationFor(clazz);
			if (candidate.isPresent() == false) {
				continue;
			}
			if (repository.equals(repositoryPath(candidate.get()))) {
				ri = candidate.get();
				break;
			}
		}
		return ri;
	}

	public static RepositoryInformation findRepositoryInformation(
			Repositories repositories, Class<?> domainObjectClass) {
		RepositoryInformation ri = null;
		for (Class<?> clazz : repositories) {
			if (clazz.equals(domainObjectClass)) {
				return repositories.getRepositoryInformationFor(clazz).get();
			}
		}
		return ri;
	}
}
