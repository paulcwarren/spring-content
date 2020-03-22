package org.springframework.content.elasticsearch;

public interface IndexingStrategy {

    void setup() throws Exception;

    String indexName();

}
