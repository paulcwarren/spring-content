package internal.org.springframework.content.commons.search;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.commons.utils.ReflectionService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.ContentAccessException;
import org.springframework.content.commons.repository.ContentRepositoryInvoker;
import org.springframework.content.commons.search.Searchable;


import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class SolrSearchImplTest {

    private SolrSearchImpl search;

    private String keyword;

    private String[] terms;
    private double[] weights;
    private String operator, starts, ends;
    private int proximity;
    private List<Object> result;
    private Exception e;

    // mock
    private SolrClient solr;
    private ReflectionService reflectionService;


    {
        Describe("Searchable", () -> {
                    BeforeEach(() -> {
                        solr = mock(SolrClient.class);
                        reflectionService = mock(ReflectionService.class);
                        NamedList list = new NamedList();
                        SolrDocumentList docs = new SolrDocumentList();
                        SolrDocument doc = new SolrDocument();
                        doc.addField("id", "12345");
                        docs.add(doc);
                        list.add("response", docs);
                        when(solr.query(anyObject())).thenReturn(new QueryResponse(list, solr));
                    });
            Context("#findKeyword", () -> {
                    Context("given a keyword", () -> {
                        BeforeEach(() -> {
                            keyword = "something";
                        });
                        JustBeforeEach(() -> {
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findKeyword(keyword);
                            } catch (Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is(keyword));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findAllKeywords(terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("something AND else"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findAnyKeywords(terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("something OR else OR bobbins"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findKeywordsNear(proximity, terms);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("\"foo bar\"~4"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findKeywordStartsWith(keyword);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("something*"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findKeywordStartsWithAndEndsWith(starts, ends);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("something*else"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                            search = new SolrSearchImpl(solr, reflectionService);
                            try {
                                result = search.findAllKeywordsWithWeights(terms, weights);
                            } catch(Exception e) {
                                this.e = e;
                            }
                        });
                        It("should execute a query", () -> {
                            ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);
                            verify(solr).query(argument.capture());
                            assertThat(argument.getValue().getQuery(), is("(foo)^1.59 AND (bar)^200.0"));
                            assertThat(argument.getValue().getFields(), is("id"));
                        });
                        It("should map results to set of IDs", () -> {
                            assertThat(result.size(), is(1));
                            assertThat(result.get(0), is("12345"));
                        });
                        Context("given a SolrServerException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(SolrServerException.class);
                            });
                            It("should throw an ContentAccessException", () -> {
                                assertThat(e, is(not(nullValue())));
                                assertThat(e, is(instanceOf(ContentAccessException.class)));
                            });
                        });
                        Context("given an IOException from solr", () -> {
                            BeforeEach(() -> {
                                when(solr.query(anyObject())).thenThrow(IOException.class);
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
                           search = new SolrSearchImpl(null, reflectionService);
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
                           search = new SolrSearchImpl(null, reflectionService);
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
                    search = new SolrSearchImpl(null, reflectionService);
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
                It("should invoke the correct method", () -> {
                    reflectionService = mock(ReflectionService.class);

                    MethodInvocation mockedMethod = mock(MethodInvocation.class);
                    Method method = Searchable.class.getMethod("findKeyword", String.class);
                    when(mockedMethod.getMethod()).thenReturn(method);
                    when(mockedMethod.getArguments()).thenReturn(new String[]{"something"});
                    ContentRepositoryInvoker invoker = mock(ContentRepositoryInvoker.class);

                    search = new SolrSearchImpl(null, reflectionService);
                    search.invoke(mockedMethod, invoker);

                    verify(reflectionService).invokeMethod(method, search, "something");
                });
            });
        });
    }

    @Test
    public void noop() {
    }
}
