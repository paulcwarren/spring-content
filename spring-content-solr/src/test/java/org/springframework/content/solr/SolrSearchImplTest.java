package org.springframework.content.solr;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.core.Every;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.ContentAccessException;
import org.springframework.content.commons.repository.ContentRepositoryInvoker;
import org.springframework.content.commons.search.Searchable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class SolrSearchImplTest {

    private SolrSearchImpl search;

    private String keyword;

    private String[] terms;
    private double[] weights;
    private String operator, starts, ends;
    private int proximity;
    private List<Object> result;
    private NamedList<Object> result2;
    private Exception e;
    private Method method;
    private MethodInvocation invocation;
    private ContentRepositoryInvoker invoker;
    private GenericConversionService conversionService;
    private SolrProperties solrProperties;
    // mock
    private SolrClient solr;
    private QueryRequest queryRequest;
    private ReflectionService reflectionService;

    {
        Describe("Searchable", () -> {
                    BeforeEach(() -> {
                        solr = mock(SolrClient.class);
                        reflectionService = mock(ReflectionService.class);
                        conversionService = mock(GenericConversionService.class);
                        solrProperties = new SolrProperties();
                        NamedList list = new NamedList();
                        SolrDocumentList docs = new SolrDocumentList();
                        SolrDocument doc = new SolrDocument();
                        doc.addField("id", "12345");
                        docs.add(doc);
                        list.add("response", docs);
                        when(solr.request(anyObject(), anyObject())).thenReturn(list);
                    });
            Context("#findKeyword", () -> {
                    Context("given a keyword", () -> {
                        BeforeEach(() -> {
                            keyword = "something";
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findKeyword(keyword);
                            } catch (Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"),is(keyword));
                            assertThat(argument.getValue().getParams().get("fl"),is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findAllKeywords", () -> {
                    Context("given an array of keywords", () -> {
                        BeforeEach(() -> {
                            terms = new String[] {"something", "else"};
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findAllKeywords(terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("something AND else"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findAnyKeywords", () -> {
                    Context("given an array of terms", () -> {
                        BeforeEach(() -> {
                            terms = new String[] {"something", "else", "bobbins"};
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findAnyKeywords(terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("something OR else OR bobbins"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findKeywordsNear", () -> {
                    Context("given a proximity and array of terms", () -> {
                        BeforeEach(() -> {
                            proximity = 4;
                            terms = new String[] {"foo", "bar"};
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findKeywordsNear(proximity, terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("\"foo bar\"~4"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findKeywordStartsWith", () -> {
                    Context("given a keyword", () -> {
                        BeforeEach(() -> {
                            keyword = "something";
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findKeywordStartsWith(keyword);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("something*"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findKeywordStartsWithAndEndsWith", () -> {
                    Context("given a starting and ending keyword", () -> {
                        BeforeEach(() -> {
                            starts = "something";
                            ends = "else";
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findKeywordStartsWithAndEndsWith(starts, ends);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("something*else"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });
                Context("#findAllKeywordsWithWeights", () -> {
                    Context("given an array of keywords and an array of their corresponding weights", () -> {
                        BeforeEach(() -> {
                            terms = new String[] {"foo", "bar"};
                            weights = new double[] {1.59, 200};
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                            try {
                                result = search.findAllKeywordsWithWeights(terms, weights);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                            verify(solr).request(argument.capture(),anyObject());
                            assertThat(argument.getValue().getParams().get("q"), is("(foo)^1.59 AND (bar)^200.0"));
                            assertThat(argument.getValue().getParams().get("fl"), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                    });
                });


            Context("#parseTerms", () -> {
               Context("given multiple terms", () -> {
                   BeforeEach(() -> {
                       terms = new String[] {"term1", "term2", "term3"};
                   });
                   Context("given AND", () -> {
                       BeforeEach(() -> {
                           operator = "AND";
                       });

                       JustBeforeEach(() -> {
                           search = new SolrSearchImpl(null, reflectionService, conversionService, solrProperties);
                       });
                       It("should return the correct string", () -> {
                           String parsedResult = search.parseTerms(operator, terms);
                           String passValue = "term1 AND term2 AND term3";
                           assertThat(parsedResult, is(passValue));

                       });
                   });
                   Context("given OR", () -> {
                       BeforeEach(() -> {
                           operator = "OR";
                       });

                       JustBeforeEach(() -> {
                           search = new SolrSearchImpl(null, reflectionService, conversionService, solrProperties);
                       });
                       It("should return the correct string", () -> {
                           String parsedResult = search.parseTerms(operator, terms);
                           String passValue = "term1 OR term2 OR term3";
                           assertThat(parsedResult, is(passValue));

                       });
                   });
               });
            });
        });

        Describe("ContentRepositoryExtension", () ->{
            Context("#getMethods", () -> {
                It("should return searchable methods", () -> {
                    search = new SolrSearchImpl(null, reflectionService, conversionService, solrProperties);
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

                    invoker = mock(ContentRepositoryInvoker.class);

                    conversionService = new GenericConversionService();
                    conversionService.addConverter(new StringToInteger());
                });
                It("should invoke the correct method", () -> {
                    reflectionService = mock(ReflectionService.class);
                    when(reflectionService.invokeMethod(anyObject(), anyObject(), anyObject())).thenReturn(Collections.singletonList("12345"));
                    doReturn(String.class).when(invoker).getContentIdClass();

                    search = new SolrSearchImpl(null, reflectionService, conversionService, solrProperties);
                    search.invoke(invocation, invoker);

                    verify(reflectionService).invokeMethod(method, search, "something");
                });

                It("should convert the returned list to the content id type", () -> {
                    solr = mock(SolrClient.class);
                    solrProperties = new SolrProperties();
                    NamedList list = new NamedList();
                    SolrDocumentList docs = new SolrDocumentList();
                    SolrDocument doc = new SolrDocument();
                    doc.addField("id", "12345");
                    docs.add(doc);
                    list.add("response", docs);
                    when(solr.request(anyObject(), anyObject())).thenReturn(list);
                    doReturn(Integer.class).when(invoker).getContentIdClass();

                    reflectionService = new ReflectionServiceImpl();

                    search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                    Iterable<?> results = (Iterable<?>)search.invoke(invocation, invoker);
                    assertThat(results, Every.everyItem(instanceOf(Integer.class)));
                });
            });
            Context("#solrAuthenticate", () -> {
                BeforeEach(() -> {
                    queryRequest = new QueryRequest(new SolrQuery("something"));
                    solrProperties = new SolrProperties();
                    solrProperties.setPassword("password");
                    solrProperties.setUser("username");
                });

                It("should create an authenticated request", () -> {
                    search = new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties);
                    QueryRequest authRequest = search.solrAuthenticate(queryRequest);
                    assertThat(authRequest.getBasicAuthUser(), is("username"));
                    assertThat(authRequest.getBasicAuthPassword(), is("password"));

                });
            });
            Context("#executeQuery", () -> {
                BeforeEach(() -> {
                    NamedList list = new NamedList();
                    SolrDocumentList docs = new SolrDocumentList();
                    SolrDocument doc = new SolrDocument();
                    doc.addField("id", "12345");
                    docs.add(doc);
                    list.add("response", docs);
                    keyword = "something";
                    solr = mock(SolrClient.class, CALLS_REAL_METHODS);
                    when(solr.request(anyObject(), anyObject())).thenReturn(list);
                    solrProperties = new SolrProperties();
                    search = spy(new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties));

                });
                Context("When a username/password is specified", () -> {
                    BeforeEach(() -> {
                        solrProperties.setPassword("password");
                        solrProperties.setUser("username");
                    });
                    JustBeforeEach(() -> {
                        search = spy(new SolrSearchImpl(solr, reflectionService, conversionService, solrProperties));
                        search.executeQuery(keyword);
                    });
                    It("should authenticate", () -> {
                        verify(search, times(1)).solrAuthenticate(anyObject());
                    });
                });
                JustBeforeEach(() -> {
                    result2 = search.executeQuery(keyword);
                });
                It("should execute the query", () -> {
                    verify(solr, times(1)).request(anyObject(), anyObject());
                    assertThat(result2, is(notNullValue()));
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
}
