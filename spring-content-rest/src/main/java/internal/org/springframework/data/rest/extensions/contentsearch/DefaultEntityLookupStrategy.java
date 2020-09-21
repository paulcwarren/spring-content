package internal.org.springframework.data.rest.extensions.contentsearch;

import java.util.List;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.RootResourceInformation;

public class DefaultEntityLookupStrategy {

    public void lookup(RootResourceInformation rri, RepositoryInformation ri, List<Object> contentIds, List<Object> results) {

        RepositoryInvoker invoker = rri.getInvoker();
        Iterable<?> entities = invoker.invokeFindAll(Pageable.unpaged());

        for (Object entity : entities) {
            for (Object contentId : contentIds) {

                Object candidate = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
                if (contentId.equals(candidate)) {
                    results.add(entity);
                }
            }
        }
    }
}
