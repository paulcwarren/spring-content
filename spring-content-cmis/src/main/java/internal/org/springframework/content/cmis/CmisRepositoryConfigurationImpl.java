package internal.org.springframework.content.cmis;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.ReflectionUtils;

public class CmisRepositoryConfigurationImpl implements CmisRepositoryConfiguration, InitializingBean {

	private RepositoryInfo cmisRepositoryInfo;
	private TypeDefinitionList cmisTypeDefinitionList;
	private CrudRepository cmisFolderRepository;
	private CrudRepository cmisDocumentRepository;
	private ContentStore cmisDocumentStorage;
	private Optional<CmisNavigationService> cmisNavigationService;

	@Autowired
	public CmisRepositoryConfigurationImpl(Optional<CmisNavigationService> cmisNavigationService) {
		this.cmisNavigationService = cmisNavigationService;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (cmisNavigationService.isPresent() == false) {
			cmisNavigationService = Optional.of(new DefaultCmisNavigationService(cmisFolderRepository, cmisDocumentRepository));
		}
	}

	public CmisNavigationService getCmisNavigationService() {
		return cmisNavigationService.get();
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

	private class DefaultCmisNavigationService implements CmisNavigationService {

		private CrudRepository[] repositories;

		public DefaultCmisNavigationService(CrudRepository...repositories) {
			this.repositories = repositories;
		}

		@Override
		public List getChildren(Object parent) {
			List<Object> children = new ArrayList<>();

			for (CrudRepository repo : repositories) {
				String parentProp = null;
				Iterable candidates = repo.findAll();
				for (Object candidate : candidates) {
					if (parentProp == null) {
						parentProp = CmisServiceBridge.findParentProperty(candidate);
					}
					if (parentProp != null) {
						BeanWrapper wrapper = new BeanWrapperImpl(candidate);
						PropertyDescriptor descriptor = wrapper.getPropertyDescriptor(parentProp);
						if (descriptor != null && descriptor.getReadMethod() != null) {
							Object actualParent = ReflectionUtils.invokeMethod(descriptor.getReadMethod(), candidate);
							if (Objects.equals(parent, actualParent)) {
								children.add(candidate);
							}
						}
					}
				}
			}

			return children;
		}
	}
}
