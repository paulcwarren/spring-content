package org.springframework.content.cmis.configs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.cmis.support.Document;
import org.springframework.content.cmis.support.DocumentRepository;
import org.springframework.content.cmis.support.Folder;
import org.springframework.content.cmis.support.FolderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CmisNavigationConfig {

	@Bean
	public CmisNavigationService cmisNavigationService(FolderRepository folders, DocumentRepository docs) {

		return new CmisNavigationService<Folder>() {
			@Override
			public List getChildren(Folder parent) {

				List<Object> children = new ArrayList<>();
				List<Folder> folderChildern = folders.findAllByParent(parent);
				List<Document> documentChildren = docs.findAllByParent(parent);
				children.addAll(folderChildern);
				children.addAll(documentChildren);
				return children;
			}
		};
	}
}
