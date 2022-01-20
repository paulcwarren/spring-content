package internal.org.springframework.content.rest.controllers.resolvers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.AntPathMatcher;

public class EntityResolvers extends ArrayList<EntityResolver> {

    private static final long serialVersionUID = -7652384665879868640L;

    public EntityResolution resolve(String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        Comparator<String> patternComparator = matcher.getPatternComparator(path);

        List<String> entityUriTemplates = new ArrayList<>();
        for (EntityResolver resolver : this) {
            if (matcher.match(resolver.getMapping(), path)) {
                entityUriTemplates.add(resolver.getMapping());
            }
        }

        String bestMatch = null;
        if (entityUriTemplates.size() > 1) {
            entityUriTemplates.sort(patternComparator);
        }

        bestMatch = entityUriTemplates.get(0);

        EntityResolver matchedEntityResolver = null;
        for (EntityResolver resolver : this) {
            if (bestMatch.equals(resolver.getMapping())) {
                matchedEntityResolver = resolver;
            }
        }

        return matchedEntityResolver.resolve(path);
    }

    public boolean hasPropertyFor(String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        Comparator<String> patternComparator = matcher.getPatternComparator(path);

        List<String> entityUriTemplates = new ArrayList<>();
        for (EntityResolver resolver : this) {
            if (matcher.match(resolver.getMapping(), path)) {
                entityUriTemplates.add(resolver.getMapping());
            }
        }

        String bestMatch = null;
        if (entityUriTemplates.size() > 1) {
            entityUriTemplates.sort(patternComparator);
        }

        bestMatch = entityUriTemplates.get(0);

        EntityResolver matchedEntityResolver = null;
        for (EntityResolver resolver : this) {
            if (bestMatch.equals(resolver.getMapping())) {
                matchedEntityResolver = resolver;
            }
        }

        return matchedEntityResolver.hasPropertyFor(path);
    }
}
