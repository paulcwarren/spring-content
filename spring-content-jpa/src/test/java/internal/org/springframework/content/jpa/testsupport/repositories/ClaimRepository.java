package internal.org.springframework.content.jpa.testsupport.repositories;

import org.springframework.data.repository.CrudRepository;

import internal.org.springframework.content.jpa.testsupport.models.Claim;

public interface ClaimRepository extends CrudRepository<Claim, String> {
}
