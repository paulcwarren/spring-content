package org.springframework.content.commons.mappingcontext;

@FunctionalInterface
public interface NameProvider {

    String name(String fieldName);
}
