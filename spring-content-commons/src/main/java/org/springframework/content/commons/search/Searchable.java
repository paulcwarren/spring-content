package org.springframework.content.commons.search;

public interface Searchable<T> {

    Iterable<T> findKeyword(String query);

    Iterable<T> findAllKeywords(String...terms);

    Iterable<T> findAnyKeywords(String...terms);

    Iterable<T> findKeywordsNear(int proximity, String...terms);

    Iterable<T> findKeywordStartsWith(String term);

    Iterable<T> findKeywordStartsWithAndEndsWith(String a, String b);

    Iterable<T> findAllKeywordsWithWeights(String[] terms, double[] weights);
}
