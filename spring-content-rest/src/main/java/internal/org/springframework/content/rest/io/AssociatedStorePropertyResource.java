package internal.org.springframework.content.rest.io;

public interface AssociatedStorePropertyResource<S> extends AssociatedStoreResource<S> {

    public boolean embedded();

    public Object getProperty();
}
