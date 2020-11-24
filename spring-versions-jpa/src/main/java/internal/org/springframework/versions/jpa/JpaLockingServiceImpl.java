package internal.org.springframework.versions.jpa;

import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import internal.org.springframework.versions.LockingService;

@Service
public class JpaLockingServiceImpl implements LockingService {

    private static Log logger = LogFactory.getLog(JpaLockingServiceImpl.class);

    private JdbcTemplate template;

    @Autowired
    public JpaLockingServiceImpl(JdbcTemplate template){
        this.template = template;
    }

    @Override
    public boolean lock(Object entityId, Principal principal) {
        if (principal == null) {
            throw new SecurityException("no principal");
        }

        String sql = "SELECT count(lock_owner) from locks where entity_id = ?  and lock_owner = ?";
        Integer rc = template.queryForObject(sql, new String[] {entityId.toString(), principal.getName()}, Integer.class);

        if (rc == 1) {
            return true;
        }

        try {
            sql = "INSERT INTO locks (entity_id, lock_owner) VALUES (?,?)";
            rc = template.update(sql, entityId.toString(), principal.getName());
            return (rc == 1);
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean unlock(Object entityId, Principal principal) {
        if (principal == null) {
            throw new SecurityException("no principal");
        }

        String sql = "DELETE from locks where entity_id = ? and lock_owner = ?";
        int rc = template.update(sql, entityId.toString(), principal.getName());
        return (rc == 1);
    }

    @Override
    public Principal lockOwner(Object entityId) {
        String sql = "SELECT lock_owner from locks where entity_id = '" + entityId + "'";
        List<String> lockOwners = template.query(sql, new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }

        });

        if (lockOwners.isEmpty()) {
            return null;
        } else if (lockOwners.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1);
        } else {
            final String name = lockOwners.get(0);
            return new Principal() {

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public boolean implies(Subject subject) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Override
    public boolean isLockOwner(Object entityId, Principal principal) {
        if (principal == null) {
            throw new SecurityException("no principal");
        }

        String sql = "SELECT entity_id from locks where entity_id = ? and lock_owner = ?";
        SqlRowSet rs = template.queryForRowSet(sql, entityId.toString(), principal.getName());
        return rs.next();
    }
}
