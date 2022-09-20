package internal.org.springframework.content.cmis;

import static java.lang.String.format;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import javax.persistence.Id;
import javax.transaction.Transactional;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.cmis.CmisDescription;
import org.springframework.content.cmis.CmisDocument;
import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.CmisName;
import org.springframework.content.cmis.CmisPropertySetter;
import org.springframework.content.cmis.CmisReference;
import org.springframework.content.cmis.CmisReferenceType;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CmisServiceBridge {

	private static final DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService(false);
	private static final String UNKNOWN = "<unknown>";

	private final CmisRepositoryConfiguration cmisRepositoryConfiguration;

	private Map<String, TypeDefinition> typeMap = new HashMap<>();

	public CmisServiceBridge(CmisRepositoryConfiguration cmisRepositoryConfiguration) {
		this.cmisRepositoryConfiguration = cmisRepositoryConfiguration;

		for (TypeDefinition typeDef : cmisRepositoryConfiguration.getCmisTypeDefinitionList().getList()) {
			typeMap.put(typeDef.getId(), typeDef);
		}

		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		registrar.registerFormatters(conversionService);

		conversionService.addConverter(new Converter<Instant, GregorianCalendar>() {
			@Override
			public GregorianCalendar convert(Instant source) {
				ZonedDateTime zdt = ZonedDateTime.ofInstant(source, ZoneId.systemDefault());
				return GregorianCalendar.from(zdt);
			}
		});
	}

	public TypeDefinitionList getTypeChildren(CmisRepositoryConfiguration config,
			String typeId,
			Boolean includePropertyDefinitions,
			BigInteger maxItems,
			BigInteger skipCount,
			ExtensionsData extension) {
		if (typeId == null) {
			return cmisRepositoryConfiguration.getCmisTypeDefinitionList();
		}
		return new TypeDefinitionListImpl(Collections.EMPTY_LIST);
	}

	public TypeDefinition getTypeDefinition(CmisRepositoryConfiguration config,
			String typeId,
			ExtensionsData extension) {

		return typeMap.get(typeId);
	}

	@Transactional
	public ObjectInFolderList getChildren(CmisRepositoryConfiguration config,
			String folderId,
			String filter,
			String orderBy,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships,
			String renditionFilter,
			Boolean includePathSegment,
			BigInteger maxItems,
			BigInteger skipCount,
			ExtensionsData extension,
			CallContext context,
			ObjectInfoHandler handler) {

		Set<String> filterCol = splitFilter(filter);

		Object parent = null;
		List<Object> children = new ArrayList<>();

		if (folderId.equals(getRootId()) == false) {
			parent = getObjectInternal(config,
					folderId,
					filterCol,
					false,
					IncludeRelationships.NONE,
					null,
					false,
					false,
					null
					);
		}

		children = config.getCmisNavigationService().getChildren(parent);

		return toObjectInFolderList(config, context, children, filterCol,true, handler);
	}

	@Transactional
	public List<ObjectParentData> getObjectParents(CmisRepositoryConfiguration config,
			String objectId,
			String filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships,
			String renditionFilter,
			Boolean includeRelativePathSegment,
			ExtensionsData extension,
			CallContext context,
			ObjectInfoHandler handler) {

		if (objectId.equals(getRootId())) {
			throw new CmisInvalidArgumentException("The root folder has no parent!");
		}

		Set<String> filterCol = splitFilter(filter);

		Object object = getObjectInternal(config,
				objectId,
				filterCol,
				false,
				IncludeRelationships.NONE,
				renditionFilter,
				false,
				false,
				extension);

		String parentProperty = findParentProperty(object);
		BeanWrapper wrapper = new BeanWrapperImpl(object);
		Object parents = wrapper.getPropertyValue(parentProperty);

		if (parents == null) {
			return Collections.emptyList();
		}

		if (context.isObjectInfoRequired()) {
			toObjectData(config, context, getType(object), object, false, filterCol, handler);
		}

		List<ObjectParentData> results = new ArrayList<>();

		ObjectParentDataImpl result = new ObjectParentDataImpl();

		// TODO: handle parents when it is a collection
		//
		result.setObject(toObjectData(config, context, typeMap.get("cmis:folder"), parents /* singleton only!*/, false, filterCol, handler));

		if (includeRelativePathSegment) {
			result.setRelativePathSegment(getName(object));
		}
		results.add(result);

		return results;
	}

	@Transactional
	public ObjectData getFolderParent(CmisRepositoryConfiguration config,
			String folderId,
			String filter,
			ExtensionsData extension,
			CallContext context,
			ObjectInfoHandler handler) {

		List<ObjectParentData> parentData = this.getObjectParents(config,
				folderId,
				filter,
				false,
				IncludeRelationships.NONE,
				null,
				false,
				extension,
				context,
				handler);

		if (parentData != null && parentData.size() > 0) {
			return parentData.get(0).getObject();
		}

		return toObjectData(config, context, typeMap.get("cmis:folder"), new Root(), true, null, handler);
	}

	@Transactional
	public ObjectData getObjectByPath(CmisRepositoryConfiguration config,
			String path,
			String filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships,
			String renditionFilter,
			Boolean includePolicyIds,
			Boolean includeAcl,
			ExtensionsData extension,
			CallContext context,
			ObjectInfoHandler handler) {

		if (path.equals("/")) {
			return toObjectData(config, context, typeMap.get("cmis:folder"), new Root(), true, null, handler);
		}

		if (path.startsWith("/")) {
			path = path.replaceFirst("/","");
		}

		String[] segments = path.split("/");

		Object object = null;
		for (int i=0; i < segments.length; i++) {
			List children = config.getCmisNavigationService().getChildren(object);
			object = null;
			for (int j=0; j < children.size() && object == null; j++) {
				Object child = children.get(j);
				String name = getAsString(child, CmisName.class);
				if (segments[i].equals(name)) {
					object = child;
				}
			}
		}

		if (object == null) {
			throw new CmisObjectNotFoundException(format("object not found for path %s", path));
		}

		return toObjectData(config, context, getType(object), object, false, null, handler);
	}

	@Transactional
	public ObjectData getObjectInternal(CmisRepositoryConfiguration config,
			String objectId,
			String filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships,
			String renditionFilter,
			Boolean includePolicyIds,
			Boolean includeAcl,
			ExtensionsData extension,
			CallContext context,
			ObjectInfoHandler handler) {

		Set<String> filterCol = splitFilter(filter);

		Object object = null;
		ObjectDataImpl result = null;
		ObjectInfoImpl objectInfo = new ObjectInfoImpl();

		if (objectId.equals(getRootId())) {
			object = new Root();
			result = toObjectData(config, context, getType(object), object, true, filterCol, handler);
		} else {
			object = this.getObjectInternal(config, objectId, filterCol, includeAllowableActions, includeRelationships,
					renditionFilter, includePolicyIds, includeAcl, extension);

			result = toObjectData(config, context, getType(object), object, false, filterCol, handler);
		}

		return result;
	}

	@Transactional
	public ContentStream getContentStream(CmisRepositoryConfiguration config,
			String objectId,
			String streamId,
			BigInteger offset,
			BigInteger length,
			ExtensionsData extension) {

		Object object = getObjectInternal(config, objectId, Collections.EMPTY_SET, false, IncludeRelationships.NONE,
				"", false, false, extension);
		if (object != null) {
			return new ContentStream() {

				@Override
				public long getLength() {
					return CmisServiceBridge.contentLength(object);
				}

				@Override
				public BigInteger getBigLength() {
					return BigInteger.valueOf(CmisServiceBridge.contentLength(object));
				}

				@Override
				public String getMimeType() {
					Object mimeType = BeanUtils.getFieldWithAnnotation(object, MimeType.class);
					return (mimeType != null) ? mimeType.toString() : null;
				}

				@Override
				public String getFileName() {
					return null;
				}

				@Override
				public InputStream getStream() {
					return config.cmisDocumentStorage().getContent(object);
				}

				@Override
				public List<CmisExtensionElement> getExtensions() {
					return null;
				}

				@Override
				public void setExtensions(List<CmisExtensionElement> extensions) {

				}
			};
		}
		return null;
	}

	@Transactional
	public void setContentStream(CmisRepositoryConfiguration config,
			Holder<String> objectId,
			Boolean overwriteFlag,
			Holder<String> changeToken,
			ContentStream contentStream,
			ExtensionsData extension) {

		Object object = getObjectInternal(config, objectId.getValue(), Collections.EMPTY_SET, false, IncludeRelationships.NONE,
				"", false, false, extension);
		if (object != null) {
			config.cmisDocumentStorage().setContent(object, contentStream.getStream());

			// re-fetch to ensure we have the latest
			object = getObjectInternal(config, objectId.getValue(), Collections.EMPTY_SET, false, IncludeRelationships.NONE,
					"", false, false, extension);

			if (BeanUtils.hasFieldWithAnnotation(object, MimeType.class)) {
				BeanUtils.setFieldWithAnnotation(object, MimeType.class, contentStream.getMimeType());
			}

			config.cmisDocumentRepository().save(object);
		}
	}

	void setContentStreamInternal(CmisRepositoryConfiguration config,
			ContentStream contentStream,
			Object object) {

		config.cmisDocumentStorage().setContent(object, contentStream.getStream());

		if (BeanUtils.hasFieldWithAnnotation(object, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(object, MimeType.class, contentStream.getMimeType());
		}

		config.cmisDocumentRepository().save(object);
	}

	@Transactional
	public void deleteContentStream(CmisRepositoryConfiguration config,
			Holder<String> objectId,
			Holder<String> changeToken,
			ExtensionsData extension) {

		Object object = getObjectInternal(config, objectId.getValue(), Collections.EMPTY_SET, false, IncludeRelationships.NONE,
				"", false, false, extension);

		if (object != null) {
			config.cmisDocumentStorage().unsetContent(object);
			config.cmisDocumentRepository().save(object);
		}
	}

	@Transactional
	public void updateProperties(CmisRepositoryConfiguration config,
			String objectId,
			String changeToken,
			Properties properties,
			ExtensionsData extension) {

		Long id = conversionService.convert(objectId, Long.class);

		Optional<Object> object = config.cmisDocumentRepository().findById(id);
		if (object.isPresent()) {
			CmisPropertySetter propSetter = new CmisPropertySetter(properties);
			propSetter.populate(object.get());
			config.cmisDocumentRepository().save(object);
		}
	}

	@Transactional
	public void checkOut(CmisRepositoryConfiguration config,
			String objectId,
			ExtensionsData extension,
			Boolean contentCopied) {

		Long id = conversionService.convert(objectId, Long.class);

		Optional<Object> object = config.cmisDocumentRepository().findById(id);
		if (object.isPresent()) {
			Object lockedObject = ((LockingAndVersioningRepository) config.cmisDocumentRepository()).lock(object.get());
			((LockingAndVersioningRepository) config.cmisDocumentRepository()).workingCopy(lockedObject);
		}
	}

	@Transactional
	public void cancelCheckOut(CmisRepositoryConfiguration config,
			String objectId,
			ExtensionsData extension) {

		Long id = conversionService.convert(objectId, Long.class);

		config.cmisDocumentRepository().findById(id)
			.ifPresent((obj) -> {

				Object pwc = ((LockingAndVersioningRepository)config.cmisDocumentRepository()).findWorkingCopy(obj);

				if (pwc != null) {
					Long ancestorId = (Long)BeanUtils.getFieldWithAnnotation(pwc, AncestorId.class);

					config.cmisDocumentRepository().findById(ancestorId)
						.ifPresent((ancestor) -> {

							// need to unfile before we can delete
							String parentProperty = findParentProperty(pwc);
							BeanWrapper childWrapper = new BeanWrapperImpl(pwc);
							Object parent = childWrapper.getPropertyValue(parentProperty);

							String childProperty = findChildProperty(parent);
							BeanWrapper parentWrapper = new BeanWrapperImpl(parent);
							Object child = parentWrapper.getPropertyValue(childProperty);
							if (child instanceof Collection) {
								((Collection)child).remove(pwc);
							} else {
								parentWrapper.setPropertyValue(childProperty, null);
							}

							if (parent instanceof Collection) {
								((Collection)parent).remove(ancestor);
							} else {
								childWrapper.setPropertyValue(parentProperty, null);
							}

							config.cmisDocumentStorage().unsetContent(pwc);

							((LockingAndVersioningRepository)config.cmisDocumentRepository()).delete(pwc);
							((LockingAndVersioningRepository)config.cmisDocumentRepository()).unlock(ancestor);
						});
				}
			});
	}

	@Transactional
	public void checkIn(CmisRepositoryConfiguration config,
			String objectId,
			Boolean major,
			Properties properties,
			ContentStream contentStream,
			String checkinComment,
			List<String> policies,
			Acl addAces,
			Acl removeAces,
			ExtensionsData extension) {

		Long id = conversionService.convert(objectId, Long.class);

		Optional<Object> object = config.cmisDocumentRepository().findById(id);

		if (object.isPresent()) {
			Object versionNumber = BeanUtils.getFieldWithAnnotation(object.get(), VersionNumber.class);
			if (versionNumber == null) {
				versionNumber = "0.0";
			}

			VersionInfo vi = new VersionInfo();
			vi.setNumber(incrementVersionNumber(versionNumber.toString(), major));
			vi.setLabel(checkinComment);

			Object newObjectVersion = ((LockingAndVersioningRepository) config.cmisDocumentRepository()).version(object.get(), vi);

			CmisPropertySetter propSetter = new CmisPropertySetter(properties);
			propSetter.populate(newObjectVersion);

			if (contentStream.getLength() > 0) {
				this.setContentStreamInternal(config, contentStream, newObjectVersion);

				// fetch to ensure we have the latest
				Optional<Object> fetched = config.cmisDocumentRepository().findById(getId(newObjectVersion));
				if (fetched.isPresent()) {
					newObjectVersion = fetched.get();
				}
			}

			((LockingAndVersioningRepository) config.cmisDocumentRepository()).unlock(newObjectVersion);
		}
	}

	String incrementVersionNumber(String versionNumber, Boolean isMajor) {

		int major = 0;
		int minor = 0;

		String[] elements =  versionNumber.split("\\.");

		if (elements.length != 2) {
			return versionNumber;
		}

		if (elements.length == 2) {
			major = Integer.parseInt(elements[0]);
			minor = Integer.parseInt(elements[1]);
		}

		if (isMajor) {
			major++;
		} else {
			minor++;
		}

		return major + "." + minor;
	}

    public List<ObjectData> getAllVersions(
            CmisRepositoryConfiguration config,
            String objectId,
            String versionSeriesId,
            String filter,
            Boolean includeAllowableActions,
            ExtensionsData extension,
            CallContext context,
            ObjectInfoHandler handler) {

        List<ObjectData> od = new ArrayList<>();

        Long id = conversionService.convert(objectId, Long.class);

        Optional<Object> object = config.cmisDocumentRepository().findById(id);

        if (object.isPresent()) {

            Field f = BeanUtils.findFieldWithAnnotation(object.get(), Id.class);
            Assert.notNull(f, "illegal state exception - cant find Id property");

            ((LockingAndVersioningRepository)config.cmisDocumentRepository()).findAllVersions(object.get(), Sort.by(Order.desc(f.getName()))).forEach((doc -> {
                ObjectData result = toObjectData(config, context, getType(doc), doc, false, null, handler);
                if (result != null) {
                    od.add(result);
                }
            }));
        }

        return od;
    }

	@Transactional
	public String createDocument(CmisRepositoryConfiguration config,
			Properties properties,
			String folderId,
			ContentStream contentStream,
			VersioningState versioningState,
			List<String> policies,
			Acl addAces,
			Acl removeAces,
			ExtensionsData extension) {

		Object object = null;
		Object id = null;
		CrudRepository repo = config.cmisDocumentRepository();
		Type repoIface = repo.getClass().getGenericInterfaces()[0];
		if (repoIface instanceof Class) {
			Type crudIface = ((Class) repoIface).getGenericInterfaces()[0];
			Type clazz = ((ParameterizedType)crudIface).getActualTypeArguments()[0];
			try {
				object = ((Class)clazz).newInstance();
			}
			catch (InstantiationException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			CmisPropertySetter propSetter = new CmisPropertySetter(properties);
			propSetter.populate(object);

			if (folderId != null) {
				// find CmisReference(type==Parent)
				String parentProperty = findParentProperty(object);
				if (parentProperty != null) {
					Object parent = getObjectInternal(config, folderId, Collections.EMPTY_SET, false, null,
							"", false, false, extension);

					BeanWrapper wrapper = new BeanWrapperImpl(object);
					wrapper.setPropertyValue(parentProperty, parent);
				}
				if (parentProperty == null) {
					// TODO: check cmis:folder type for child_relationship
				}
			}

			object = config.cmisDocumentRepository().save(object);
			id = getId(object);

			if (contentStream.getLength() > 0) {
				setContentStreamInternal(config, contentStream, object);
			}
		}

		return id.toString();
	}

	public void deleteObject(CmisRepositoryConfiguration config,
			String objectId,
			Boolean allVersions,
			ExtensionsData extension) {

		Object object = getObjectInternal(config, objectId, Collections.EMPTY_SET, false, IncludeRelationships.NONE, "", false, false, extension);
		if (object != null) {
            if (object.getClass().getAnnotation(CmisFolder.class) != null) {
                config.cmisFolderRepository().delete(object);
            } else if (object.getClass().getAnnotation(CmisDocument.class) != null) {

                if (!allVersions) {
                    config.cmisDocumentStorage().unsetContent(object);

                    // re-fetch to ensure we have the latest
                    object = getObjectInternal(config, objectId, Collections.EMPTY_SET, false, IncludeRelationships.NONE,
                            "", false, false, extension);

                    config.cmisDocumentRepository().delete(object);
                } else {
                    Field f = BeanUtils.findFieldWithAnnotation(object, Id.class);
                    Assert.notNull(f, "illegal state exception - cant find Id property");

                    ((LockingAndVersioningRepository)config.cmisDocumentRepository()).findAllVersions(object, Sort.by(Order.desc(f.getName()))).forEach((doc) -> {
                        config.cmisDocumentStorage().unsetContent(doc);
                    });

                    // re-fetch to ensure we have the latest
                    object = getObjectInternal(config, objectId, Collections.EMPTY_SET, false, IncludeRelationships.NONE,
                            "", false, false, extension);

                    ((LockingAndVersioningRepository)config.cmisDocumentRepository()).deleteAllVersions(object);
                }
            }
		}
	}

	@Transactional
	public String createFolder(CmisRepositoryConfiguration config,
			Properties properties,
			String folderId,
			List<String> policies,
			Acl addAces,
			Acl removeAces,
			ExtensionsData extension) {

		Object object = null;
		CrudRepository repo = config.cmisFolderRepository();
		Type repoIface = repo.getClass().getGenericInterfaces()[0];
		if (repoIface instanceof Class) {
			Type crudIface = ((Class) repoIface).getGenericInterfaces()[0];
			Type clazz = ((ParameterizedType)crudIface).getActualTypeArguments()[0];
			try {
				object = ((Class)clazz).newInstance();
			}
			catch (InstantiationException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			// TODO: copy properties
			CmisPropertySetter propSetter = new CmisPropertySetter(properties);
			propSetter.populate(object);

			// TODO: set folder (if applicable)
			if (folderId != null) {
				// find CmisReference(type==Parent)
				String parentProperty = findParentProperty(object);
				if (parentProperty != null) {
					Object parent = getObjectInternal(config, folderId, Collections.EMPTY_SET, false, null,
							"", false, false, extension);

					BeanWrapper wrapper = new BeanWrapperImpl(object);
					wrapper.setPropertyValue(parentProperty, parent);
				}
				if (parentProperty == null) {
					// TODO: check cmis:folder type for child_relationship
				}
			}
			object = config.cmisFolderRepository().save(object);
		}

		return getId(object).toString();
	}

	TypeDefinition getType(Object object) {
		Assert.notNull(object, () -> "object is null");

		if (object.getClass().getAnnotation(CmisFolder.class) != null) {
			return typeMap.get("cmis:folder");
		} else if (object.getClass().getAnnotation(CmisDocument.class) != null) {
			return typeMap.get("cmis:document");
		}

		throw new IllegalStateException(format("invalid type %s", object.getClass()));
	}

	ObjectInFolderList toObjectInFolderList(CmisRepositoryConfiguration config, CallContext callContext, Collection children, Set<String> filter, boolean root, ObjectInfoHandler objectInfos) {
		List<ObjectInFolderData> objectInFolderList = new ArrayList<>();
		ObjectInFolderListImpl list = new ObjectInFolderListImpl();
		list.setObjects(objectInFolderList);

		if (children == null) {
			return list;
		}

		for (Object child : children) {
			ObjectInFolderDataImpl folderData = new ObjectInFolderDataImpl();

			ObjectData object = toObjectData(config, callContext, getType(child), child, root, filter, objectInfos);
			folderData.setObject(object);

			objectInFolderList.add(folderData);
		}

		return list;
	}

	String getRootId() {
		return cmisRepositoryConfiguration.getCmisRepositoryInfo().getRootFolderId();
	}

	Object getObjectInternal(CmisRepositoryConfiguration config,
			String objectId,
			Set<String> filter,
			Boolean includeAllowableActions,
			IncludeRelationships includeRelationships,
			String renditionFilter,
			Boolean includePolicyIds,
			Boolean includeAcl,
			ExtensionsData extension) {

		if (objectId.equals(getRootId())) {
			return null;
		}

		Optional object = config.cmisFolderRepository().findById(Long.parseLong(objectId));
		if (!object.isPresent()) {
			object = config.cmisDocumentRepository().findById(Long.parseLong(objectId));
		}

		return object.isPresent() ? object.get() : null;
	}

	static String findParentProperty(Object object) {
		return findFirstProperty(object, CmisReferenceType.Parent);
	}

	public static String findChildProperty(Object object) {
		return findFirstProperty(object, CmisReferenceType.Child);
	}

	static String findFirstProperty(Object object, CmisReferenceType type) {
		String[] properties = findProperties(object, type);
		return (properties.length >= 1 ? properties[0] : null);
	}

	static String[] findProperties(Object object, CmisReferenceType type) {
		List<String> properties = new ArrayList<>();
		if (BeanUtils.hasFieldWithAnnotation(object, CmisReference.class)) {
			Field[] cmisPropertyFields = BeanUtils.findFieldsWithAnnotation(object.getClass(), CmisReference.class, new BeanWrapperImpl(object));
			for (Field cmisPropertyField : cmisPropertyFields) {
				CmisReference cmisProperty = cmisPropertyField.getAnnotation(CmisReference.class);
				if (cmisProperty.type() == type) {
					properties.add(cmisPropertyField.getName());
				}
			}
		}
		return properties.toArray(new String[]{});
	}

	static Object getId(Object object) {
		Object id;
		id = BeanUtils.getFieldWithAnnotation(object, Id.class);
		if (id == null) {
			id = BeanUtils.getFieldWithAnnotation(object, org.springframework.data.annotation.Id.class);
		}
		if (id == null) {
			id = "";
		}
		return id;
	}

	static String getName(Object object) {
		return BeanUtils.getFieldWithAnnotation(object, CmisName.class).toString();
	}

	static ObjectDataImpl toObjectData(CmisRepositoryConfiguration config,
			CallContext context,
			TypeDefinition type,
			Object object,
			boolean root,
			Set<String> filter,
			ObjectInfoHandler objectInfos) {

		ObjectDataImpl result = new ObjectDataImpl();
		ObjectInfoImpl objectInfo = new ObjectInfoImpl();

		compileObjectMetadata(objectInfo, type);

		result.setProperties(compileProperties(config, type, object, root, filter, objectInfo));

		result.setAllowableActions(compileAllowableActions(type, object, false, false));

		if (context.isObjectInfoRequired()) {
			objectInfo.setObject(result);
			objectInfos.addObjectInfo(objectInfo);
		}

		return result;
	}

	static PropertiesImpl compileProperties(CmisRepositoryConfiguration config,
			TypeDefinition type,
			Object object,
			boolean root,
			Set<String> orgfilter,
			ObjectInfoImpl objectInfo) {

		// copy filter
		Set<String> filter = (orgfilter == null ? null : new HashSet<>(orgfilter));

		Object id = BeanUtils.getFieldWithAnnotation(object, Id.class);
		Object name = BeanUtils.getFieldWithAnnotation(object, CmisName.class);
		PropertiesImpl props = new PropertiesImpl();
		addPropertyId(props, type, filter, PropertyIds.OBJECT_ID, id != null ? id.toString() : "");
		addPropertyString(props, type, filter, PropertyIds.NAME, name != null ? name.toString() : "");

		Object createdBy = BeanUtils.getFieldWithAnnotation(object, CreatedBy.class);
		addPropertyString(props, type, filter, PropertyIds.CREATED_BY, createdBy != null ? createdBy.toString() : UNKNOWN);
		objectInfo.setCreatedBy(createdBy != null ? createdBy.toString() : UNKNOWN);

		Object createdDate = BeanUtils.getFieldWithAnnotation(object, CreatedDate.class);
		if (createdDate != null) {
			if (conversionService.canConvert(createdDate.getClass(), Instant.class) == false) {
				throw new IllegalArgumentException(format("Unable to convert created date %s to java.time.Instant", createdDate));
			}

			Instant instant = conversionService.convert(createdDate, Instant.class);

			addPropertyString(props, type, filter, PropertyIds.CREATION_DATE, conversionService.convert(instant, String.class));

			objectInfo.setCreationDate(conversionService.convert(instant, GregorianCalendar.class));
		}

		Object modifiedBy = BeanUtils.getFieldWithAnnotation(object, LastModifiedBy.class);
		addPropertyString(props, type, filter, PropertyIds.LAST_MODIFIED_BY, modifiedBy != null ? modifiedBy.toString() : UNKNOWN);

		Object modifiedDate = BeanUtils.getFieldWithAnnotation(object, LastModifiedDate.class);
		if (modifiedDate != null) {
			if (conversionService.canConvert(modifiedDate.getClass(), Instant.class) == false) {
				throw new IllegalArgumentException(format("Unable to convert last modified date %s to java.time.Instant", modifiedDate));
			}

			Instant instant = conversionService.convert(modifiedDate, Instant.class);

			addPropertyString(props, type, filter, PropertyIds.LAST_MODIFICATION_DATE, conversionService.convert(instant, String.class));

			objectInfo.setLastModificationDate(conversionService.convert(instant, GregorianCalendar.class));
		}

		addPropertyString(props, type, filter, PropertyIds.DESCRIPTION, getAsString(object, CmisDescription.class));

		String typeId = null;

		boolean folder = type.getId().equals(BaseTypeId.CMIS_FOLDER.value());
		if (folder) {
			typeId = BaseTypeId.CMIS_FOLDER.value();

			addPropertyId(props, type, filter, PropertyIds.BASE_TYPE_ID, typeId);
			addPropertyId(props, type, filter, PropertyIds.OBJECT_TYPE_ID, typeId);
			addPropertyString(props, type, filter, PropertyIds.PATH, (root) ? "/" : getPath(object));

			if (root) {
				addPropertyId(props, type, filter, PropertyIds.PARENT_ID, (String)null);
				objectInfo.setHasParent(false);
			} else {
				addPropertyId(props, type, filter, PropertyIds.PARENT_ID, (getParentId(object) != null ? getParentId(object) : config.getCmisRepositoryInfo().getRootFolderId()));
				objectInfo.setHasParent(true);
			}
		} else {
			typeId = BaseTypeId.CMIS_DOCUMENT.value();

			addPropertyId(props, type, filter, PropertyIds.BASE_TYPE_ID, typeId);
			addPropertyId(props, type, filter, PropertyIds.OBJECT_TYPE_ID, typeId);

			if (((DocumentTypeDefinition)type).isVersionable()) {
				addPropertyBoolean(props, type, filter, PropertyIds.IS_IMMUTABLE, false);
				addPropertyBoolean(props, type, filter, PropertyIds.IS_LATEST_VERSION, isLatestVersion(object, config.cmisDocumentRepository()));
				addPropertyBoolean(props, type, filter, PropertyIds.IS_MAJOR_VERSION, isMajorVersion(object));
				addPropertyBoolean(props, type, filter, PropertyIds.IS_LATEST_MAJOR_VERSION, isLatestVersion(object, config.cmisDocumentRepository()) && isMajorVersion(object));
				addPropertyString(props, type, filter, PropertyIds.VERSION_LABEL, getAsString(object, VersionNumber.class));
 				addPropertyId(props, type, filter, PropertyIds.VERSION_SERIES_ID, getAsString(object, AncestorRootId.class));
 				Object pwc = getWorkingCopy(object, config.cmisDocumentRepository());
				addPropertyBoolean(props, type, filter, PropertyIds.IS_PRIVATE_WORKING_COPY, pwc != null && id.equals(getId(pwc)));
				addPropertyBoolean(props, type, filter, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, pwc != null);
				addPropertyId(props, type, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, getAsString(pwc, LockOwner.class));
				addPropertyString(props, type, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, getAsString(pwc, Id.class));
				addPropertyString(props, type, filter, PropertyIds.CHECKIN_COMMENT, getAsString(object, VersionLabel.class));

//				if (context.getCmisVersion() != CmisVersion.CMIS_1_0) {
//					addPropertyBoolean(result, typeId, filter,
//							PropertyIds.IS_PRIVATE_WORKING_COPY, false);
//				}
			}

			ContentStreamAllowed allowed = ((DocumentTypeDefinition)type).getContentStreamAllowed();
			if (allowed == ContentStreamAllowed.ALLOWED || allowed == ContentStreamAllowed.REQUIRED) {
				Long len = contentLength(object);
				if (len == 0L) {
					addPropertyBigInteger(props, type, filter, PropertyIds.CONTENT_STREAM_LENGTH, null);
					addPropertyString(props, type, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
					addPropertyString(props, type, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, null);

					objectInfo.setHasContent(false);
					objectInfo.setContentType(null);
					objectInfo.setFileName(null);
				}
				else {
					addPropertyInteger(props, type, filter, PropertyIds.CONTENT_STREAM_LENGTH, len);
					addPropertyString(props, type, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE,
							MimeTypes.getMIMEType(toString(BeanUtils.getFieldWithAnnotation(object, MimeType.class))));
					addPropertyString(props, type, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, toString(name));

					objectInfo.setHasContent(true);
					objectInfo.setContentType(MimeTypes.getMIMEType(toString(BeanUtils.getFieldWithAnnotation(object, MimeType.class))));
					objectInfo.setFileName(toString(name));
				}

				addPropertyId(props, type, filter, PropertyIds.CONTENT_STREAM_ID, getAsString(object, ContentId.class, null));
			}
		}
		return props;
	}

	static Object getWorkingCopy(Object object, CrudRepository repo) {
		return ((LockingAndVersioningRepository)repo).findWorkingCopy(object);
	}

	static void compileObjectMetadata(ObjectInfoImpl objectInfo, TypeDefinition type) {

		if (type.getId().equals(BaseTypeId.CMIS_FOLDER.value())) {
			objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
			objectInfo.setTypeId(BaseTypeId.CMIS_FOLDER.value());
			objectInfo.setContentType(null);
			objectInfo.setFileName(null);
			objectInfo.setHasAcl(true);
			objectInfo.setHasContent(false);
			objectInfo.setVersionSeriesId(null);
			objectInfo.setIsCurrentVersion(true);
			objectInfo.setRelationshipSourceIds(null);
			objectInfo.setRelationshipTargetIds(null);
			objectInfo.setRenditionInfos(null);
			objectInfo.setSupportsDescendants(true);
			objectInfo.setSupportsFolderTree(true);
			objectInfo.setSupportsPolicies(false);
			objectInfo.setSupportsRelationships(false);
			objectInfo.setWorkingCopyId(null);
			objectInfo.setWorkingCopyOriginalId(null);
		} else {
			objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
			objectInfo.setTypeId(BaseTypeId.CMIS_DOCUMENT.value());
			objectInfo.setHasAcl(true);
			objectInfo.setHasContent(true);
			objectInfo.setHasParent(true);
			objectInfo.setVersionSeriesId(null);
			objectInfo.setIsCurrentVersion(true);
			objectInfo.setRelationshipSourceIds(null);
			objectInfo.setRelationshipTargetIds(null);
			objectInfo.setRenditionInfos(null);
			objectInfo.setSupportsDescendants(false);
			objectInfo.setSupportsFolderTree(false);
			objectInfo.setSupportsPolicies(false);
			objectInfo.setSupportsRelationships(false);
			objectInfo.setWorkingCopyId(null);
			objectInfo.setWorkingCopyOriginalId(null);
		}
	}

	static AllowableActions compileAllowableActions(TypeDefinition type, Object object, boolean isRoot, boolean isPrincipalReadOnly) {
		AllowableActionsImpl result = new AllowableActionsImpl();

		Set<Action> aas = EnumSet.noneOf(Action.class);

		addAction(aas, Action.CAN_GET_OBJECT_PARENTS, false /*!isRoot*/);
		addAction(aas, Action.CAN_GET_PROPERTIES, true);
		addAction(aas, Action.CAN_UPDATE_PROPERTIES, !isPrincipalReadOnly /*&& !isReadOnly*/);
		addAction(aas, Action.CAN_MOVE_OBJECT, false /*!userReadOnly && !isRoot*/);
		addAction(aas, Action.CAN_DELETE_OBJECT, !isPrincipalReadOnly && !isRoot /*!userReadOnly && !isReadOnly && !isRoot*/);
		addAction(aas, Action.CAN_GET_ACL, false /*true*/);

		boolean folder = type.getId().equals(BaseTypeId.CMIS_FOLDER.value());
		if (folder) {
			addAction(aas, Action.CAN_GET_DESCENDANTS, false);
			addAction(aas, Action.CAN_GET_CHILDREN, true);
			addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
			addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
			addAction(aas, Action.CAN_CREATE_DOCUMENT, !isPrincipalReadOnly);
			addAction(aas, Action.CAN_CREATE_FOLDER, !isPrincipalReadOnly);
			addAction(aas, Action.CAN_DELETE_TREE, !isPrincipalReadOnly /*&& !isReadOnly*/);
		} else {
			ContentStreamAllowed allowed = ((DocumentTypeDefinition)type).getContentStreamAllowed();
			if (allowed == ContentStreamAllowed.ALLOWED || allowed == ContentStreamAllowed.REQUIRED) {
				addAction(aas, Action.CAN_GET_CONTENT_STREAM, contentLength(object) > 0);
				addAction(aas, Action.CAN_SET_CONTENT_STREAM, !isPrincipalReadOnly /*&& !isReadOnly*/);
				addAction(aas, Action.CAN_DELETE_CONTENT_STREAM, !isPrincipalReadOnly /*&& !isReadOnly*/);
				addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
			}

			if (((DocumentTypeDefinition)type).isVersionable()) {
				addAction(aas, Action.CAN_CHECK_IN, true);
				addAction(aas, Action.CAN_CHECK_OUT, true);
				addAction(aas, Action.CAN_CANCEL_CHECK_OUT, true);
			}
		}

		result.setAllowableActions(aas);

		return result;
	}

	static void addAction(Set<Action> aas, Action action, boolean condition) {
		if (condition) {
			aas.add(action);
		}
	}

	static String toString(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString();
	}

	static void addPropertyId(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, String value) {
		if (!checkAddProperty(props, type, filter, id)) {
			return;
		}

		props.addProperty(new PropertyIdImpl(id, value));
	}

	static void addPropertyString(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, String value) {
		if (!checkAddProperty(props, type, filter, id)) {
			return;
		}

		props.addProperty(new PropertyStringImpl(id, value));
	}

	static void addPropertyInteger(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, long value) {
		addPropertyBigInteger(props, type, filter, id,
				BigInteger.valueOf(value));
	}

	static void addPropertyBigInteger(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, BigInteger value) {
		if (!checkAddProperty(props, type, filter, id)) {
			return;
		}

		props.addProperty(new PropertyIntegerImpl(id, value));
	}

	static void addPropertyBoolean(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, boolean value) {
		if (!checkAddProperty(props, type, filter, id)) {
			return;
		}

		props.addProperty(new PropertyBooleanImpl(id, value));
	}

	static void addPropertyDateTime(PropertiesImpl props, TypeDefinition type, Set<String> filter, String id, GregorianCalendar value) {
		if (!checkAddProperty(props, type, filter, id)) {
			return;
		}

		props.addProperty(new PropertyDateTimeImpl(id, value));
	}

	static boolean checkAddProperty(Properties properties, TypeDefinition type, Set<String> filter, String id) {
		if ((properties == null) || (properties.getProperties() == null)) {
			throw new IllegalArgumentException("Properties must not be null!");
		}

		if (id == null) {
			throw new IllegalArgumentException("Id must not be null!");
		}

		if (!type.getPropertyDefinitions().containsKey(id)) {
			throw new IllegalArgumentException("Unknown property: " + id);
		}

		String queryName = type.getPropertyDefinitions().get(id).getQueryName();

		if ((queryName != null) && (filter != null)) {
			if (!filter.contains(queryName)) {
				return false;
			} else {
				filter.remove(queryName);
			}
		}

		return true;
	}

	static Long contentLength(Object object) {
		Object theLen = BeanUtils.getFieldWithAnnotation(object, ContentLength.class);
		if (theLen == null) {
			theLen = "0";
		}
		Long len = 0L;
		try {
			len = Long.parseLong(theLen.toString());
		} catch (NumberFormatException nfe) {}
		return len;
	}

	static Set<String> splitFilter(String filter) {
		if (filter == null) {
			return null;
		}

		if (filter.trim().length() == 0) {
			return null;
		}

		Set<String> result = new HashSet<String>();
		for (String s : filter.split(",")) {
			s = s.trim();
			if (s.equals("*")) {
				return null;
			} else if (s.length() > 0) {
				result.add(s);
			}
		}

		// set a few base properties
		// query name == id (for base type properties)
		result.add(PropertyIds.OBJECT_ID);
		result.add(PropertyIds.OBJECT_TYPE_ID);
		result.add(PropertyIds.BASE_TYPE_ID);

		return result;
	}

	static String getAsString(Object object, Class<? extends Annotation> annotation) {
		return getAsString(object, annotation, "");
	}

	static String getAsString(Object object, Class<? extends Annotation> annotation, String defaultValue) {
		if (object == null) {
			return defaultValue;
		}
		Object value = BeanUtils.getFieldWithAnnotation(object, annotation);
		return (value != null) ? value.toString() : "";
	}

	static Boolean getAsBoolean(Object object, Class<? extends Annotation> annotation) {
		Object value = BeanUtils.getFieldWithAnnotation(object, annotation);
		if (value != null && conversionService.canConvert(value.getClass(), Boolean.class)) {
			return conversionService.convert(value, Boolean.class);
		}
		return false;
	}

	static boolean isMajorVersion(Object object) {
		Object versionNumber = BeanUtils.getFieldWithAnnotation(object, VersionNumber.class);
		if (versionNumber != null) {
			return versionNumber.toString().endsWith(".0");
		}
		return false;
	}

	static Boolean isLatestVersion(Object object, CrudRepository repo) {
		return 	(BeanUtils.getFieldWithAnnotation(object, SuccessorId.class) == null) &&
				(((LockingAndVersioningRepository)repo).isPrivateWorkingCopy(object) == false);
	}

	static String getParentId(Object object) {
		String propName = findParentProperty(object);
		if (propName == null) {
			return "";
		}

		BeanWrapper wrapper = new BeanWrapperImpl(object);
		Object parent = wrapper.getPropertyValue(propName);

		if (parent == null) {
			return null;
		}

		return BeanUtils.getFieldWithAnnotation(parent, Id.class).toString();
	}

	static String getPath(Object object) {
		Vector<String> elements = new Vector<>();
		elements.add(BeanUtils.getFieldWithAnnotation(object, CmisName.class).toString());

		String propName = findParentProperty(object);
		if (propName == null) {
			return "/";
		}

		BeanWrapper wrapper = new BeanWrapperImpl(object);
		Object parent = null;
		while ((parent = wrapper.getPropertyValue(propName)) != null) {
			elements.insertElementAt(BeanUtils.getFieldWithAnnotation(parent, CmisName.class).toString(), 0);
			wrapper = new BeanWrapperImpl(parent);
		}

		StringBuilder builder = new StringBuilder();
		for (int i=0; i < elements.size(); i++) {
			builder.append("/");
			builder.append(encode(elements.get(i)));
		}

		return builder.toString();
	}

	static String encode(String value) {
		return value;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@CmisFolder
	public class Root {

		@Id
		private String id = CmisServiceBridge.this.cmisRepositoryConfiguration.getCmisRepositoryInfo().getRootFolderId();

		@CmisName
		private String name = "";
	}
}
