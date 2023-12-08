package internal.org.springframework.versions.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;

import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

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
                Context("given selecting a lock record fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class))).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException.class", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                        assertThat(e.getMessage(), is("connection-error"));
                    });
                });
                Context("given inserting the lock record fails", () -> {
                    BeforeEach(() -> {
                        ResultSet rs = mock(ResultSet.class);
                        when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class))).thenReturn(0);
                        when(jdbcTemplate.update(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException.class", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                        assertThat(e.getMessage(), is("connection-error"));
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
                Context("given the lock record deletion fails", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.update(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
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
                        when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                    });
                });
                Context("given the principal is the lock owner", () -> {
                    BeforeEach(() -> {
                        SqlRowSet rs = mock(SqlRowSet.class);
                        when(rs.next()).thenReturn(true);
                        when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenReturn(rs);
                    });
                    It("should return true", () -> {
                        assertThat(result, is(true));
                    });
                });
                Context("given the principal is not the lock owner", () -> {
                    BeforeEach(() -> {
                        SqlRowSet rs = mock(SqlRowSet.class);
                        when(rs.next()).thenReturn(false);
                        when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenReturn(rs);
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
                        when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                    });
                    It("should throw the DataAccessException", () -> {
                        assertThat(e, is(instanceOf(DataAccessException.class)));
                    });
                });
                Context("given there is no lock record", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(null);
                    });
                    It("should return null", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
                Context("given there is a lock record", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(Collections.singletonList("some-principal"));
                    });
                    It("should return a principal", () -> {
                        assertThat(result, is(instanceOf(Principal.class)));
                        assertThat(((Principal)result).getName(), is("some-principal"));
                    });
                });
                Context("given there are mulitple lock records", () -> {
                    BeforeEach(() -> {
                        when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(Arrays.asList(new String[]{("some-principal"), "some-other-principal"}));
                    });
                    It("should throw an IncorrectResultSize exception", () -> {
                        assertThat(e, is(instanceOf(IncorrectResultSizeDataAccessException.class)));
                    });
                });
            });
        });
    }
}
