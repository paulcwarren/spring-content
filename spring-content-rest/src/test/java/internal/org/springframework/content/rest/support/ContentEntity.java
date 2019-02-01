package internal.org.springframework.content.rest.support;

import java.util.UUID;

public interface ContentEntity {

	Long getId();
	void setId(Long id);

	UUID getContentId();
	void setContentId(UUID id);

	Long getLen();
	void setLen(Long len);

	String getMimeType();
	void setMimeType(String mimeType);

	String getOriginalFileName();
	void setOriginalFileName(String originalFileName);
}
