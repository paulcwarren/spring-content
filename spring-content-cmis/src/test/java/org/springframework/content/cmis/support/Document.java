package org.springframework.content.cmis.support;

import java.util.UUID;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.content.cmis.CmisDocument;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

@Entity
@NoArgsConstructor
@Getter
@Setter
@CmisDocument
public class Document extends BaseObject {

	@ContentId
	private UUID contentId;

	@ContentLength
	private Long contentLen;

	@MimeType
	private String mimeType;

	@LockOwner
	private String lockOwner;

	@AncestorId
	private Long ancestorId;

	@AncestorRootId
	private Long ancestralRootId;

	@SuccessorId
	private Long successorId;

	@VersionNumber
	private String versionNumber = "0.0";

	@VersionLabel
	private String versionLabel;

	public Document(String name) {
		super(name);
	}

	public Document(Document doc) {
		this.setName(doc.getName());
		this.setDescription(doc.getDescription());
		this.setParent(doc.getParent());
	}
}
