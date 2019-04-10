package internal.org.springframework.content.cmis;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;

import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableFolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.content.cmis.CmisDescription;
import org.springframework.content.cmis.CmisDocument;
import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.CmisName;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import static java.lang.String.format;

public class CmisTypeDefinitionFactoryBean implements BeanFactoryAware, FactoryBean<TypeDefinition>, BeanClassLoaderAware, ResourceLoaderAware, EnvironmentAware {

	private ClassLoader classLoader;
	private BeanFactory beanFactory;
	private ResourceLoader resourceLoader;
	private Environment environment;

	private Class<?> entityClass;
	private Class<?> repoClass;
	private Class<?> storeClass;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public void setRepoClass(Class<?> repoClass) {
		this.repoClass = repoClass;
	}

	public void setStoreClass(Class<?> storeClass) {
		this.storeClass = storeClass;
	}

	@Override
	public TypeDefinition getObject() throws Exception {
		CmisDocument cmisDocumentMetadata = entityClass.getAnnotation(CmisDocument.class);
		CmisFolder cmisFolderMetadata = entityClass.getAnnotation(CmisFolder.class);
		if (cmisDocumentMetadata != null) {
			return createDocumentTypeDefinition(entityClass, repoClass, storeClass, cmisDocumentMetadata, CmisVersion.CMIS_1_1, null);
		} else if (cmisFolderMetadata != null) {
			return createFolderTypeDefinition(entityClass, repoClass, cmisFolderMetadata, CmisVersion.CMIS_1_1, null);
		}
		throw new IllegalStateException(format("Unknown type definition: %s", entityClass));
	}

	@Override
	public Class<?> getObjectType() {
		return DocumentTypeDefinitionImpl.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public MutableDocumentTypeDefinition createDocumentTypeDefinition(Class<?> entityClass, Class<?> repoClass, Class<?> storeClass, CmisDocument metadata, CmisVersion cmisVersion, String parentId) {
		MutableDocumentTypeDefinition documentType = new DocumentTypeDefinitionImpl();
		documentType.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
		documentType.setParentTypeId(parentId);
		documentType.setIsControllableAcl(false);
		documentType.setIsControllablePolicy(false);
		documentType.setIsCreatable(repoClass != null);
		documentType.setDescription("Document");
		documentType.setDisplayName("Document");
		documentType.setIsFileable(isFileable(repoClass));
		documentType.setIsFulltextIndexed(isFulltextIndexed(repoClass));
		documentType.setIsIncludedInSupertypeQuery(true);
		documentType.setLocalName("Document");
		documentType.setLocalNamespace("");
		documentType.setIsQueryable(false);
		documentType.setQueryName("cmis:document");
		documentType.setId(BaseTypeId.CMIS_DOCUMENT.value());
		if (cmisVersion != CmisVersion.CMIS_1_0) {
			TypeMutabilityImpl typeMutability = new TypeMutabilityImpl();
			typeMutability.setCanCreate(false);
			typeMutability.setCanUpdate(false);
			typeMutability.setCanDelete(false);
			documentType.setTypeMutability(typeMutability);
		}

		documentType.setIsVersionable(isVersionable(repoClass));
		documentType.setContentStreamAllowed((storeClass != null) ? ContentStreamAllowed.ALLOWED : ContentStreamAllowed.NOTALLOWED);

		this.addBasePropertyDefinitions(documentType, entityClass, cmisVersion, parentId != null);
		this.addDocumentPropertyDefinitions(documentType, repoClass, storeClass, cmisVersion, parentId != null);
		return documentType;
	}

	protected void addBasePropertyDefinitions(MutableTypeDefinition type, Class<?> entityClazz, CmisVersion cmisVersion, boolean inherited) {
		Field cmisNameField = BeanUtils.findFieldWithAnnotation(entityClazz, CmisName.class);
		if (cmisNameField != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:name", "Name", "Name", propertyType(cmisNameField), cardinality(cmisNameField), updatability(entityClazz, cmisNameField), inherited, true, false, false));
		}
		if (cmisVersion != CmisVersion.CMIS_1_0) {
			Field cmisDescField = BeanUtils.findFieldWithAnnotation(entityClazz, CmisDescription.class);
			if (cmisDescField != null) {
				type.addPropertyDefinition(this.createPropertyDefinition("cmis:description", "Description", "Description", propertyType(cmisDescField), cardinality(cmisDescField), updatability(entityClazz, cmisDescField), inherited, false, false, false));
			}
		}

		Field idField = BeanUtils.findFieldWithAnnotation(entityClazz, Id.class);
		if (idField == null) {
			idField = BeanUtils.findFieldWithAnnotation(entityClazz, org.springframework.data.annotation.Id.class);
		}
		if (idField != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:objectId", "Object Id", "Object Id", PropertyType.ID, cardinality(idField), Updatability.READONLY, inherited, false, false, false));
		}

		type.addPropertyDefinition(this.createPropertyDefinition("cmis:baseTypeId", "Base Type Id", "Base Type Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
		type.addPropertyDefinition(this.createPropertyDefinition("cmis:objectTypeId", "Object Type Id", "Object Type Id", PropertyType.ID, Cardinality.SINGLE, Updatability.ONCREATE, inherited, true, false, false));
		if (cmisVersion != CmisVersion.CMIS_1_0) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:secondaryObjectTypeIds", "Secondary Type Ids", "Secondary Type Ids", PropertyType.ID, Cardinality.MULTI, Updatability.READONLY, inherited, false, false, false));
		}

		Field field = BeanUtils.findFieldWithAnnotation(entityClazz, CreatedBy.class);
		if (field != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:createdBy", "Created By", "Created By", propertyType(field), cardinality(field), Updatability.READONLY, inherited, false, false, false));
		}
		field = BeanUtils.findFieldWithAnnotation(entityClazz, CreatedDate.class);
		if (field != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:creationDate", "Creation Date", "Creation Date", propertyType(field), cardinality(field), Updatability.READONLY, inherited, false, false, false));
		}
		field = BeanUtils.findFieldWithAnnotation(entityClazz, LastModifiedBy.class);
		if (field != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:lastModifiedBy", "Last Modified By", "Last Modified By", propertyType(field), cardinality(field), Updatability.READONLY, inherited, false, false, false));
		}
		field = BeanUtils.findFieldWithAnnotation(entityClazz, LastModifiedDate.class);
		if (field != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:lastModificationDate", "Last Modification Date", "Last Modification Date", propertyType(field), cardinality(field), Updatability.READONLY, inherited, false, false, false));
		}
		field = BeanUtils.findFieldWithAnnotation(entityClazz, Version.class);
		if (field != null) {
			type.addPropertyDefinition(this.createPropertyDefinition("cmis:changeToken", "Change Token", "Change Token", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
		}
	}

	protected void addDocumentPropertyDefinitions(MutableDocumentTypeDefinition type, Class<?> repoClazz, Class<?> storeClazz, CmisVersion cmisVersion, boolean inherited) {

		if (storeClass != null) {
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:contentStreamLength", "Content Stream Length", "Content Stream Length", PropertyType.INTEGER, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:contentStreamMimeType", "MIME Type", "MIME Type", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:contentStreamFileName", "Filename", "Filename", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:contentStreamId", "Content Stream Id", "Content Stream Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
		}

		if (isVersionable(repoClass)) {
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:isImmutable", "Is Immutable", "Is Immutable", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:isLatestVersion", "Is Latest Version", "Is Latest Version", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:isMajorVersion", "Is Major Version", "Is Major Version", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:isLatestMajorVersion", "Is Latest Major Version", "Is Latest Major Version", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			if (cmisVersion != CmisVersion.CMIS_1_0) {
				type.addPropertyDefinition(this
						.createPropertyDefinition("cmis:isPrivateWorkingCopy", "Is Private Working Copy", "Is Private Working Copy", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, true, false));
			}
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:versionLabel", "Version Label", "Version Label", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, true, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:versionSeriesId", "Version Series Id", "Version Series Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, inherited, false, true, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:isVersionSeriesCheckedOut", "Is Version Series Checked Out", "Is Verison Series Checked Out", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, inherited, false, true, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:versionSeriesCheckedOutBy", "Version Series Checked Out By", "Version Series Checked Out By", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:versionSeriesCheckedOutId", "Version Series Checked Out Id", "Version Series Checked Out Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:checkinComment", "Checkin Comment", "Checkin Comment", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
		}
	}

	public MutableFolderTypeDefinition createFolderTypeDefinition(Class<?> entityClass, Class<?> repoClass, CmisFolder metadata, CmisVersion cmisVersion, String parentId) {
		MutableFolderTypeDefinition folderType = new FolderTypeDefinitionImpl();
		folderType.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
		folderType.setParentTypeId(parentId);
		folderType.setIsControllableAcl(false);
		folderType.setIsControllablePolicy(false);
		folderType.setIsCreatable(repoClass != null);
		folderType.setDescription("Folder");
		folderType.setDisplayName("Folder");
		folderType.setIsFileable(isFileable(repoClass));
		folderType.setIsFulltextIndexed(false);
		folderType.setIsIncludedInSupertypeQuery(true);
		folderType.setLocalName("Folder");
		folderType.setLocalNamespace("");
		folderType.setIsQueryable(false);
		folderType.setQueryName("cmis:folder");
		folderType.setId(BaseTypeId.CMIS_FOLDER.value());
		if (cmisVersion != CmisVersion.CMIS_1_0) {
			TypeMutabilityImpl typeMutability = new TypeMutabilityImpl();
			typeMutability.setCanCreate(false);
			typeMutability.setCanUpdate(false);
			typeMutability.setCanDelete(false);
			folderType.setTypeMutability(typeMutability);
		}

		this.addBasePropertyDefinitions(folderType, entityClass, cmisVersion, parentId != null);
		this.addFolderPropertyDefinitions(folderType, cmisVersion, parentId != null);
		return folderType;
	}

	protected void addFolderPropertyDefinitions(MutableFolderTypeDefinition type, CmisVersion cmisVersion, boolean inherited) {

		if (true /* is fileable */) {
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:parentId", "Parent Id", "Parent Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:path", "Path", "Path", PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, inherited, false, false, false));
			type.addPropertyDefinition(this
					.createPropertyDefinition("cmis:allowedChildObjectTypeIds", "Allowed Child Object Type Ids", "Allowed Child Object Type Ids", PropertyType.ID, Cardinality.MULTI, Updatability.READONLY, inherited, false, false, false));
		}
	}

	public MutablePropertyDefinition<?> createPropertyDefinition(String id, String displayName, String description, PropertyType datatype, Cardinality cardinality, Updatability updateability, boolean inherited, boolean required, boolean queryable, boolean orderable) {
		if (id == null) {
			throw new IllegalArgumentException("ID must be set!");
		} else if (datatype == null) {
			throw new IllegalArgumentException("Datatype must be set!");
		} else if (cardinality == null) {
			throw new IllegalArgumentException("Cardinality must be set!");
		} else if (updateability == null) {
			throw new IllegalArgumentException("Updateability must be set!");
		} else {
			MutablePropertyDefinition<?> result = null;
			switch(datatype) {
			case BOOLEAN:
				result = new PropertyBooleanDefinitionImpl();
				break;
			case DATETIME:
				result = new PropertyDateTimeDefinitionImpl();
				break;
			case DECIMAL:
				result = new PropertyDecimalDefinitionImpl();
				break;
			case HTML:
				result = new PropertyHtmlDefinitionImpl();
				break;
			case ID:
				result = new PropertyIdDefinitionImpl();
				break;
			case INTEGER:
				result = new PropertyIntegerDefinitionImpl();
				break;
			case STRING:
				result = new PropertyStringDefinitionImpl();
				break;
			case URI:
				result = new PropertyUriDefinitionImpl();
				break;
			default:
				throw new IllegalArgumentException("Unknown datatype! Spec change?");
			}

			result.setId(id);
			result.setLocalName(id);
			result.setDisplayName(displayName);
			result.setDescription(description);
			result.setPropertyType(datatype);
			result.setCardinality(cardinality);
			result.setUpdatability(updateability);
			result.setIsInherited(inherited);
			result.setIsRequired(required);
			result.setIsQueryable(queryable);
			result.setIsOrderable(orderable);
			result.setQueryName(id);
			return result;
		}
	}

	PropertyType propertyType(Field field) {

		if (field.getType().equals(String.class)) {
			return PropertyType.STRING;
		} else if (field.getType().equals(Boolean.class)) {
			return PropertyType.BOOLEAN;
		} else if (field.getType().equals(Date.class)) {
			return PropertyType.DATETIME;
		} else if (field.getType().equals(Integer.class)) {
			return PropertyType.INTEGER;
		} else if (field.getType().equals(Double.class) || field.getType().equals(Float.class)) {
			return PropertyType.DECIMAL;
		} else if (field.getType().equals(URI.class)) {
			return PropertyType.URI;
		} else if ((field.getType().getAnnotation(Id.class) == null) || (field.getType().getAnnotation(org.springframework.data.annotation.Id.class) == null)) {
			return PropertyType.ID;
		}

		throw new IllegalStateException(format("Unable to convert type for field: %s", field.getName()));
	}

	Cardinality cardinality(Field field) {

		return (org.springframework.beans.BeanUtils.isSimpleValueType(field.getType())) ? Cardinality.SINGLE : Cardinality.MULTI;
	}

	Updatability updatability(Class<?> clazz, Field field) {

		PropertyDescriptor descriptor = org.springframework.beans.BeanUtils.getPropertyDescriptor(clazz, field.getName());
		return (descriptor.getWriteMethod() != null) ? Updatability.READWRITE : Updatability.READONLY;
	}

	private Boolean isFulltextIndexed(Class<?> storeClass) {
		Type[] types = repoClass.getGenericInterfaces();
		for (Type type : types) {
			if ("org.springframework.content.commons.search.Searchable".equals(type.getTypeName())) {
				return true;
			}
		}
		return false;
	}

	private Boolean isVersionable(Class<?> repoClass) {
		Type[] types = repoClass.getGenericInterfaces();
		for (Type type : types) {
			if (type instanceof ParameterizedType) {
				if ("org.springframework.versions.LockingAndVersioningRepository".equals(((ParameterizedType)type).getRawType().getTypeName())) {
					return true;
				}
			}
		}
		return false;
	}

	private Boolean isFileable(Class<?> repoClass) {
		return true;
	}

	private Boolean isMultiFileable(Class<?> repoClass) {
		return true;
	}
}
