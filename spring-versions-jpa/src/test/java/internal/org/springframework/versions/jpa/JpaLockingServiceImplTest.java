package internal.org.springframework.versions.jpa;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jRunner.class)
public class JpaLockingServiceImplTest {

    private JpaLockingServiceImpl locker;

    // mocks
    private JdbcTemplate jdbcTemplate;

    private Object entityId;
    private Principal principal;

    private Object result;
    private Exception e;

    {
        Describe("JpaLockingServiceImpl", () -> {
            BeforeEach(() -> {
                jdbcTemplate = mock(JdbcTemplate.class);
            });
            JustBeforeEach(() -> {
                locker = new JpaLockingServiceImpl(jdbcTemplate);
            });
            Context("#lock", () -> {
                BeforeEach(() -> {
                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");
                });
                JustBeforeEach(() -> {
                    try {
                        result = locker.lock(entityId, principal);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a null principal", () -> {
                    BeforeEach(() -> {
                        principal = null;
                    });
                    It("should throw a SecurityException", () -> {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                    });
                });
                Context("given creating a lock record fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update(anyObject(), anyObject(), anyObject(), anyObject())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException.class", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                        assertThat(e.getMessage(), is("connection-error"));
                    });
                });
                Context("given a lock record is created", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(1);
                    });
                    It("should return true", () -> {
                        verify(jdbcTemplate).update(argThat(startsWith("INSERT INTO versions")),
                                argThat(is("some-id")),
                                argThat(is("some-principal")),
                                argThat(is("some-id")));
                        assertThat(result, is(true));
                    });
                });
                Context("given a lock record is not created", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(0);
                    });
                    It("should return false", () -> {
                        assertThat(result, is(false));
                    });
                });
            });
            Context("#unlock", () -> {
                BeforeEach(() -> {
                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");
                });
                JustBeforeEach(() -> {
                    try {
                        result = locker.unlock(entityId, principal);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a null principal", () -> {
                    BeforeEach(() -> {
                        principal = null;
                    });
                    It("should throw a SecurityException", () -> {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                    });
                });
                Context("given the lock record can be removed", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update((String)anyObject(), (Object[])anyVararg())).thenReturn(1);
                    });
                    It("should return true", () -> {
                        verify(jdbcTemplate).update(argThat(startsWith("DELETE from versions")),
                                                    argThat(is("some-id")),
                                                    argThat(is("some-principal")));
                        assertThat(result, is(true));
                    });
                });
                Context("given the lock record is not removed", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update((String)anyObject(), (Object[])anyVararg())).thenReturn(0);
                    });
                    It("should return true", () -> {
                        assertThat(result, is(false));
                    });
                });
                Context("given the lock record deletion fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update((String)anyObject(), (Object[])anyVararg())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw a DataAccessException", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                        assertThat(e.getMessage(), is("connection-error"));
                    });
                });
            });
            Context("#isLockOwner", () -> {
                BeforeEach(() -> {
                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");
                });
                JustBeforeEach(() -> {
                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given a null principal", () -> {
                    BeforeEach(() -> {
                        principal = null;
                    });
                    It("should throw a SecurityException", () -> {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                    });
                });
                Context("given the database fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.queryForObject(anyString(), (Class)anyObject(), anyVararg())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                    });
                });
                Context("given the principal is the lock owner", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.queryForObject(anyString(), (Class)anyObject(), anyVararg())).thenReturn(1);
                    });
                    It("should return true", () -> {
                        assertThat(result, is(true));
                    });
                });
                Context("given the principal is not the lock owner", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.queryForObject(anyString(), (Class)anyObject(), anyVararg())).thenReturn(0);
                    });
                    It("should return false", () -> {
                        assertThat(result, is(false));
                    });
                });
            });
            Context("#lockOwner", () -> {
                BeforeEach(() -> {
                    entityId = "some-id";
                });
                JustBeforeEach(() -> {
                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given the database fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)anyObject())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                    });
                });
                Context("given there is no lock record", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)anyObject())).thenReturn(null);
                    });
                    It("should return null", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
                Context("given there is a lock record", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)anyObject())).thenReturn(Collections.singletonList("some-principal"));
                    });
                    It("should return a principal", () -> {
                        assertThat(result, is(instanceOf(Principal.class)));
                        assertThat(((Principal)result).getName(), is("some-principal"));
                    });
                });
                Context("given there are mulitple lock records", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)anyObject())).thenReturn(Arrays.asList(new String[]{("some-principal"), "some-other-principal"}));
                    });
                    It("should throw an IncorrectResultSize exception", () -> {
                        assertThat(e, is(instanceOf(IncorrectResultSizeDataAccessException.class)));
                    });
                });
            });
        });
    }
}
