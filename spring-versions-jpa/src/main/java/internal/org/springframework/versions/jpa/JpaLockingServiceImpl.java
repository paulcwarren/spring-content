package internal.org.springframework.versions.jpa;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.security.auth.Subject;
import java.security.Principal;

import static java.lang.String.format;

@Service
public class JpaLockingServiceImpl implements LockingService {

    private static Log logger = LogFactory.getLog(JpaLockingServiceImpl.class);

    private JdbcTemplate template;
    private PlatformTransactionManager txnMgr;
    private AuthenticationFacade auth;

    @Autowired
    public JpaLockingServiceImpl(JdbcTemplate template, PlatformTransactionManager txnMgr, AuthenticationFacade auth){
        this.template = template;
        this.txnMgr = txnMgr;
        this.auth = auth;
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
        try {
            int rc = template.update(sql, entityId, principal.getName(), entityId);
            return (rc == 1);
        } catch (Exception e) {
            logger.error(format("Unexpected error locking entity with id %s", entityId), e);
        }
        return false;
    }

    @Override
    public boolean unlock(Object entityId, Principal principal) {
        if (principal == null) {
            return false;
        }

        String sql = "DELETE from versions where entity_id = ? and lock_owner = ?;";
        try {
            int rc = template.update(sql, entityId, principal.getName());
            return (rc == 1);
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public boolean isLockOwner(Object entityId, Principal principal) {
        if (principal == null) {
            return false;
        }

        int count = 0;
        String sql = "SELECT count(id) from versions where entity_id = ? and lock_owner = ?;";
        try {
            count = template.queryForObject(sql, Integer.class, entityId, principal.getName());
        } catch (Exception e) {
        }
        return count == 1;
    }

    @Override
    public Principal lockOwner(Object entityId) {
        String lockOwner = null;
        String sql = "SELECT lock_owner from versions where entity_id = ?;";
        try {
            lockOwner = template.queryForObject(sql, String.class, entityId);
        } catch (Exception e) {
        }

        if (lockOwner == null) {
            return null;
        }

        final String name = lockOwner;
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
