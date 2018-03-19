package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Embeddable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Embeddable
public class TestEntityChild {
	@ContentId public UUID contentId;
	@ContentLength public Long contentLen;
	@MimeType public String mimeType;
	@OriginalFileName public String fileName = "";
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
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public String getFileName() { return fileName; }
	public void setFileName(String fileName) { this.fileName = fileName; }
}
