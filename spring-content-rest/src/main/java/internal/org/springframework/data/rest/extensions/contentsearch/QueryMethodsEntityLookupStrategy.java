package internal.org.springframework.data.rest.extensions.contentsearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.content.rest.FulltextEntityLookupQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class QueryMethodsEntityLookupStrategy {

    private static final String CONTENT_IDS = "contentIds";

    public void lookup(RootResourceInformation rri, RepositoryInformation ri, List<Object> contentIds, List<Object> results) {

        Map<Object, Object> contentIdToEntities = new HashMap<>();
        for (Object contentId : contentIds) {
            contentIdToEntities.put(contentId, null);
        }

        ri.getQueryMethods().stream()
            .filter(m -> m.getAnnotation(FulltextEntityLookupQuery.class) != null)
            .forEach(m -> {

                int size = contentIds.size();

                for (int i=0; i < size; ) {
                    int lowerbound = i;
                    int upperbound = (lowerbound + 250 <= size ? lowerbound + 250 : size);

                    List<Object> subset = contentIds.subList(lowerbound, upperbound);

                    Map<String, List<Object>> map = Collections.singletonMap(CONTENT_IDS, subset);
                    MultiValueMap<String, ? extends Object> args = new LinkedMultiValueMap<>(map);
                    Optional<Object> partialResults = rri.getInvoker().invokeQueryMethod(m, args, Pageable.unpaged(), Sort.unsorted());
                    if (partialResults.isPresent()) {
                        results.addAll((List<Object>) partialResults.get());
                    }

                    i += 250;
                }
        });
    }
}