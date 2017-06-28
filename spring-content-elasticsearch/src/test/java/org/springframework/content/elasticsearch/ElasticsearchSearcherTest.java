package org.springframework.content.elasticsearch;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
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

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.StoreAccessException;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.core.SearchResult;

@RunWith(Ginkgo4jRunner.class)
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
                        JsonObject obj = parser.parse("{\"hits\":{\"total\":1,\"max_score\":0.5338346,\"hits\":[{\"_index\":\"docs\",\"_type\":\"doc\",\"_id\":\"1\",\"_score\":0.5338346,\"_source\":{\"original-content\":\"UWJveCBt\",\"id\":\"12345\"}}]}}").getAsJsonObject();
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

            });
        });
    }
}
