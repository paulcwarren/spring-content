package org.springframework.content.elasticsearch;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import internal.org.springframework.content.elasticsearch.ElasticsearchSearcher;
import org.aopalliance.intercept.MethodInvocation;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.convert.ConversionService;

public class ElasticsearchStoreExtension implements StoreExtension {

	private RestHighLevelClient client;
	private ReflectionService reflectionService;
	private ConversionService conversionService;

	public ElasticsearchStoreExtension(RestHighLevelClient solr,
			ReflectionService reflectionService,
			ConversionService conversionService) {

		this.client = solr;
		this.reflectionService = reflectionService;
		this.conversionService = conversionService;
	}

	@Override
	public Set<Method> getMethods() {
		Set<Method> methods = new HashSet<>();
		methods.addAll(Arrays.asList(Searchable.class.getMethods()));
		return methods;
	}

	@Override
	public Object invoke(MethodInvocation invocation, StoreInvoker invoker) {
		List newList = new ArrayList();
		Class<? extends Serializable> clazz = invoker.getContentIdClass();
		Class<?> domainClass = invoker.getDomainClass();

		Object tgt = new ElasticsearchSearcher(client, domainClass);
		List<String> list = (List) reflectionService.invokeMethod(invocation.getMethod(), tgt, invocation.getArguments());
		for (String item : list) {
			if (conversionService.canConvert(item.getClass(), clazz) == false) {
				throw new IllegalStateException(String.format("Cannot convert item of type %s to %s",
								item.getClass().getName(), clazz.getName()));
			}
			newList.add(conversionService.convert(item, clazz));
		}

		return newList;
	}
}
