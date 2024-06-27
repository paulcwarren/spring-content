package it.typesupport.model;

import org.springframework.content.commons.annotations.ContentId;

public class LongBasedContentEntity {

	@ContentId 
	private Long contentId;

	public Long getContentId() {
		return contentId;
	}

	public void setContentId(Long contentId) {
		this.contentId = contentId;
	}
}
