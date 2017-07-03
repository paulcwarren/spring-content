package org.springframework.content.elasticsearch;

import internal.org.springframework.content.elasticsearch.CreateIndex;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;

import java.io.IOException;

import static java.lang.String.format;

public class ElasticsearchIndexFactory {

    private final JestClient client;

    public ElasticsearchIndexFactory(JestClient client) {
        this.client = client;
    }

    public void createIndexes(String[] stores) {
        for (String store : stores) {
            io.searchbox.indices.CreateIndex index = new CreateIndex.Builder(store).build();

            try {
                JestResult result = client.execute(index);
                if (result.isSucceeded() == false) {
                    throw new StoreAccessException(format("Unexpected error creating elasticsearch index for store %s", store, result.getErrorMessage()));
                }
            } catch (IOException e) {
                throw new StoreAccessException(format("Unexpected error creating elasticsearch index for store %s", store), e);
            }
        }
    }
}
