package internal.org.springframework.content.rest.mappings;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;

public class CorsConfigurationBuilder {

	public CorsConfiguration build(Class<?> storeInterface) {
		CorsConfiguration config = new CorsConfiguration();

		CrossOrigin annotation = AnnotatedElementUtils
				.findMergedAnnotation(storeInterface, CrossOrigin.class);
		if (annotation == null) {
			return null;
		}

		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(origin);
		}

		if (CollectionUtils.isEmpty(config.getAllowedOrigins())) {
			config.applyPermitDefaultValues();
		}

		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(header);
		}

		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(header);
		}

		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (HttpMethod httpMethod : HttpMethod.values()) {
				config.addAllowedMethod(httpMethod);
			}
		}

		String allowCredentials = annotation.allowCredentials();

		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		}
		else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		}
		else if (!allowCredentials.isEmpty()) {
			throw new IllegalStateException(
					"@CrossOrigin's allowCredentials value must be \"true\", \"false\", "
							+ "or an empty string (\"\"): current value is ["
							+ allowCredentials + "]");
		}

		if (annotation.maxAge() >= 0) {
			config.setMaxAge(annotation.maxAge());
		}

		return config;
	}
}
