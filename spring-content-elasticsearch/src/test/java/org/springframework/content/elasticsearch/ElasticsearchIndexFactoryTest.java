package org.springframework.content.elasticsearch;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import io.searchbox.client.JestClient;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.StoreAccessException;

import java.io.IOException;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static internal.org.springframework.content.elasticsearch.HasFieldWithValue.hasField;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ElasticsearchIndexFactoryTest {

    private ElasticsearchIndexFactory factory;

    private String[] stores = new String[]{};
    private Exception e;

    // mock
    private JestClient client;
    {
        Describe("ElasticsearchIndexFactory", () -> {
            BeforeEach(() -> {
                client = mock(JestClient.class);

                factory = new ElasticsearchIndexFactory(client);
            });
            Context("#createIndexes", () -> {
                JustBeforeEach(() -> {
                    try {
                        factory.createIndexes(stores);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a store", () -> {
                    BeforeEach(() -> {
                        stores = new String[]{"MyStore", "MyOtherStore"};

                        SearchResult result = mock(SearchResult.class);
                        when(result.isSucceeded()).thenReturn(true);
                        when(client.execute(anyObject())).thenReturn(result);
                    });
                    It("should use jestClient to create an index for each store", () -> {
                        ArgumentCaptor<CreateIndex> captor = ArgumentCaptor.forClass(CreateIndex.class);

                        verify(client, times(2)).execute(captor.capture());

                        List<CreateIndex> results = captor.getAllValues();
                        for (int i = 0; i < stores.length; i++) {
                            assertThat(results.get(i), hasField("indexName", is(stores[i])));
                        }
                    });
                    Context("when the jest client throws an IOException", () -> {
                        BeforeEach(() -> {
                            when(client.execute(anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("when the jest client returns a unsuccessful result", () -> {
                        BeforeEach(() -> {
                            SearchResult result = mock(SearchResult.class);
                            when(result.isSucceeded()).thenReturn(false);
                            when(client.execute(anyObject())).thenReturn(result);
                        });
                        It("should throw a StoreAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                });
            });
        });
    }

    public ElasticsearchIndexFactoryTest() {
    }
}
