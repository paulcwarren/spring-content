package org.springframework.content.cmis.support;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.versions.LockingAndVersioningRepository;

public interface DocumentRepository extends JpaRepository<Document, Long>, LockingAndVersioningRepository<Document, Long> {

	List<Document> findAllByParent(Folder parent);

}
