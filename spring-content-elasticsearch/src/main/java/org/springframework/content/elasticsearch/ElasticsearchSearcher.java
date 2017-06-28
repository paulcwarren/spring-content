package org.springframework.content.elasticsearch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticsearchSearcher implements Searchable<Serializable> {

    private JestClient client;

    public ElasticsearchSearcher(JestClient client) {
        this.client = client;
    }

    @Override
    public Iterable<Serializable> findKeyword(String query) {
        JsonParser parser = new JsonParser();
        JsonObject payload = parser.parse(String.format("{\"query\":{\"query_string\":{\"query\":\"%s\"}}}", query)).getAsJsonObject();

        Search search = new Search.Builder(payload.toString())
                .addIndex("docs")
                .addType("doc")
                .build();

        SearchResult result = null;

        try {
            result = client.execute(search);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Exception while searching for %s", query), e);
        }

        List<Serializable> contents = new ArrayList<>();

        if (result != null) {
            List<SearchResult.Hit<Content, Void>> hits = result.getHits(Content.class);

            contents = hits.stream()
                    .map(hit -> hit.source.getId())
                    .collect(Collectors.toList());
        }
        return contents;
    }

    @Override
    public Iterable<Serializable> findAllKeywords(String... terms) {
        return null;
    }

    @Override
    public Iterable<Serializable> findAnyKeywords(String... terms) {
        return null;
    }

    @Override
    public Iterable<Serializable> findKeywordsNear(int proximity, String... terms) {
        return null;
    }

    @Override
    public Iterable<Serializable> findKeywordStartsWith(String term) {
        return null;
    }

    @Override
    public Iterable<Serializable> findKeywordStartsWithAndEndsWith(String a, String b) {
        return null;
    }

    @Override
    public Iterable<Serializable> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        return null;
    }
}
