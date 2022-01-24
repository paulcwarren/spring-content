package internal.org.springframework.content.jpa.testsupport.stores;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.jpa.testsupport.models.Claim;

public interface ClaimStore extends ContentStore<Claim, String> {
}
