package internal.org.springframework.content.commons.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static java.lang.String.format;

public class StoreFragmentDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";

	private final Environment environment;
	private final ResourceLoader resourceLoader;
	private final String postfix;
	private final Set<String> basePackages;
	private final MetadataReaderFactory metadataReaderFactory;
	private Lazy<Set<BeanDefinition>> implementationCandidates = Lazy.empty();

	public StoreFragmentDetector(Environment environment, ResourceLoader loader, String postfix, String[] basePackages, MetadataReaderFactory metadataReaderFactory) {
		this.environment = environment;
		this.resourceLoader = loader;
		this.postfix = postfix;

		this.basePackages = new HashSet<String>(Arrays.asList(basePackages));
		this.basePackages.add("org.springframework.content.fragments");
		this.basePackages.add("internal.org.springframework.content.fragments");

		this.metadataReaderFactory = metadataReaderFactory;
		this.implementationCandidates = Lazy.of(() -> findCandidateBeanDefinitions());
	}

	public Set<BeanDefinition> getBeanDefinitions() {
		return implementationCandidates.get();
	}

	private Set<BeanDefinition> findCandidateBeanDefinitions() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false, environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, postfix));
		provider.setMetadataReaderFactory(metadataReaderFactory);
		provider.addIncludeFilter((reader, factory) -> true);

//		config.getExcludeFilters().forEach(it -> provider.addExcludeFilter(it));

		return basePackages.stream()//
				.flatMap(it -> provider.findCandidateComponents(it).stream())//
				.collect(Collectors.toSet());
	}

	public StoreFragmentDefinition detectCustomImplementation(String iface, String storeInterface) {

		Predicate pred = new InterfaceNamePredicate(iface, basePackages, postfix);

		List<BeanDefinition> definitions = implementationCandidates.get().stream().filter(pred::test).collect(Collectors.toList());

		if (definitions.isEmpty()) {
			throw new IllegalStateException(format("No implementation found for store interface %s", iface));
		}

		if (definitions.size() > 1) {
			throw new IllegalStateException(format("Multiple implementations (%s) found for store interface %s", concat(definitions), iface));
		}

		StoreFragmentDefinition defn = new StoreFragmentDefinition(iface, definitions.get(0));
		defn.setStoreInterfaceName(storeInterface);
		return defn;
	}


	private String concat(List<BeanDefinition> definitions) {

		return definitions.stream()//
				.map(BeanDefinition::getBeanClassName)//
				.collect(Collectors.joining(", "));
	}

	private static class InterfaceNamePredicate implements Predicate<BeanDefinition> {

		private final String interfaceName;
		private final Set<String> basePackages;
		private final String postfix;

		public InterfaceNamePredicate(String interfaceName, Set<String> basePackages, String postfix) {
			this.interfaceName = interfaceName;
			this.basePackages = basePackages;
			this.postfix = postfix;
		}

		@Override
		public boolean test(BeanDefinition definition) {
			Assert.notNull(definition, "BeanDefinition must not be null!");

			String beanClassName = definition.getBeanClassName();

			if (beanClassName == null /*|| isExcluded(beanClassName, getExcludeFilters())*/) {
				return false;
			}

			String beanPackage = ClassUtils.getPackageName(beanClassName);
			String shortName = ClassUtils.getShortName(beanClassName);
			String localName = shortName.substring(shortName.lastIndexOf('.') + 1);

			return localName.equals(getImplementationClassName(interfaceName)) //
					&& basePackages.stream().anyMatch(it -> beanPackage.startsWith(it));

		}

		private String getImplementationClassName(String interfaceName) {
			String shortName = ClassUtils.getShortName(interfaceName);
			String localName = shortName.substring(shortName.lastIndexOf('.') + 1);
			return localName.concat(postfix);
		}
	}
}
