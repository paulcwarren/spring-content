package org.springframework.content.elasticsearch;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.core.SearchResult;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ElasticsearchSearcherTest {

    private ElasticsearchSearcher searcher;

    private String keywords;
    private Iterable<Serializable> result;
    private Exception exception;

    // mock
    JestClient client;

    {
        Describe("ElasticsearchSearcher", () -> {
            BeforeEach(() -> {
                client = mock(JestClient.class);

                searcher = new ElasticsearchSearcher(client);
                keywords = "something";
            });
            Context("#findKeyword", () -> {
                JustBeforeEach(() -> {
                    try {
                        result = searcher.findKeyword(keywords);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given elasticsearch return a result", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should return a result", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                        assertThat(argumentCaptor.getValue().getData(new GsonBuilder().create()) , containsString("\"query\":\"something\""));
                    });
                });
                Context("given elasticsearch errors", () -> {
                    BeforeEach(() -> {
                        when(client.execute(anyObject())).thenThrow(IOException.class);
                    });
                    It("should throw a StoreAccessException", () -> {
                        assertThat(exception, is(instanceOf(StoreAccessException.class)));
                    });
                });
            });
            Context("#findAllKeywords", () -> {
                JustBeforeEach(() -> {
                    String[] keywords = {"one", "two", "three"};
                    try {
                        result = searcher.findAllKeywords(keywords);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given elastic search is available", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, containsString("\"query\":\"one AND two AND three\""));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elasticsearch throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
            Context("#findAnyKeywords", () -> {
                JustBeforeEach(() -> {
                    String[] keywords = {"one", "two", "three"};
                    try {
                        result = searcher.findAnyKeywords(keywords);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given an elastic search", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, containsString("\"query\":\"one OR two OR three\""));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elasticsearch throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
            Context("#findKeywordsNear", () -> {
                JustBeforeEach(() -> {
                    String[] keywords = {"one", "two", "three"};
                    try {
                        result = searcher.findKeywordsNear(10, keywords);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given an elastic search", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, is("{\"query\":{\"query_string\":{\"query\":\"\\\"one two three\\\"~10\"}}}"));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elastic search throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
            Context("#findKeywordsStartsWith", () -> {
                JustBeforeEach(() -> {
                    String keyword = "one";
                    try {
                        result = searcher.findKeywordStartsWith(keyword);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given an elastic search", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, is("{\"query\":{\"query_string\":{\"query\":\"one*\"}}}"));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elastic search throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
            Context("#findAllKeywordsWithWeights", () -> {
                JustBeforeEach(() -> {
                    String[] keywrods = {"one", "two"};
                    double[] weights = {2, 3};
                    try {
                        result = searcher.findAllKeywordsWithWeights(keywrods, weights);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given an elastic search", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, is("{\"query\":{\"query_string\":{\"query\":\"one^2.0 two^3.0\"}}}"));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elastic search throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
            Context("#findKeywordsStartsWithAndEndsWith", () -> {
                JustBeforeEach(() -> {
                    String startsWith = "one";
                    String endsWith = "two";
                    try {
                        result = searcher.findKeywordStartsWithAndEndsWith(startsWith, endsWith);
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                Context("given an elastic search", () -> {
                    BeforeEach(() -> {
                        SearchResult result = new SearchResult(new GsonBuilder().create());
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":0.221545,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.221545,\"_source\":{\"original-content\":\"UWJv\",\"id\":\"12345\"}},{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"2\",\"_score\":0.73642,\"_source\":{\"original-content\":\"pb24u\",\"id\":\"67890\"}}]}}").getAsJsonObject();
                        result.setJsonObject(obj);
                        result.setPathToResult("hits/hits/_source");

                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should call the client with a correctly formed query", () -> {
                        Class<Action<SearchResult>> actionClass = (Class<Action<SearchResult>>)(Class)Action.class;
                        ArgumentCaptor<Action<SearchResult>> argumentCaptor = ArgumentCaptor.forClass(actionClass);
                        verify(client).execute(argumentCaptor.capture());

                        String query = argumentCaptor.getValue().getData(new GsonBuilder().create());
                        assertThat(query, is("{\"query\":{\"query_string\":{\"query\":\"one*two\"}}}"));
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, hasItem("12345"));
                        assertThat(result, hasItem("67890"));
                    });
                    Context("given elastic search throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(exception, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
        });
    }
}
