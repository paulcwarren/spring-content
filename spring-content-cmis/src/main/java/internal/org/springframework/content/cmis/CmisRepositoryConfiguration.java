package internal.org.springframework.content.cmis;

import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;

import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;

public interface CmisRepositoryConfiguration {

	CrudRepository cmisFolderRepository();

	CrudRepository getCmisFolderRepository();

	void setCmisFolderRepository(CrudRepository cmisFolderRepository);

	CrudRepository cmisDocumentRepository();

	CrudRepository getCmisDocumentRepository();

	void setCmisDocumentRepository(CrudRepository cmisDocumentRepository);

	ContentStore cmisDocumentStorage();

	ContentStore getCmisDocumentStorage();

	void setCmisDocumentStorage(ContentStore cmisDocumentStorage);

	RepositoryInfo getCmisRepositoryInfo();

	void setCmisRepositoryInfo(RepositoryInfo cmisRepositoryInfo);

	TypeDefinitionList getCmisTypeDefinitionList();

	void setCmisTypeDefinitionList(TypeDefinitionList cmisTypeDefinitionList);

	CmisNavigationService getCmisNavigationService();

}
