package org.springframework.support;

import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TestEntityVersioned {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private Long id;

    // these are required to satisfy the
    @AncestorRootId private String ancestralRootId;
    @SuccessorId private String successorId;
}
