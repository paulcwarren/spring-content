package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.*;

@Entity
@Content
public class TestEntity {
	public @Id @GeneratedValue Long id;
	public String name;
	public @ContentId UUID contentId;
	public @OriginalFileName String originalFileName;
	public @ContentLength Long len;
	public @MimeType String mimeType;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public UUID getContentId() {
		return contentId;
	}
	public void setContentId(UUID contentId) {
		this.contentId = contentId;
	}
	public String getOriginalFileName() {
		return originalFileName;
	}
	public void setOriginalFileName(String name) {
		this.originalFileName = name;
	}
	public Long getLen() {
		return len;
	}
	public void setLen(Long len) {
		this.len = len;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}
