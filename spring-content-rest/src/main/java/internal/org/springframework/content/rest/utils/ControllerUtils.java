package internal.org.springframework.content.rest.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ControllerUtils {

    private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

    private ControllerUtils() {}

    public static <R extends RepresentationModel<?>> ResponseEntity<RepresentationModel<?>> toResponseEntity(HttpStatus status, HttpHeaders headers, Optional<R> resource) {
        HttpHeaders hdrs = new HttpHeaders();
        if (headers != null) {
            hdrs.putAll(headers);
        }

        return new ResponseEntity((RepresentationModel)resource.orElse(null), hdrs, status);
    }

    protected static CollectionModel<?> entitiesToResources(Page<Object> page,
            PagedResourcesAssembler<Object> prAssembler,
            PersistentEntityResourceAssembler assembler,
            Class<?> domainType) {

        if (page.getContent().isEmpty()) {
            return prAssembler.toEmptyModel(page, domainType);
        }

        if (assembler != null) {
            return prAssembler.toModel(page, assembler);
        } else {
            return prAssembler.toModel(page);
        }
    }

    protected static CollectionModel<?> entitiesToResources(Iterable<Object> entities,
            PersistentEntityResourceAssembler assembler,
            Class<?> domainType) {

        if (!entities.iterator().hasNext()) {

            List<Object> content = Arrays.<Object> asList(WRAPPERS.emptyCollectionOf(domainType));
            return CollectionModel.of(content, getDefaultSelfLink());
        }

        List<EntityModel<Object>> resources = new ArrayList<EntityModel<Object>>();

        for (Object obj : entities) {
            if (assembler != null) {
                resources.add(obj == null ? null : assembler.toModel(obj));
            } else {

                EntityModel m = EntityModel.of(obj);
                resources.add(m);
            }
        }

        return CollectionModel.of(resources, getDefaultSelfLink());
    }

    protected static Link getDefaultSelfLink() {
        return Link.of(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
    }

    public static CollectionModel<?> toCollectionModel(Iterable<?> source,
            PagedResourcesAssembler<Object> prAssembler,
            PersistentEntityResourceAssembler assembler,
            Class<?> domainType) {

        if (source instanceof Page) {
            Page<Object> page = (Page<Object>) source;
            return entitiesToResources(page, prAssembler, assembler, domainType);
        } else if (source instanceof Iterable) {
            return entitiesToResources((Iterable<Object>) source, assembler, domainType);
        } else {
            return CollectionModel.empty();
        }
    }
}
