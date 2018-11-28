package internal.org.springframework.versions.jpa;

import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.repository.core.EntityInformation;

import javax.persistence.EntityManager;

public class EntityInformationFacade {
    public EntityInformation getEntityInformation(Class<?> entityClass, EntityManager em) {
        return JpaEntityInformationSupport.getEntityInformation(entityClass, em);
    }
}
