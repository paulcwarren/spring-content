package internal.org.springframework.content.rest.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.content.rest.RestResource;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestEntity11 {

    @Id
    @GeneratedValue
    private Long id;

    private @Version Long version;
    private @CreatedDate Date createdDate;
    private @LastModifiedDate Date modifiedDate;

    @RestResource(linkRel="package", path="package")
    private @Embedded TestEntity10Child _package = new TestEntity10Child();
}
