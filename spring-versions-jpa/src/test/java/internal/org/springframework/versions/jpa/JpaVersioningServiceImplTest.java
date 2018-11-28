package internal.org.springframework.versions.jpa;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import lombok.Getter;
import lombok.Setter;
import org.junit.runner.RunWith;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
public class JpaVersioningServiceImplTest {

    private VersioningService versioner;

    private Object result;

    private EntityManager em;

    private TestEntity entity, successor, ancestralRoot, ancestor;
    private String versionNo, versionLabel;

    {
        Describe("JpaVersioningServiceImpl", () -> {
            JustBeforeEach(() -> {
                versioner = new JpaVersioningServiceImpl(em);
            });
            Context("#establishAncestralRoot", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                });
                JustBeforeEach(() -> {
                    result = versioner.establishAncestralRoot(entity);
                });
                It("should set the @AncestorId to null", () -> {
                    assertThat(entity.getAncestorId(), is(nullValue()));
                });
                It("should set the @AncestorRootId to it's own id", () -> {
                    assertThat(entity.getAncestorRootId(), is(entity.getId()));
                });
//                It("should set the @SuccessorId to null", () -> {
//                    assertThat(entity.getSuccessorId(), is(nullValue()));
//                });
                It("should return the entity", () -> {
                    assertThat(result, is(entity));
                });
            });
            Context("#establishAncestor", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    successor = new TestEntity();
                    successor.setId(999L);
                });
                JustBeforeEach(() -> {
                    result = versioner.establishAncestor(entity, successor);
                });
//                It("should set the @VersionStatus to false", () -> {
//                    assertThat(entity.getVersionStatus(), is(false));
//                });
                It("should set the @SuccessorId to null", () -> {
                    assertThat(entity.getSuccessorId(), is(999L));
                });
                It("should return the entity", () -> {
                    assertThat(result, is(entity));
                });
            });
            Context("#establishSuccessor", () -> {
                BeforeEach(() -> {
                    successor = new TestEntity();
                    ancestralRoot = new TestEntity();
                    ancestralRoot.setId(1234L);
                    ancestor = new TestEntity();
                    ancestor.setId(5678L);
                    versionNo = "1.1";
                    versionLabel = "a new version";
                });
                JustBeforeEach(() -> {
                    result = versioner.establishSuccessor(successor, versionNo, versionLabel, ancestralRoot, ancestor);
                });
//                It("should reset the @Id", () -> {
//                    assertThat(successor.getId(), is(nullValue()));
//                });
//                It("should reset the @Version", () -> {
//                    assertThat(successor.getVersion(), is(0L));
//                });
                It("should set the @VersionNumber", () -> {
                    assertThat(successor.getVersionNo(), is("1.1"));
                });
                It("should set the @VersionLabel", () -> {
                    assertThat(successor.getVersionLabel(), is("a new version"));
                });
//                It("should set the @VersionStatus to true", () -> {
//                    assertThat(successor.getVersionStatus(), is(true));
//                });
                It("should set the @SuccessorId to null", () -> {
                    assertThat(successor.getSuccessorId(), is(nullValue()));
                });
                It("should set the @AncestorRootId", () -> {
                    assertThat(successor.getAncestorRootId(), is(1234L));
                });
                It("should set the @AncestorId", () -> {
                    assertThat(successor.getAncestorId(), is(5678L));
                });
                It("should return the entity", () -> {
                    assertThat(result, is(successor));
                });
            });
        });
    }

    @Getter
    @Setter
    private class TestEntity {
        @Id private Long id;
        @Version private Long version;
        @AncestorId private Long ancestorId;
        @AncestorRootId private Long ancestorRootId;
        @SuccessorId private Long successorId;
        @VersionNumber private String versionNo;
        @VersionLabel private String versionLabel;
//        @VersionStatus private Boolean versionStatus;
    }
}
