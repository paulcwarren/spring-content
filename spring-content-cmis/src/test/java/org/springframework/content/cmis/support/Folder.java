package org.springframework.content.cmis.support;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.CmisReference;
import org.springframework.content.cmis.CmisReferenceType;

@Entity
@NoArgsConstructor
@Getter
@Setter
@CmisFolder
public class Folder extends BaseObject {

	@CmisReference(type= CmisReferenceType.Child)
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.ALL)
	private Collection<BaseObject> children;

	public Folder(String name) {
		super(name);
	}
}
