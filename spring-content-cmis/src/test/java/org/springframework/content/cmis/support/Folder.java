package org.springframework.content.cmis.support;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.CmisProperty;
import org.springframework.content.cmis.CmisPropertyType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@Getter
@Setter
@CmisFolder
public class Folder extends BaseObject {

	@CmisProperty(name="children", type= CmisPropertyType.Child_Relationship)
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.ALL)
	private Collection<BaseObject> children;

	public Folder(String name) {
		super(name);
	}
}
