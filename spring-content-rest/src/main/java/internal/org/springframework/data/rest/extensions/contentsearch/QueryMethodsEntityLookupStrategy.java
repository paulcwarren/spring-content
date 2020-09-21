package internal.org.springframework.data.rest.extensions.contentsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class QueryMethodsEntityLookupStrategy {

    public void lookup(RootResourceInformation rri, RepositoryInformation ri, List<Object> contentIds, List<Object> results) {

        Map<Object, Object> contentIdToEntities = new HashMap<>();
        for (Object contentId : contentIds) {
            contentIdToEntities.put(contentId, null);
        }

        ri.getQueryMethods().stream().forEach(m -> {

            List<Object> contentIdsToQueryFor = new ArrayList<>();
            int i=0;
            for (Object contentId : contentIdToEntities.keySet()) {
                if (contentIdToEntities.get(contentId) == null) {
                    contentIdsToQueryFor.add(contentId);
                    i++;
                    if (i == 250) {
                        continue;
                    }
                }
            }

            Map<String, List<Object>> map = Collections.singletonMap("contentIds", contentIdsToQueryFor);
            MultiValueMap<String, ? extends Object> args = new LinkedMultiValueMap<>(map);
            Optional<Object> partialResults = rri.getInvoker().invokeQueryMethod(m, args, Pageable.unpaged(), Sort.unsorted());
            if (partialResults.isPresent()) {
                results.addAll((List<Object>) partialResults.get());
            }
        });
    }
}