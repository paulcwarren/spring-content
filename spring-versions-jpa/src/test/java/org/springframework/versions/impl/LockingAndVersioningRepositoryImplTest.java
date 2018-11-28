package org.springframework.versions.impl;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.JpaVersioningServiceImpl;
import internal.org.springframework.versions.jpa.VersioningService;
import lombok.Getter;
import lombok.Setter;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.security.core.Authentication;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Version;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class LockingAndVersioningRepositoryImplTest {

    private LockingAndVersioningRepositoryImpl repo;

    //mocks
    private EntityManager em;
    private EntityInformationFacade entityInfo;
    private AuthenticationFacade auth;
    private LockingService locker;
    private VersioningService versioner;
    private CloningService cloner;
    private Authentication principal, lockOwner;
    private EntityInformation ei;

    private TestEntity entity, currentEntity, ancestorRoot, ancestor, nextVersion;
    private VersionInfo vi;

    private Object result;
    private Exception e;

    {
        Describe("LockingAndVersioningRepositoryImpl", () -> {
            BeforeEach(() -> {
                em = mock(EntityManager.class);
                entityInfo = mock(EntityInformationFacade.class);
                auth = mock(AuthenticationFacade.class);
                locker = mock(LockingService.class);
                versioner = new JpaVersioningServiceImpl(em);
                cloner = mock(CloningService.class);
            });
            JustBeforeEach(() -> {
                repo = new LockingAndVersioningRepositoryImpl(em, entityInfo, auth, locker, versioner, cloner);
            });
            Context("#lock", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                });
                JustBeforeEach(() -> {
                    try {
                        result = repo.lock(entity);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a principal", () -> {
                    BeforeEach(() -> {
                        principal = mock(Authentication.class);
                        when(principal.getName()).thenReturn("some-principal");
                        when(principal.isAuthenticated()).thenReturn(true);
                        when(auth.getAuthentication()).thenReturn(principal);

                        // save
                        ei = mock(EntityInformation.class);
                        when(entityInfo.getEntityInformation(entity.getClass(), em)).thenReturn(ei);
                        when(em.merge(entity)).thenReturn(entity);
                    });
                    Context("given the lock is obtained", () -> {
                        BeforeEach(() -> {
                            when(locker.lock(0L, principal)).thenReturn(true);
                            when(locker.lockOwner(0L)).thenReturn(principal);
                        });
                        It("should update the entity's @LockOwner field", () -> {
                            assertThat(entity.getLockOwner(), is("some-principal"));
                        });
                        It("should save the entity", () -> {
                            verify(em).merge(entity);
                            assertThat(result, is(entity));
                        });
                    });
                    Context("given the lock cant be obtained", () -> {
                        BeforeEach(() -> {
                            when(locker.lock(0L, principal)).thenReturn(false);
                        });
                        It("should return null", () -> {
                            assertThat(result, is(nullValue()));
                        });
                    });
                    Context("that is not authenticated", () -> {
                        BeforeEach(() -> {
                            when(principal.isAuthenticated()).thenReturn(false);
                        });
                        It("should return SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });
                });
            });
            Context("#unlock", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                });
                JustBeforeEach(() ->{
                    try {
                        result = repo.unlock(entity);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a principal", () -> {
                    BeforeEach(() -> {
                        principal = mock(Authentication.class);
                        when(principal.isAuthenticated()).thenReturn(true);
                        when(principal.getName()).thenReturn("some-principal");
                        when(auth.getAuthentication()).thenReturn(principal);

                        // save
                        ei = mock(EntityInformation.class);
                        when(entityInfo.getEntityInformation(entity.getClass(), em)).thenReturn(ei);
                        when(em.merge(entity)).thenReturn(entity);
                    });
                    Context("given the principal is the lock owner", () -> {
                        BeforeEach(() -> {
                            when(locker.isLockOwner(0L, principal)).thenReturn(true);

                            //unlock
                            when(locker.unlock(0L, principal)).thenReturn(true);
                        });
                        It("should null the @LockOwner field and save", () -> {
                            assertThat(entity.getLockOwner(), is(nullValue()));
                            verify(em).merge(entity);
                        });
                        It("should unlock the entity and return it", () -> {
                            verify(locker).unlock(0L, principal);
                            assertThat(result, is(entity));
                        });
                    });
                    Context("given the principal is not authenticated", () -> {
                        BeforeEach(() -> {
                            principal = mock(Authentication.class);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(principal.isAuthenticated()).thenReturn(false);
                        });
                        It("should a SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });
                });
            });
            Context("#save", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                });
                JustBeforeEach(() -> {
                    try {
                        result = repo.save(entity);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a principal", () -> {
                    BeforeEach(() -> {
                        principal = mock(Authentication.class);
                        when(auth.getAuthentication()).thenReturn(principal);
                    });
                    Context("given the entity is new", () -> {
                        BeforeEach(() -> {
                            ei = mock(EntityInformation.class);
                            when(entityInfo.getEntityInformation(entity.getClass(), em)).thenReturn(ei);
                            when(ei.isNew(entity)).thenReturn(true);
                        });
                        It("should persist and return the entity", () -> {
                            verify(em).persist(entity);
                            assertThat(result, is(entity));
                        });
                    });
                    Context("given the entity already existed", () -> {
                        BeforeEach(() -> {
                            ei = mock(EntityInformation.class);
                            when(entityInfo.getEntityInformation(entity.getClass(), em)).thenReturn(ei);
                        });
                        Context("given the principal is the lock owner", () -> {
                            BeforeEach(() -> {
                                when(principal.isAuthenticated()).thenReturn(true);
                                when(locker.lockOwner(0L)).thenReturn(principal);
                                when(principal.getName()).thenReturn("some-principal");
                                when(em.merge(entity)).thenReturn(entity);
                            });
                            It("should merge and return the entity", () -> {
                                verify(em).merge(entity);
                                assertThat(result, is(entity));
                            });
                        });
                        Context("given the principal is not the lock owner", () -> {
                            BeforeEach(() -> {
                                when(principal.isAuthenticated()).thenReturn(true);
                                lockOwner = mock(Authentication.class);
                                when(locker.lockOwner(0L)).thenReturn(lockOwner);
                                when(principal.getName()).thenReturn("some-principal");
                                when(lockOwner.getName()).thenReturn("some-other-principal");
                                when(em.merge(entity)).thenReturn(entity);
                            });
                            It("should throw a LockOwnerException", () -> {
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                            });
                        });
                        Context("given the principal is not authenticated", () -> {
                            BeforeEach(() -> {
                                when(principal.isAuthenticated()).thenReturn(false);
                            });
                            It("should throw a SecurityException", () -> {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                            });
                        });
                    });
                });
            });
            Context("#version", () ->   {
                BeforeEach(() -> {
                    currentEntity = new TestEntity();
                    vi = new VersionInfo("1.1", "a version label");
                });
                JustBeforeEach(() -> {
                    try {
                        result = repo.version(currentEntity, vi);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a principal", () -> {
                    BeforeEach(() -> {
                        principal = mock(Authentication.class);
                        when(principal.isAuthenticated()).thenReturn(true);
                        when(auth.getAuthentication()).thenReturn(principal);
                    });
                    Context("when the entity is not yet part of a version tree", () -> {
                        BeforeEach(() -> {
                            when(entityInfo.getEntityInformation(TestEntity.class, em)).thenReturn(mock(EntityInformation.class));
                            when(locker.lock(1L, principal)).thenReturn(true);
                            when(locker.lockOwner(1L)).thenReturn(principal);
                            when(locker.isLockOwner(1L, principal)).thenReturn(true);
                        });
                        Context("given the principal is the lock owner", () -> {
                            BeforeEach(() -> {
                                when(principal.getName()).thenReturn("some-principal");
                                when(locker.lockOwner(currentEntity.getId())).thenReturn(principal);
                                when(locker.isLockOwner(currentEntity.getId(), principal)).thenReturn(true);
                            });
                            Context("given the entity manager finds the ancestor root", () -> {
                                BeforeEach(() -> {
                                    when(em.find(TestEntity.class, 0L)).thenReturn(currentEntity);
                                });
                                Context("given the cloner can clone the entity to make a new version", () -> {
                                    BeforeEach(() -> {
                                        nextVersion = new TestEntity();
                                        when(cloner.clone(currentEntity)).thenReturn(nextVersion);
                                    });
                                    Context("given persisting the new version assigns a new ID", () -> {
                                        BeforeEach(() -> {
                                            doAnswer(new Answer() {
                                                public Object answer(InvocationOnMock invocation) {
                                                    nextVersion.setId(1L);
                                                    return null;
                                                }
                                            }).when(em).persist(nextVersion);
                                            when(em.merge(nextVersion)).thenReturn(nextVersion);
                                        });
                                        It("should update the version attributes on the current entity and unlock the entity", () -> {
                                            assertThat(currentEntity.getAncestorId(), is(nullValue()));
                                            assertThat(currentEntity.getAncestorRootId(), is(currentEntity.getId()));
                                            assertThat(currentEntity.getSuccessorId(), is(1L));
                                            verify(locker).unlock(currentEntity.getId(), principal);
                                        });
                                        It("should establish the new version, persist and lock it", () -> {
                                            assertThat(((TestEntity) result).getAncestorRootId(), is(0L));
                                            assertThat(((TestEntity) result).getAncestorId(), is(0L));
                                            assertThat(((TestEntity) result).getSuccessorId(), is(nullValue()));
                                            assertThat(((TestEntity) result).getVersionNumber(), is("1.1"));
                                            assertThat(((TestEntity) result).getVersionLabel(), is("a version label"));
                                            verify(locker).lock(((TestEntity) result).getId(), principal);
                                        });
                                    });
                                });
                            });
                        });
                    });
                    Context("when the entity is part of a version tree", () -> {
                        BeforeEach(() -> {
                            ancestorRoot = new TestEntity();
                            ancestorRoot.setId(0L);
                            ancestorRoot.setAncestorRootId(0L);
                            ancestorRoot.setSuccessorId(1L);
                            currentEntity = new TestEntity();
                            currentEntity.setId(1L);
                            currentEntity.setAncestorRootId(0L);
                            currentEntity.setAncestorId(0L);
                            vi = new VersionInfo("1.2", "another version label");
                        });
                        Context("given the principal is the lock owner", () -> {
                            BeforeEach(() -> {
                                when(principal.getName()).thenReturn("some-principal");
                                when(locker.lockOwner(currentEntity.getId())).thenReturn(principal);
                                when(locker.isLockOwner(currentEntity.getId(), principal)).thenReturn(true);
                            });
                            Context("given the entity manager finds the entity", () -> {
                                BeforeEach(() -> {
                                    when(entityInfo.getEntityInformation(TestEntity.class, em)).thenReturn(mock(EntityInformation.class));
                                    when(em.find(TestEntity.class, 0L)).thenReturn(ancestorRoot);
                                    when(em.find(TestEntity.class, 1L)).thenReturn(currentEntity);
                                });
                                Context("given the cloner can clone the entity to make a new version", () -> {
                                    BeforeEach(() -> {
                                        nextVersion = new TestEntity();
                                        when(cloner.clone(currentEntity)).thenReturn(nextVersion);
                                    });
                                    Context("given persisting the new version assigns a new ID", () -> {
                                        BeforeEach(() -> {
                                            doAnswer(new Answer() {
                                                public Object answer(InvocationOnMock invocation) {
                                                    nextVersion.setId(2L);
                                                    return null;
                                                }
                                            }).when(em).persist(nextVersion);
                                            when(em.merge(nextVersion)).thenReturn(nextVersion);
                                        });
                                        It("should update the version attributes on the current entity, unlock and flush the entity to the db", () -> {
                                            assertThat(currentEntity.getAncestorId(), is(0L));
                                            assertThat(currentEntity.getAncestorRootId(), is(0L));
                                            assertThat(currentEntity.getSuccessorId(), is(2L));
                                            assertThat(currentEntity.getLockOwner(), is(nullValue()));
                                            verify(locker).unlock(1L, principal);
                                        });
                                        It("should establish the new version, persist and lock it", () -> {
                                            assertThat(((TestEntity)result).getAncestorRootId(), is(0L));
                                            assertThat(((TestEntity)result).getAncestorId(), is(1L));
                                            assertThat(((TestEntity)result).getSuccessorId(), is(nullValue()));
                                            assertThat(((TestEntity)result).getVersionNumber(), is("1.2"));
                                            assertThat(((TestEntity)result).getVersionLabel(), is("another version label"));
                                            verify(locker).lock(2L, principal);
                                        });
                                    });
                                });
                            });
                            Context("given the entity manager cant find the ancestor", () -> {
                                It("should throw an IllegalStateException", () -> {
                                    assertThat(result, is(nullValue()));
                                    assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                                    assertThat(e.getMessage(), containsString("ancestor root not found"));
                                });
                            });
                        });
                        Context("given the entity is not the head", () -> {
                            BeforeEach(() -> {
                                currentEntity.setSuccessorId(999L);
                            });
                            It("should fail", () -> {
                                assertThat(result, is(nullValue()));
                                assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                                assertThat(e.getMessage(), containsString("not head"));
                            });
                        });
                    });
                    Context("given the principal is not the lock owner", () -> {
                        BeforeEach(() -> {
                            when(principal.getName()).thenReturn("some-principal");
                            when(locker.lockOwner(0L)).thenReturn(null);
                        });
                        It("should throw a LockOwnerException", () -> {
                            assertThat(e, is(instanceOf(LockOwnerException.class)));
                        });
                    });
                });
                Context("given there is no lock owner", () -> {
                    BeforeEach(() -> {
                        principal = mock(Authentication.class);
                        when(auth.getAuthentication()).thenReturn(principal);
                        when(principal.isAuthenticated()).thenReturn(false);
                    });
                    It("should throw a InsufficientAuthenticationException", () -> {
                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                    });
                });
            });
            Context("#delete", () -> {
                JustBeforeEach(() -> {
                    try {
                        repo.delete(entity);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given the entity is not in a version tree", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                    });
                    Context("given a principal", () -> {
                        BeforeEach(() -> {
                            principal = mock(Authentication.class);
                            when(principal.isAuthenticated()).thenReturn(true);
                            when(auth.getAuthentication()).thenReturn(principal);
                        });
                        Context("given a lock is held by the principal", () -> {
                            BeforeEach(() -> {
                                when(principal.getName()).thenReturn("some-principal");
                                when(locker.lockOwner(entity.getId())).thenReturn(principal);
                                when(locker.isLockOwner(entity.getId(), principal)).thenReturn(true);
                            });
                            It("should be deleted", () -> {
                                verify(em).remove(entity);
                            });
                        });
                        Context("given a lock is held by another principal", () -> {
                            BeforeEach(() -> {
                                when(principal.getName()).thenReturn("some-principal");
                                when(locker.lockOwner(0L)).thenReturn(mock(Authentication.class));
                                when(locker.isLockOwner(entity.getId(), principal)).thenReturn(false);
                            });
                            It("should fail to delete the entity", () -> {
                                verify(em, never()).remove(entity);
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                            });
                        });
                        Context("given there is no lock", () -> {
                            BeforeEach(() -> {
                                when(locker.lockOwner(entity.getId())).thenReturn(null);
                            });
                            It("should be deleted", () -> {
                                verify(em).remove(entity);
                            });
                        });
                    });
                });
                Context("given the entity is not the head", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                        entity.setSuccessorId(999L);
                    });
                    It("should fail", () -> {
                        assertThat(result, is(nullValue()));
                        assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                        assertThat(e.getMessage(), containsString("not head"));
                    });
                });
                Context("given the entity is the head of a version tree of 3 versions (ancestor is not ancestral root)", () -> {
                    BeforeEach(() -> {
                        ancestorRoot = new TestEntity();
                        ancestorRoot.setId(0L);
                        ancestorRoot.setAncestorRootId(0L);
                        ancestorRoot.setSuccessorId(1L);
                        ancestor = new TestEntity();
                        ancestor.setId(1L);
                        ancestor.setAncestorRootId(0L);
                        ancestor.setAncestorId(0L);
                        ancestor.setSuccessorId(2L);
                        entity = new TestEntity();
                        entity.setId(2L);
                        entity.setAncestorRootId(0L);
                        entity.setAncestorId(1L);

                        when(em.find(TestEntity.class, 1L)).thenReturn(ancestor);
                    });
                    It("should delete the entity", () -> {
                        verify(em).remove(entity);
                    });
                    It("should re-instate the ancestor as the head", () -> {
                        assertThat(ancestor.getSuccessorId(), is(nullValue()));
                    });
                    Context("given a lock is held by the principal", () -> {
                        BeforeEach(() -> {
                            principal = mock(Authentication.class);
                            when(principal.isAuthenticated()).thenReturn(true);
                            when(auth.getAuthentication()).thenReturn(principal);
                            when(principal.getName()).thenReturn("some-principal");
                            when(locker.lockOwner(entity.getId())).thenReturn(principal);
                            when(locker.isLockOwner(entity.getId(), principal)).thenReturn(true);
                        });
                        It("should remove the lock", () -> {
                            verify(locker).unlock(2L, principal);
                        });
                        It("should re-instate the lock on the new head", () -> {
                            verify(locker).lock(1L, principal);
                        });
                    });
                });
                Context("given the entity is the head of a version tree of 2 versions (ancestor is ancestral root)", () -> {
                    BeforeEach(() -> {
                        ancestorRoot = new TestEntity();
                        ancestorRoot.setId(0L);
                        ancestorRoot.setAncestorRootId(0L);
                        ancestorRoot.setSuccessorId(1L);
                        entity = new TestEntity();
                        entity.setId(1L);
                        entity.setAncestorRootId(0L);
                        entity.setAncestorId(null);

                        when(em.find(TestEntity.class, 0L)).thenReturn(ancestorRoot);
                    });
                    It("should delete the entity", () -> {
                        verify(em).remove(entity);
                    });
                    It("should re-instate the ancestor as the head", () -> {
                        assertThat(ancestorRoot.getSuccessorId(), is(nullValue()));
                    });
                });
            });
        });
    }

    @Getter
    @Setter
    private class TestEntity {
        @Id private Long id = 0L;
        @Version private Long vstamp;
        @LockOwner String lockOwner;
        @AncestorRootId Long ancestorRootId;
        @AncestorId Long ancestorId;
        @SuccessorId Long successorId;
        @VersionNumber private String versionNumber;
        @VersionLabel private String versionLabel;
    }
}
