package org.springframework.content.solr;

import internal.org.springframework.content.solr.SolrSearchService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.convert.ConversionService;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

public class SolrSearchContentRepositoryExtension implements StoreExtension {

    private SolrClient solr;
    private ReflectionService reflectionService;
    private ConversionService conversionService;
    private SolrProperties solrProperties;

    public SolrSearchContentRepositoryExtension(SolrClient solr, ReflectionService reflectionService, ConversionService conversionService, SolrProperties solrProperties) {
        this.solr = solr;
        this.reflectionService = reflectionService;
        this.conversionService = conversionService;
        this.solrProperties = solrProperties;
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

        Object tgt = new SolrSearchService(solr, solrProperties, domainClass);
        List<String> list = (List) reflectionService.invokeMethod(invocation.getMethod(), tgt, invocation.getArguments());
        for (String item : list) {
            if (conversionService.canConvert(item.getClass(), clazz) == false) {
                throw new IllegalStateException(String.format("Cannot convert item of type %s to %s", item.getClass().getName(), clazz.getName()));
            }
            newList.add(conversionService.convert(item, clazz));
        }

        return newList;
    }
}





