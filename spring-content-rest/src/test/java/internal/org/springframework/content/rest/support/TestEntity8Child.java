package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Embeddable;

import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Embeddable
public class TestEntity8Child {
	@ContentId
	public UUID contentId;
	@ContentLength
	public Long contentLen;
	@MimeType
	public String contentMimeType;
	@OriginalFileName
	public String contentFileName = "";

	// prevent TestEntity8Child from being return by hibernate as null
	@Formula("1")
	private int workaroundForBraindeadJpaImplementation;

	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(UUID contentId) {
		this.contentId = contentId;
	}

	public Long getContentLen() {
		return contentLen;
	}

	public void setContentLen(Long contentLen) {
		this.contentLen = contentLen;
	}

	public String getContentMimeType() {
		return contentMimeType;
	}

	public void setContentMimeType(String mimeType) {
		this.contentMimeType = mimeType;
	}

	public String getContentFileName() {
		return contentFileName;
	}

	public void setContentFileName(String fileName) {
		this.contentFileName = fileName;
	}
}
