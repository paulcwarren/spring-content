package internal.org.springframework.versions.jpa;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.versions.LockingAndVersioningException;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
public class JpaCloningServiceImplTest {

    private JpaCloningServiceImpl cloner;

    private Object entity;
    private Object result;

    private Exception e;

    {
        Describe("JpaCloningServiceImpl", () -> {
            JustBeforeEach(() -> {
                cloner = new JpaCloningServiceImpl();
            });
            Context("#clone", () -> {
                JustBeforeEach(() -> {
                    try {
                        result = cloner.clone(entity);
                    } catch (Exception e) {
                        this.e = e;
                    }
                });
                Context("given an entity with a copy constructor", () -> {
                    BeforeEach(() -> {
                        entity = new TestEntity();
                    });
                    It("should clone the entity", () -> {
                        assertThat(result, is(not(nullValue())));
                        assertThat(result, is(not(entity)));
                    });
                });
                Context("given an entity with a copy constructor", () -> {
                    BeforeEach(() -> {
                        entity = new NoCopyConstructorTestEntity();
                    });
                    It("should clone the entity", () -> {
                        assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                        assertThat(e.getMessage(), containsString("no copy constructor"));
                    });
                });
                Context("given an entity with a failing copy constructor", () -> {
                    BeforeEach(() -> {
                        entity = new FailingCopyConstructorTestEntity();
                    });
                    It("should clone the entity", () -> {
                        assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                        assertThat(e.getMessage(), containsString("copy constructor failed"));
                    });
                });
            });
        });
    }

    public static class TestEntity {
        public TestEntity() {}

        public TestEntity(TestEntity entity) {
        }
    }

    public static class NoCopyConstructorTestEntity {
        public NoCopyConstructorTestEntity() {}
    }

    public static class FailingCopyConstructorTestEntity {
        public FailingCopyConstructorTestEntity() {}

        public FailingCopyConstructorTestEntity(FailingCopyConstructorTestEntity entity) {
            throw new RuntimeException();
        }
    }
}
