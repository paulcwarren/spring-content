package internal.org.springframework.content.rest.support.mockstore;

import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public interface MockContentStore<S, SID extends Serializable> extends ContentStore<S, SID> {
}
