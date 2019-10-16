package internal.org.springframework.content.rest.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TestEntity2 {
	private @Id @GeneratedValue Long id;

	private @Version Long version;
	private @CreatedDate Date createdDate;
	private @LastModifiedDate Date modifiedDate;

	private @Embedded TestEntityChild child;
	private @RestResource @ElementCollection(fetch = FetchType.EAGER) List<TestEntityChild> children = new ArrayList<>();
}
