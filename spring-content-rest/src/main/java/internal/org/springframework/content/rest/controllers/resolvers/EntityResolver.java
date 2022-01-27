package internal.org.springframework.content.rest.controllers.resolvers;

public interface EntityResolver {
    public String getMapping();
    public EntityResolution resolve(String path);
    public boolean hasPropertyFor(String path);
}
