package org.springframework.versions.jpa.boot;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;

@Entity
public class TestEntityVersioned {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private Long id;

    // these are required to satisfy the
    @AncestorRootId private String ancestralRootId;
    @AncestorId private String ancestorId;
    @SuccessorId private String successorId;
}
