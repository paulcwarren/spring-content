package internal.org.springframework.content.rest.support;

import java.util.Date;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestEntity8 {

    @Id
    @GeneratedValue
    private Long id;

    private @Version Long version;
    private @CreatedDate Date createdDate;
    private @LastModifiedDate Date modifiedDate;

    private @Embedded TestEntity8Child child = new TestEntity8Child();
}
