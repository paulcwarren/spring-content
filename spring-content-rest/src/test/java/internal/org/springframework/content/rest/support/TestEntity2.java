package internal.org.springframework.content.rest.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TestEntity2 {
	private @Id @GeneratedValue Long id;

	private @Version Long version;
	private @CreatedDate Date createdDate;
	private @LastModifiedDate Date modifiedDate;

	private @Embedded TestEntityChild child = new TestEntityChild();
	private @RestResource @ElementCollection(fetch = FetchType.EAGER) List<TestEntityChild> children = new ArrayList<>();
}
