package internal.org.springframework.content.jpa.testsupport.repositories;

import org.springframework.data.repository.CrudRepository;

import internal.org.springframework.content.jpa.testsupport.models.Document;

public interface DocumentRepository extends CrudRepository<Document, Long> {
}
