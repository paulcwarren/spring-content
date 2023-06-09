package org.springframework.versions.impl;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.versions.LockingService;
import lombok.Getter;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes={JpaLockingAndVersioningRepositoryImplIT.TestConfig.class})
public class JpaLockingAndVersioningRepositoryImplIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LockingService lockingService;

    private TestRepository repo;

    private TestEntity e1, e2, e3, e1v11, e1v12, e2v2, e3wc, entityForDeletion, result;

    private Exception e;

    //////////////////////////////////////////////////
    // this entity and repository ensure that multiple repositories can implement LockingAndVersioningRepository at the same time
    @Autowired
    private OtherTestRepository otherRepo;

    {
        Describe("given a locking and versioning repository and a security context", () -> {

            BeforeEach(() -> {
                repo = context.getBean(TestRepository.class);
            });

            Context("given two entities with two versions each", () -> {

                BeforeEach(() -> {
                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());
                });

                Context("#lock", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);
                    });

                    JustBeforeEach(() -> {
                        try {
                            result = repo.lock(e1);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });

                    Context("given a null principal", () -> {
                        BeforeEach(() -> {
                            setupSecurityContext(null, false);
                        });
                        It("should throw a SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });

                    Context("given the entity is new", () -> {

                        It("should fail", () -> {
                            assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                        });
                    });

                    Context("given the entity exists", () -> {

                        BeforeEach(() -> {
                            e1 = repo.save(e1);
                        });

                        Context("when the object is not locked", () -> {

                            It("should update the entity's @LockOwner field", () -> {
                                assertThat(e1.getXLockOwner(), is("some-principal"));
                            });

                            It("should save the entity", () -> {
                                assertThat(e, is(nullValue()));
                                assertThat(result.getXid(), is(e1.getXid()));
                            });
                        });

                        Context("when the lock is already taken", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-other-principal", true);
                                e1 = repo.lock(e1);
                                setupSecurityContext("some-principal", true);
                            });

                            It("should return null", () -> {
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                                assertThat(result, is(nullValue()));
                            });
                        });

                        Context("when the lock is already held", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);
                                setupSecurityContext("some-principal", true);
                            });

                            It("should succeed", () -> {
                                assertThat(e, is(nullValue()));
                                assertThat(result, is(not(nullValue())));
                            });
                        });

                        Context("when the principal is not authenticated", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", false);
                            });

                            It("should return SecurityException", () -> {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                            });
                        });
                    });
                });

                Context("#unlock", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);
                    });

                    JustBeforeEach(() ->{
                        try {
                            result = repo.unlock(e1);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });

                    Context("given a null principal", () -> {
                        BeforeEach(() -> {
                            setupSecurityContext(null, false);
                        });
                        It("should throw a SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });

                    Context("given the entity is new", () -> {

                        It("should fail", () -> {
                            assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                        });
                    });

                    Context("given the entity exists", () -> {

                        BeforeEach(() -> {
                            e1 = repo.save(e1);
                        });

                        Context("given the principal is the lock owner", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);
                            });

                            It("should null the @LockOwner field and save", () -> {
                                assertThat(result.getXLockOwner(), is(nullValue()));
                            });

                            It("should unlock the entity and return it", () -> {
                                assertThat(e, is(nullValue()));
                                assertThat(result.getXid(), is(e1.getXid()));
                            });
                        });

                        Context("given the principal is not the lock owner", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-other-principal", true);
                                e1 = repo.lock(e1);
                                setupSecurityContext("some-principal", true);
                            });

                            It("should a SecurityException", () -> {
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                                assertThat(e.getMessage(), containsString("not lock owner"));
                            });
                        });

                        Context("given the principal is not authenticated", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", false);
                            });

                            It("should a SecurityException", () -> {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                            });
                        });
                    });
                });

                Context("#save", () -> {

                    BeforeEach(() -> {
                        e3 = new TestEntity();
                    });

                    JustBeforeEach(() -> {
                        try {
                            result = repo.save(e3);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });

                    Context("given an authenticated principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", true);
                        });

                        Context("given the entity is new", () -> {

                            Context("given there is no lock owner", () -> {

                                It("should merge and return the entity", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });
                        });

                        Context("given the entity exists", () -> {

                            BeforeEach(() -> {
                                e3 = repo.save(e3);
                            });

                            Context("given the principal is the lock owner", () -> {

                                BeforeEach(() -> {
                                    e3 = repo.lock(e3);
                                });

                                It("should return the entity", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });

                            Context("given the principal is not the lock owner", () -> {

                                BeforeEach(() -> {
                                    e3 = repo.lock(e3);
                                    setupSecurityContext("some-other-principal", true);
                                });

                                It("should throw a LockOwnerException", () -> {
                                    assertThat(e, is(instanceOf(LockOwnerException.class)));
                                });
                            });

                            Context("given there is no lock owner", () -> {

                                It("should merge and return the entity", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });
                        });
                    });

                    Context("given an unauthenticated principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", false);
                        });

                        Context("given the entity is new", () -> {

                            Context("given there is no lock owner", () -> {

                                It("should merge and return the entity", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });
                        });

                        Context("given the entity exists", () -> {

                            BeforeEach(() -> {
                                e3 = repo.save(e3);
                            });

                            Context("given there is no lock owner", () -> {

                                It("should merge and return the entity", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });
                        });
                    });

                    Context("given there is no principal", () -> {

                        Context("given the entity is new", () -> {

                            Context("given there is no lock owner", () -> {

                                It("should succeed", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(result.getXid(), is(e3.getXid()));
                                });
                            });
                        });
                    });
                });

                Context("#findAllVersions", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        e2 = repo.save(e2);
                        e2 = repo.lock(e2);
                        e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                        e2v2 = repo.unlock(e2v2);
                    });

                    It("should return the version series", () -> {
                        List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                        assertThat(results.size(), is(2));
                        assertThat(results, Matchers.hasItems(hasProperty("xid", is(e1.getXid())), hasProperty("xid", is(e1v11.getXid()))));
                    });

                    It("should return the ordered version series", () -> {
                        List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                        assertThat(results.size(), is(2));
                        assertThat(results, Matchers.hasItems(hasProperty("xid", is(e1.getXid())), hasProperty("xid", is(e1v11.getXid()))));

                        // is ordered
                        {
                            long lastId = 0;
                            for (int i=results.size() - 1; i >= 0; i--) {
                                assertThat(results.get(i).getXid(), is(greaterThan(lastId)));
                                lastId = results.get(i).getXid();
                            }
                        }
                    });
                });

                Context("#findAllLatestVersions", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        e2 = repo.save(e2);
                        e2 = repo.lock(e2);
                        e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                        e2v2 = repo.unlock(e2v2);

                        e2v2 = repo.lock(e2v2);
                        e3wc = repo.workingCopy(e2v2);
                    });

                    It("should return only the latest version of the entities", () -> {
                        List<TestEntity> results = repo.findAllVersionsLatest((Class<TestEntity>) e1.getClass());
                        assertThat(results, Matchers.hasItems(
                                hasProperty("xid", is(e1v11.getXid())),
                                hasProperty("xid", is(e2v2.getXid())),
                                not(hasProperty("xid", is(e3wc.getXid())))
                        ));

                        results = repo.findAllVersionsLatest(TestEntity.class);
                        assertThat(results, Matchers.hasItems(
                                hasProperty("xid", is(e1v11.getXid())),
                                hasProperty("xid", is(e2v2.getXid())),
                                not(hasProperty("xid", is(e3wc.getXid())))
                        ));
                    });
                });

                Context("#workingCopy", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);
                    });

                    JustBeforeEach(() -> {
                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });

                    Context("when the entity is new", () -> {

                        It("should fail", () -> {
                            assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                        });
                    });

                    Context("when the entity exists", () -> {

                        BeforeEach(() -> {
                            e1 = repo.save(e1);
                        });

                        Context("given the principal is the lock owner", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);
                            });

                            Context("when the entity is not yet part of a version tree", () -> {

                                It("should create the pwc with a new id", () -> {
                                    assertThat(result.getXid(), is(not(nullValue())));
                                    assertThat(result.getVersionLabel(), is("~~PWC~~"));
                                    assertThat(result.getXAncestorId(), is(e1.getXid()));
                                    assertThat(result.getXAncestorRootId(), is(e1.getXid()));
                                    assertThat(result.getXSuccessorId(), is(nullValue()));
                                });
                            });
                        });

                        Context("given the principal is not the lock owner", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);
                                setupSecurityContext("some-other-principal", true);
                            });

                            It("should create the pwc with a new id", () -> {
                                assertThat(e, is(instanceOf(LockOwnerException.class)));
                            });
                        });

                        Context("given the principal is unauthenticated", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", false);
                            });

                            It("should throw a SecurityException", () -> {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                                assertThat(e.getMessage(), containsString("no principal"));
                            });
                        });

                        Context("given the entity is not the current version", () -> {

                            BeforeEach(() -> {
                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            });

                            It("should throw an exception", () -> {
                                assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                                assertThat(e.getMessage(), containsString("not head"));
                            });
                        });
                    });
                });

                Context("#isPrivateWorkingCopy", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);
                    });

                    It("should return false", () -> {
                        assertThat(repo.isPrivateWorkingCopy(e1), is(false));
                    });

                    It("should return true", () -> {
                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        TestEntity wc = repo.workingCopy(e1);
                        assertThat(repo.isPrivateWorkingCopy(wc), is(true));
                    });
                });

                Context("#findWorkingCopy", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);
                    });

                    It("should return true", () -> {
                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        TestEntity wc = repo.workingCopy(e1);
                        assertThat(repo.findWorkingCopy(wc), hasProperty("xid", is(wc.getXid())));
                    });
                });

                Context("#delete", () -> {

                    JustBeforeEach(() -> {
                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });

                    Context("given no principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext(null, false);
                            entityForDeletion = e1;
                        });

                        It("should throw a SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });

                    Context("given an unauthenticated principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", false);
                            entityForDeletion = e1;
                        });

                        It("should throw a SecurityException", () -> {
                            assertThat(e, is(instanceOf(SecurityException.class)));
                        });
                    });

                    Context("given a principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", true);
                        });

                        Context("given the entity is not in a version tree", () -> {

                            Context("given the principal is the lock owner", () -> {

                                BeforeEach(() -> {
                                    e1 = repo.save(e1);
                                    e1 = repo.lock(e1);
                                    entityForDeletion = e1;
                                });

                                It("should be deleted", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(repo.findById(e1.getXid()), is(Optional.empty()));
                                });
                            });

                            Context("given the principal is not the lock owner", () -> {

                                BeforeEach(() -> {
                                    e1 = repo.save(e1);
                                    e1 = repo.lock(e1);
                                    entityForDeletion = e1;
                                    setupSecurityContext("some-other-principal", true);
                                });

                                It("should fail to delete the entity", () -> {
                                    assertThat(e, is(instanceOf(LockOwnerException.class)));
                                    assertThat(e.getMessage(), containsString("not lock owner"));
                                });
                            });

                            Context("given there is no lock", () -> {

                                BeforeEach(() -> {
                                    e1 = repo.save(e1);
                                    entityForDeletion = e1;
                                });

                                It("should be deleted", () -> {
                                    assertThat(e, is(nullValue()));
                                    assertThat(repo.findById(e1.getXid()), is(Optional.empty()));
                                });
                            });
                        });

                        Context("given the entity is not the head", () -> {

                            BeforeEach(() -> {
                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                entityForDeletion = e1;
                            });

                            It("should fail", () -> {
                                assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                                assertThat(e.getMessage(), containsString("not head"));
                            });
                        });

                        Context("given the entity is the head of a version series of 3 versions and the ancestor is not ancestral root", () -> {

                            BeforeEach(() -> {
                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                e1v12 = repo.version(e1v11, new VersionInfo("1.2", "Minor"));
                                entityForDeletion = e1v12;
                            });

                            It("should delete the entity", () -> {
                                assertThat(repo.findById(e1v12.getXid()), is(Optional.empty()));
                            });

                            It("should re-instate the ancestor as the head", () -> {
                                e1v11 = repo.findById(e1v11.getXid()).get();
                                assertThat(e1v11.getXSuccessorId(), is(nullValue()));
                            });

                            It("should remove the lock", () -> {
                                assertThat(lockingService.lockOwner(e1v12.getXid()), is(nullValue()));
                            });

                            It("should re-instate the lock on the new head", () -> {
                                assertThat(lockingService.lockOwner(e1v11.getXid()), is(not(nullValue())));
                            });
                        });

                        Context("given the entity is the head of a version tree of 2 versions (ancestor is ancestral root)", () -> {

                            BeforeEach(() -> {
                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                entityForDeletion = e1v11;
                            });

                            It("should delete the entity", () -> {
                                assertThat(e, is(nullValue()));
                                assertThat(repo.findById(e1v11.getXid()), is(Optional.empty()));
                            });

                            It("should re-instate the ancestor as the head", () -> {
                                e1 = repo.findById(e1.getXid()).get();
                                assertThat(e1.getXSuccessorId(), is(nullValue()));
                            });
                        });
                    });
                });

                Context("#deleteAllVersions", () -> {

                    BeforeEach(() -> {
                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);
                    });

                    Context("given no principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext(null, false);
                        });

                        It("should throw a SecurityException", () -> {
                            try {
                                repo.deleteAllVersions(e1v11);
                                fail("expected security exception");
                            } catch (Exception e) {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                            }
                        });
                    });

                    Context("given an unauthenticated principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", false);
                        });

                        It("should throw a SecurityException", () -> {
                            try {
                                repo.deleteAllVersions(e1v11);
                                fail("expected security exception");
                            } catch (Exception e) {
                                assertThat(e, is(instanceOf(SecurityException.class)));
                            }
                        });
                    });

                    Context("given a principal", () -> {

                        BeforeEach(() -> {
                            setupSecurityContext("some-principal", true);
                        });

                        Context("given the entity is not in a version tree", () -> {

                            Context("given the principal is the lock owner", () -> {

                                It("should delete version series", () -> {
                                    e1v11 = repo.lock(e1v11);

                                    List<Long> ids = new ArrayList<>();
                                    repo.findAllVersions(e1v11).forEach((doc) -> {
                                        ids.add(doc.getXid());
                                    });

                                    repo.deleteAllVersions(e1v11);

                                    ids.forEach((id) -> {
                                        assertThat(repo.existsById(id), is(false));
                                    });
                                });
                            });

                            Context("given the principal is not the lock owner", () -> {

                                BeforeEach(() -> {
                                    e1v11 = repo.lock(e1v11);
                                    setupSecurityContext("some-other-principal", true);
                                });

                                It("should fail to delete the entity", () -> {
                                    try {
                                        repo.deleteAllVersions(e1v11);
                                        fail("expected lockownerexception");
                                    } catch (Exception e) {
                                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                                        assertThat(e.getMessage(), containsString("not lock owner"));
                                    }
                                });
                            });

                            Context("given there is no lock", () -> {

                                It("should delete version series", () -> {
                                    List<Long> ids = new ArrayList<>();
                                    repo.findAllVersions(e1).forEach((doc) -> {
                                        ids.add(doc.getXid());
                                    });

                                    repo.deleteAllVersions(e1v11);

                                    ids.forEach((id) -> {
                                        assertThat(repo.existsById(id), is(false));
                                    });
                                });
                            });
                        });

                        Context("given the entity is not the head", () -> {

                            It("should fail", () -> {
                                try {
                                    repo.deleteAllVersions(e1);
                                    fail("expected lockingandversioningexception");
                                } catch (Exception e) {
                                    assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                                    assertThat(e.getMessage(), containsString("not head"));
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories=true)
    @Import({H2Config.class, JpaLockingAndVersioningConfig.class})
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class H2Config {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }

        @Value("/org/springframework/versions/jpa/schema-drop-h2.sql")
        private ClassPathResource dropVersionSchema;

        @Value("/org/springframework/versions/jpa/schema-h2.sql")
        private ClassPathResource createVersionSchema;

        @Bean
        public DataSourceInitializer datasourceInitializer() {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropVersionSchema);
            databasePopulator.addScript(createVersionSchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource());
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }
    }

    @Getter
    @Setter
    @Entity
    public static class TestEntity {
        @Id @GeneratedValue private Long xid;
        @Version private Long version;
        @AncestorId private Long xAncestorId;
        @AncestorRootId private Long xAncestorRootId;
        @SuccessorId private Long xSuccessorId;
        @LockOwner private String xLockOwner;
        @VersionNumber private String versionNo;
        @VersionLabel private String versionLabel;

        public TestEntity() {}
        public TestEntity(TestEntity entity) {}
    }

    public interface TestRepository extends JpaRepository<TestEntity, Long>, LockingAndVersioningRepository<TestEntity, Long> {}

    @Getter
    @Setter
    @Entity
    public static class OtherTestEntity {
        @Id @GeneratedValue private Long id;
    }

    public interface OtherTestRepository extends JpaRepository<OtherTestEntity, Long>, LockingAndVersioningRepository<OtherTestEntity, Long> {}

    private static void setupSecurityContext(String principal, boolean isAuthenticated) {
        SecurityContext sc = new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new MockAuthentication(principal, isAuthenticated);
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        };

        SecurityContextHolder.setContext(sc);
    }

    private static class MockAuthentication implements Authentication {

        private final String principal;
        private final boolean isAuthenticated;

        public MockAuthentication(String principal, boolean isAuthenticated) {
            this.principal = principal;
            this.isAuthenticated = isAuthenticated;
        }

        @Override
        public String getName() {
            return principal;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAuthenticated() {
            return isAuthenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        }
    }

    @Test
    public void noop() {}
}
