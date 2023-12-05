package internal.org.springframework.versions.jpa;

import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

public class JpaVersioningServiceImpl implements VersioningService {

    private EntityManager em;

    public JpaVersioningServiceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object establishAncestralRoot(Object entity) {
        Object id = getId(entity);

        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorId.class)) {
            BeanUtils.setFieldWithAnnotation(entity, AncestorId.class, null);
        }

        if (BeanUtils.hasFieldWithAnnotation(entity, AncestorRootId.class) && BeanUtils.getFieldWithAnnotation(entity, AncestorRootId.class) == null) {
            BeanUtils.setFieldWithAnnotation(entity, AncestorRootId.class, id);
        }

        return entity;
    }

    @Override
    public Object establishAncestor(Object entity, Object successor) {

        if (BeanUtils.hasFieldWithAnnotation(entity, SuccessorId.class)) {
            Object successorId = getId(successor);
            BeanUtils.setFieldWithAnnotation(entity, SuccessorId.class, successorId);
        }

        return entity;
    }

    @Override
    public Object establishSuccessor(Object candidate, String versionNo, String versionLabel, Object ancestralRoot, Object ancestor) {
        BeanUtils.setFieldWithAnnotation(candidate, VersionNumber.class, versionNo);
        BeanUtils.setFieldWithAnnotation(candidate, VersionLabel.class, versionLabel);

        if (BeanUtils.hasFieldWithAnnotation(candidate, AncestorRootId.class)) {
            Object ancestralRootId = getId(ancestralRoot);
            BeanUtils.setFieldWithAnnotation(candidate, AncestorRootId.class, ancestralRootId);
        }

        if (BeanUtils.hasFieldWithAnnotation(candidate, AncestorId.class) && ancestor != null) {
            Object ancestorId = getId(ancestor);
            BeanUtils.setFieldWithAnnotation(candidate, AncestorId.class, ancestorId);
        }

        return candidate;
    }

    protected Object getId(Object entity) {
        Object id = BeanUtils.getFieldWithAnnotation(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            return null;
        }
        return id;
    }
}
