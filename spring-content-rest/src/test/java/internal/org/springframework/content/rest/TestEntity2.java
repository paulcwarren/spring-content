package internal.org.springframework.content.rest;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.Content;

@Entity
public class TestEntity2 {
	public @Id @GeneratedValue Long id;
	public @Content @Embedded TestEntityChild child;
	public @Content @ElementCollection(fetch=FetchType.EAGER) List<TestEntityChild> children = new ArrayList<>();
	public List<TestEntityChild> getChildren() {
		return children;
	}
}
