package org.springframework.content.solr;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.solr.SolrSearchService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.core.Every;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrSearchContentRepositoryExtensionTest {

    private SolrSearchContentRepositoryExtension search;

    private Method method;
    private MethodInvocation invocation;
    private StoreInvoker invoker;
    private GenericConversionService conversionService;
    private SolrProperties solrProperties;

    // mock
    private SolrClient solr;
    private ReflectionService reflectionService;

    {
        Describe("ContentRepositoryExtension", () ->{
            Context("#getMethods", () -> {
                It("should return searchable methods", () -> {
                    search = new SolrSearchContentRepositoryExtension(null, reflectionService, conversionService, solrProperties);
                    Set<Method> methods = search.getMethods();
                    assertThat(methods, is(not(nullValue())));
                    assertThat(methods.size(), is(greaterThan(0)));
                    assertThat(methods.contains(Searchable.class.getMethod("findKeyword", String.class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findAllKeywords", String[].class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findAnyKeywords", String[].class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findKeywordsNear", int.class, String[].class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findKeywordStartsWith", String.class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findKeywordStartsWithAndEndsWith", String.class, String.class)), is(true));
                    assertThat(methods.contains(Searchable.class.getMethod("findAllKeywordsWithWeights", String[].class, double[].class)), is(true));
                });
            });
            Context("#invoke", () -> {
                BeforeEach(() -> {
                    invocation = mock(MethodInvocation.class);
                    method = Searchable.class.getMethod("findKeyword", String.class);
                    when(invocation.getMethod()).thenReturn(method);
                    when(invocation.getArguments()).thenReturn(new String[]{"something"});

                    invoker = mock(StoreInvoker.class);

                    conversionService = new GenericConversionService();
                    conversionService.addConverter(new StringToInteger());
                });
                It("should invoke the correct method", () -> {
                    doReturn(String.class).when(invoker).getContentIdClass();
                    doReturn(Document.class).when(invoker).getDomainClass();

                    reflectionService = mock(ReflectionService.class);
                    when(reflectionService.invokeMethod(anyObject(), anyObject(), anyVararg())).thenReturn(Collections.singletonList("12345"));

                    search = new SolrSearchContentRepositoryExtension(null, reflectionService, conversionService, solrProperties);
                    search.invoke(invocation, invoker);

                    verify(reflectionService).invokeMethod(argThat(is(method)), argThat(is(instanceOf(SolrSearchService.class))), argThat(is("something")));
                });
                It("should convert the returned list to the content id type", () -> {
                    doReturn(Integer.class).when(invoker).getContentIdClass();
                    doReturn(Document.class).when(invoker).getDomainClass();

                    solr = mock(SolrClient.class);
                    solrProperties = new SolrProperties();
                    NamedList list = new NamedList();
                    SolrDocumentList docs = new SolrDocumentList();
                    SolrDocument doc = new SolrDocument();
                    doc.addField("id", "12345");
                    docs.add(doc);
                    list.add("response", docs);
                    when(solr.request(anyObject(), anyObject())).thenReturn(list);

                    reflectionService = new ReflectionServiceImpl();

                    search = new SolrSearchContentRepositoryExtension(solr, reflectionService, conversionService, solrProperties);
                    Iterable<?> results = (Iterable<?>)search.invoke(invocation, invoker);
                    assertThat(results, Every.everyItem(instanceOf(Integer.class)));
                });
            });
        });
    }

    @Test
    public void noop() {
    }

    static class StringToInteger implements Converter<String, Integer> {
        public Integer convert(String source) {
            return Integer.valueOf(source);
        }
    }

    static class Document {}
}
