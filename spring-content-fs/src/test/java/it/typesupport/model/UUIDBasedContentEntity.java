package it.typesupport.model;

import org.springframework.content.commons.annotations.ContentId;

import java.util.UUID;

public class UUIDBasedContentEntity {

	@ContentId 
	private UUID contentId;

	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(UUID contentId) {
		this.contentId = contentId;
	}
}
