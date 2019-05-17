package internal.org.springframework.content.elasticsearch;

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

import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;

import static internal.org.springframework.content.elasticsearch.ElasticsearchIndexer.INDEX_NAME;
import static java.lang.String.format;

public class ElasticsearchSearcher implements Searchable<Serializable> {

	private final RestHighLevelClient client;
	private final Class<?> domainClass;

	public ElasticsearchSearcher(RestHighLevelClient client, Class<?> domainClass) {
		this.client = client;
		this.domainClass = domainClass;
	}

	@Override
	public Iterable<Serializable> findKeyword(String query) {
		SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
		searchRequest.types(domainClass.getName());

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.queryStringQuery(query));

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
