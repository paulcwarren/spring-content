package it.typesupport.model;

import org.springframework.content.commons.annotations.ContentId;

import java.math.BigInteger;

public class BigIntegerBasedContentEntity {

	@ContentId 
	private BigInteger contentId;

	public BigInteger getContentId() {
		return contentId;
	}

	public void setContentId(BigInteger contentId) {
		this.contentId = contentId;
	}
}
