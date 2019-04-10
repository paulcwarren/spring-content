package internal.org.springframework.content.cmis;

import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;

import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;

public class CmisRepositoryConfigurationImpl implements CmisRepositoryConfiguration {

	private CmisNavigationService cmisNavigationService;
	private CrudRepository cmisFolderRepository;
	private CrudRepository cmisDocumentRepository;
	private ContentStore cmisDocumentStorage;

	private RepositoryInfo cmisRepositoryInfo;
	private TypeDefinitionList cmisTypeDefinitionList;

	public CmisNavigationService getCmisNavigationService() {
		return cmisNavigationService;
	}

	public void setCmisNavigationService(CmisNavigationService cmisNavigationService) {
		this.cmisNavigationService = cmisNavigationService;
	}

	public CrudRepository cmisFolderRepository() {
		return cmisFolderRepository;
	}

	public CrudRepository cmisDocumentRepository() {
		return cmisDocumentRepository;
	}

	public ContentStore cmisDocumentStorage() {
		return cmisDocumentStorage;
	}

	public CrudRepository getCmisFolderRepository() {
		return cmisFolderRepository;
	}

	public void setCmisFolderRepository(CrudRepository cmisFolderRepository) {
		this.cmisFolderRepository = cmisFolderRepository;
	}

	public CrudRepository getCmisDocumentRepository() {
		return cmisDocumentRepository;
	}

	public void setCmisDocumentRepository(CrudRepository cmisDocumentRepository) {
		this.cmisDocumentRepository = cmisDocumentRepository;
	}

	public ContentStore getCmisDocumentStorage() {
		return cmisDocumentStorage;
	}

	public void setCmisDocumentStorage(ContentStore cmisDocumentStorage) {
		this.cmisDocumentStorage = cmisDocumentStorage;
	}

	public TypeDefinitionList getCmisTypeDefinitionList() {
		return cmisTypeDefinitionList;
	}

	public void setCmisTypeDefinitionList(TypeDefinitionList cmisTypeDefinitionList) {
		this.cmisTypeDefinitionList = cmisTypeDefinitionList;
	}

	public RepositoryInfo getCmisRepositoryInfo() {
		return cmisRepositoryInfo;
	}

	public void setCmisRepositoryInfo(RepositoryInfo cmisRepositoryInfo) {
		this.cmisRepositoryInfo = cmisRepositoryInfo;
	}
}
