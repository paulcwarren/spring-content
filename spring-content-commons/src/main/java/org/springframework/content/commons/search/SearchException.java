package org.springframework.content.commons.search;


public class SearchException extends RuntimeException{
    public SearchException() { super("Missing extension implementation for service org.springframework.content.commons.search.Searchable"); }
}
