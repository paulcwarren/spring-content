package internal.org.springframework.content.jpa.testsupport.models;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import javax.persistence.Embeddable;

@Embeddable
public class ClaimForm {

	@ContentId
	private String contentId;
	
	@ContentLength
	private long contentLength = 0L;
	
	@MimeType
	private String mimeType = "text/plain";
	
	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	// Ensure we can handle entities with "computed" getters; i.e. getters that 
	// dont have an associated field
	public boolean getIsActive() {
		return true;
	}
}
