package internal.org.springframework.content.commons.search;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
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
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.ContentAccessException;


import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
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

    {
        Describe("SolrSearchImpl", () -> {
                    BeforeEach(() -> {
                        solr = mock(SolrClient.class);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                            search = new SolrSearchImpl(solr);
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
                           search = new SolrSearchImpl(null);
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
                           search = new SolrSearchImpl(null);
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
    }

    @Test
    public void noop() {
    }
}
