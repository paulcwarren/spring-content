package internal.org.springframework.content.cmis;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;

public class ContentCmisService extends AbstractCmisService implements CallContextAwareCmisService {

	private final CmisRepositoryConfiguration config;
	private final CmisServiceBridge bridge;

	private CallContext context;

	public ContentCmisService(CmisRepositoryConfiguration config, CmisServiceBridge bridge) {
		this.config = config;
		this.bridge = bridge;
	}

	@Override
	public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extensionsData) {

		return Collections.singletonList(config.getCmisRepositoryInfo());
	}

	@Override
	public TypeDefinitionList getTypeChildren(String repositoryId,
											  String typeId,
											  Boolean includePropertyDefinitions,
											  BigInteger maxItems,
											  BigInteger skipCount,
											  ExtensionsData extension) {

		return bridge.getTypeChildren(config,
				typeId,
				includePropertyDefinitions,
				maxItems,
				skipCount,
				extension);
	}

	@Override
	public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {

		return bridge.getTypeDefinition(config,
				typeId,
				extension);
	}

	@Override
	public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
										  Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
										  Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		return bridge.getChildren(config,
				folderId,
				filter,
				orderBy,
				includeAllowableActions,
				includeRelationships,
				renditionFilter,
				includePathSegment,
				maxItems,
				skipCount,
				extension,
				this.getCallContext(),
				this);
	}

	@Override
	public List<ObjectParentData> getObjectParents(String repositoryId,
												   String objectId, String filter, Boolean includeAllowableActions,
												   IncludeRelationships includeRelationships, String renditionFilter,
												   Boolean includeRelativePathSegment, ExtensionsData extension) {
		return bridge.getObjectParents(config,
				objectId,
				filter,
				includeAllowableActions,
				includeRelationships,
				renditionFilter,
				includeRelativePathSegment,
				extension,
				this.getCallContext(),
				this);
	}

	@Override
	public ObjectData getFolderParent(String repositoryId,
			String folderId,
			String filter,
			ExtensionsData extension) {

		return bridge.getFolderParent(config,
				folderId,
				filter,
				extension,
				this.getCallContext(),
				this);
	}

	@Override
	public ObjectData getObject(String repositoryId, String objectId,
								String filter, Boolean includeAllowableActions,
								IncludeRelationships includeRelationships, String renditionFilter,
								Boolean includePolicyIds, Boolean includeAcl,
								ExtensionsData extension) {

		return bridge.getObjectInternal(config,
				objectId,
				filter,
				includeAllowableActions,
				includeRelationships,
				renditionFilter,
				includePolicyIds,
				includeAcl,
				extension,
				this.getCallContext(),
				this);
	}

	public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions,
			IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
			Boolean includeAcl, ExtensionsData extension) {

		return bridge.getObjectByPath(config,
				path,
				filter,
				includeAllowableActions,
				includeRelationships,
				renditionFilter,
				includePolicyIds,
				includeAcl,
				extension,
				this.getCallContext(),
				this);
	}

	public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset,
										  BigInteger length, ExtensionsData extension) {

		return bridge.getContentStream(config,
				objectId,
				streamId,
				offset,
				length,
				extension);
	}

	public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken,
								 ContentStream contentStream, ExtensionsData extension) {

		bridge.setContentStream(config, objectId, overwriteFlag, changeToken, contentStream, extension);
	}

	public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ExtensionsData extension) {

		bridge.deleteContentStream(config,
				objectId,
				changeToken,
				extension);
	}

	public void updateProperties(String repositoryId,
			Holder<String> objectId,
			Holder<String> changeToken,
			Properties properties,
			ExtensionsData extension) {

		bridge.updateProperties(config,
				objectId.getValue(),
				(changeToken != null) ? changeToken.getValue() : null,
				properties,
				extension);
	}

	@Override
	public void checkOut(String repositoryId,
			Holder<String> objectId,
			ExtensionsData extension,
			Holder<Boolean> contentCopied) {

		bridge.checkOut(config,
				objectId.getValue(),
				extension,
				(contentCopied != null) ? contentCopied.getValue() : false);
	}

	@Override
	public void cancelCheckOut(String repositoryId,
			String objectId,
			ExtensionsData extension) {

		bridge.cancelCheckOut(config,
				objectId,
				extension);
	}

	public void checkIn(String repositoryId,
			Holder<String> objectId,
			Boolean major,
			Properties properties,
			ContentStream contentStream,
			String checkinComment,
			List<String> policies,
			Acl addAces, Acl
			removeAces,
			ExtensionsData extension) {

		bridge.checkIn(config,
				objectId.getValue(),
				major,
				properties,
				contentStream,
				checkinComment,
				policies,
				addAces,
				removeAces,
				extension);
	}

	public String createDocument(String repositoryId,
			Properties properties,
			String folderId,
			ContentStream contentStream,
			VersioningState versioningState,
			List<String> policies,
			Acl addAces,
			Acl removeAces,
			ExtensionsData extension) {

		return bridge.createDocument(config,
				properties,
				folderId,
				contentStream,
				versioningState,
				policies,
				addAces,
				removeAces,
				extension);
	}

	public void deleteObjectOrCancelCheckOut(String repositoryId,
			String objectId,
			Boolean allVersions,
			ExtensionsData extension) {

		bridge.deleteObject(config,
				objectId,
				allVersions,
				extension);
	}

	public String createFolder(String repositoryId,
			Properties properties,
			String folderId,
			List<String> policies,
			Acl addAces,
			Acl removeAces,
			ExtensionsData extension) {

		return bridge.createFolder(config,
				properties,
				folderId,
				policies,
				addAces,
				removeAces,
				extension);
	}

	@Override
	public CallContext getCallContext() {
		return context;
	}

	@Override
	public void setCallContext(CallContext context) {
		this.context = context;
	}

}
