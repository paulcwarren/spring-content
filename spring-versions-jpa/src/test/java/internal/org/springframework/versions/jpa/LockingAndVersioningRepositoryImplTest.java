package internal.org.springframework.versions.jpa;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.security.core.Authentication;
import org.springframework.versions.LockOwner;
import org.springframework.versions.impl.LockingAndVersioningRepositoryImpl;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.Metamodel;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class LockingAndVersioningRepositoryImplTest {

    private LockingAndVersioningRepositoryImpl<Document, Long> repo;

    private EntityManager em;
    private JpaEntityInformation<Document, Long> ei;
    private Metamodel mm;
    private IdentifiableType it;
    private AuthenticationFacade sec;
    private LockingService locker;

    private Document doc;
    private Long id = 0L;

    private Authentication auth;

    private Exception e;

    {
        Describe("LockingAndVersioningRepositoryImpl", () -> {
            BeforeEach(() -> {
                em = mock(EntityManager.class);
                ei = mock(JpaEntityInformationSupport.class);
                locker = mock(LockingService.class);
                sec = mock(AuthenticationFacade.class);

                doc = new Document();
                doc.setId(getNextId());

                when(em.merge(eq(doc))).thenReturn(doc);
            });
            JustBeforeEach(() -> {
                repo = new LockingAndVersioningRepositoryImpl(em, sec, locker);
                try {
                    doc = repo.lock(doc);
                } catch (Exception e) {
                    this.e = e;
                }
            });
        });
    }

    private Long getNextId() {
        synchronized (id) {
            return id = id++;
        }
    }

    private class Document {

        @Id
        private Long id;

        @LockOwner
        private String lockOwner;

        private String aField;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLockOwner() {
            return lockOwner;
        }

        public void setLockOwner(String lockOwner) {
            this.lockOwner = lockOwner;
        }

        public String getaField() {
            return aField;
        }

        public void setaField(String aField) {
            this.aField = aField;
        }
    }
}