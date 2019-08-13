package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;

import static internal.org.springframework.content.elasticsearch.ElasticsearchIndexer.INDEX_NAME;
import static java.lang.String.format;

public class SearchableImpl implements Searchable<Serializable> {

	private final RestHighLevelClient client;
	private Class<?> domainClass;

	public SearchableImpl() {
		client = null;
		domainClass = null;
	}

	@Autowired
	public SearchableImpl(RestHighLevelClient client) {
		this.client = client;
	}

	public void setDomainClass(Class<?> domainClass) {
		this.domainClass = domainClass;
	}

	@Override
	public Iterable<Serializable> search(String queryStr) {
		SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
		searchRequest.types(domainClass.getName());

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.simpleQueryStringQuery(queryStr));
		searchRequest.source(sourceBuilder);

		SearchResponse res = null;
		try {
			res = client.search(searchRequest, RequestOptions.DEFAULT);
		}
		catch (IOException ioe) {
			throw new StoreAccessException(format("Error searching indexed content for '%s'", queryStr), ioe);
		}

		return getIDs(res.getHits());
	}

	@Override
	public Iterable<Serializable> findKeyword(String query) {
		SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
		searchRequest.types(domainClass.getName());

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.queryStringQuery(query));
		searchRequest.source(sourceBuilder);

		SearchResponse res = null;
		try {
			res = client.search(searchRequest, RequestOptions.DEFAULT);
		}
		catch (IOException ioe) {
			throw new StoreAccessException(format("Error searching indexed content for '%s'", query), ioe);
		}

		return getIDs(res.getHits());
	}

	@Override
	public Iterable<Serializable> findAllKeywords(String... terms) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Serializable> findAnyKeywords(String... terms) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Serializable> findKeywordsNear(int proximity, String... terms) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Serializable> findKeywordStartsWith(String term) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Serializable> findKeywordStartsWithAndEndsWith(String a, String b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Serializable> findAllKeywordsWithWeights(String[] terms, double[] weights) {
		throw new UnsupportedOperationException();
	}

	private List<Serializable> getIDs(SearchHits result) {
		List<Serializable> contents = new ArrayList<>();

		if (result == null || result.getTotalHits() == 0) {
			return contents;
		}

		for (SearchHit hit : result.getHits()) {
			contents.add(hit.getId());
		}

		return contents;
	}
}
