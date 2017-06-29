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
        Search search = getSearch(query);

        SearchResult result;

        try {
            result = client.execute(search);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Exception while searching for %s", query), e);
        }

        return getIDs(result);
    }

    @Override
    public Iterable<Serializable> findAllKeywords(String... terms) {
        StringBuilder qb = join("AND", terms);

        Search search = getSearch(qb.toString());

        SearchResult result;

        try {
            result = client.execute(search);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Exception while searching for %s", qb), e);
        }

        return getIDs(result);
    }

    @Override
    public Iterable<Serializable> findAnyKeywords(String... terms) {

        StringBuilder qb = join("OR", terms);
        Search search = getSearch(qb.toString());

        SearchResult result;

        try {
            result = client.execute(search);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Exception while searching for %s", qb), e);
        }

        return getIDs(result);
    }

    @Override
    public Iterable<Serializable> findKeywordsNear(int proximity, String... terms) {
        StringBuilder sb = join("", terms);
        String query = String.format("{\"query\": {\"query_string\": {\"query\": \"\\\"%s\\\"%s\"}}}", sb.toString(), "~"+proximity);

        Search search = new Search.Builder(query)
                .addIndex("docs")
                .addType("doc")
                .build();

        SearchResult result;

        try {
            result = client.execute(search);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Exception while searching for keywords %s near %s",
                    sb.toString(), proximity), e);
        }

        return getIDs(result);
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

    private Search getSearch(String query) {
        JsonParser parser = new JsonParser();
        JsonObject payload = parser.parse(String.format("{\"query\":{\"query_string\":{\"query\":\"%s\"}}}", query)).getAsJsonObject();

        return new Search.Builder(payload.toString())
                .addIndex("docs")
                .addType("doc")
                .build();
    }

    private List<Serializable> getIDs(SearchResult result) {
        List<Serializable> contents = new ArrayList<>();

        if (result != null) {
            List<SearchResult.Hit<Content, Void>> hits = result.getHits(Content.class);

            contents = hits.stream()
                    .map(hit -> hit.source.getId())
                    .collect(Collectors.toList());
        }
        return contents;
    }

    private StringBuilder join(String delimiter, String... terms) {
        StringBuilder qb = new StringBuilder();
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) {
                if(delimiter.isEmpty()){
                    qb.append(" ");
                } else {
                    qb.append(String.format(" %s ", delimiter));
                }
            }
            qb.append(terms[i]);
        }
        return qb;
    }
}
