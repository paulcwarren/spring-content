package internal.org.springframework.versions.jpa;

import internal.org.springframework.versions.LockingService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.security.auth.Subject;
import java.security.Principal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

        String sql = "INSERT INTO versions (entity_id, lock_owner)" +
                        "  SELECT ?, ?" +
                        "  FROM mutex LEFT JOIN versions" +
                        "  ON entity_id = ?" +
                        "  WHERE i = 0 AND entity_id IS NULL;";
        int rc = template.update(sql, entityId, principal.getName(), entityId);
        return (rc == 1);
    }

    @Override
    public boolean unlock(Object entityId, Principal principal) {
        if (principal == null) {
            throw new SecurityException("no principal");
        }

        String sql = "DELETE from versions where entity_id = ? and lock_owner = ?;";
        int rc = template.update(sql, entityId, principal.getName());
        return (rc == 1);
    }

    @Override
    public boolean isLockOwner(Object entityId, Principal principal) {
        if (principal == null) {
            throw new SecurityException("no principal");
        }

        int count = 0;
        String sql = "SELECT count(id) from versions where entity_id = ? and lock_owner = ?;";
        count = template.queryForObject(sql, Integer.class, entityId, principal.getName());
        return count == 1;
    }

    @Override
    public Principal lockOwner(Object entityId) {
        String sql = "SELECT lock_owner from versions where entity_id = " + entityId;
        List<String> lockOwners = template.query(sql, new RowMapper() {

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
}
