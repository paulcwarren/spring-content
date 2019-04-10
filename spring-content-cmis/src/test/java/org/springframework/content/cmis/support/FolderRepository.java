package org.springframework.content.cmis.support;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {

	List<Folder> findAllByParent(Folder parent);

}
