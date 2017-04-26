package internal.org.springframework.content.solr;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.solr.SolrProperties;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrSearchServiceTest {

    private SolrSearchService search;

    private String keyword;
    private String[] terms;
    private double[] weights;
    private String operator, starts, ends;
    private int proximity;
    private List<Object> result;
    private Exception e;
    private SolrProperties solrProperties;

    // mocks
    private SolrClient solr;
    private QueryRequest queryRequest;

    {
        Describe("Searchable", () -> {
            BeforeEach(() -> {
                solr = mock(SolrClient.class);
                solrProperties = new SolrProperties();
                NamedList list = new NamedList();
                SolrDocumentList docs = new SolrDocumentList();
                SolrDocument doc = new SolrDocument();
                doc.addField("id", Document.class.getCanonicalName() + ":12345");
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findKeyword(keyword);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"),is("(" + keyword + ") AND id:" + Document.class.getCanonicalName() + "\\:*"));
                        assertThat(argument.getValue().getParams().get("fl"),is("id"));
                    });
                    It("should remove the type segment of the ID form the result set", () -> {
                        assertThat(result.size(), is(1));
                        assertThat(result.get(0), is("12345"));
                    });
                    Context("given a SolrServerException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findAllKeywords(terms);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("(something AND else) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findAnyKeywords(terms);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("(something OR else OR bobbins) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findKeywordsNear(proximity, terms);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("(\"foo bar\"~4) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findKeywordStartsWith(keyword);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("(something*) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findKeywordStartsWithAndEndsWith(starts, ends);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("(something*else) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                        search = new SolrSearchService(solr, solrProperties, Document.class);
                        try {
                            result = search.findAllKeywordsWithWeights(terms, weights);
                        } catch(Exception e) {
                            this.e = e;
                        }
                    });
                    It("should execute a query", () -> {
                        ArgumentCaptor<SolrRequest> argument = forClass(SolrRequest.class);
                        verify(solr).request(argument.capture(),anyObject());
                        assertThat(argument.getValue().getParams().get("q"), is("((foo)^1.59 AND (bar)^200.0) AND id:" + Document.class.getCanonicalName() + "\\:*"));
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
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given an IOException from solr", () -> {
                        BeforeEach(() -> {
                            when(solr.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw an ContentAccessException", () -> {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                           search = new SolrSearchService(null, solrProperties, Document.class);
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
                           search = new SolrSearchService(null, solrProperties, Document.class);
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
        Context("#solrAuthenticate", () -> {
            BeforeEach(() -> {
                queryRequest = new QueryRequest(new SolrQuery("something"));
                solrProperties = new SolrProperties();
                solrProperties.setPassword("password");
                solrProperties.setUser("username");
            });

            It("should create an authenticated request", () -> {
                search = new SolrSearchService(solr, solrProperties, Document.class);
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
                search = spy(new SolrSearchService(solr, solrProperties, Document.class));

            });
            Context("When a username/password is specified", () -> {
                BeforeEach(() -> {
                    solrProperties.setPassword("password");
                    solrProperties.setUser("username");
                });
                JustBeforeEach(() -> {
                    search = spy(new SolrSearchService(solr, solrProperties, Document.class));
                    search.executeQuery(Document.class, keyword);
                });
                It("should authenticate", () -> {
                    verify(search, times(1)).solrAuthenticate(anyObject());
                });
            });
            It("should execute the query", () -> {
                NamedList result = search.executeQuery(Document.class, keyword);
                verify(solr, times(1)).request(anyObject(), anyObject());
                assertThat(result, is(notNullValue()));
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
