package it.typesupport.model;

import org.springframework.content.commons.annotations.ContentId;

import java.net.URI;

public class URIBasedContentEntity {

	@ContentId 
	private URI contentId;

	public URI getContentId() {
		return contentId;
	}

	public void setContentId(URI contentId) {
		this.contentId = contentId;
	}
}
