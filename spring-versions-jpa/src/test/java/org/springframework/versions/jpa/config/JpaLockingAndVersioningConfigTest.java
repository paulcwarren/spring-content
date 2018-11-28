package org.springframework.versions.jpa.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.VersioningService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
public class JpaLockingAndVersioningConfigTest {

    private AnnotationConfigApplicationContext context;

    {
        Describe("JpaLockingAndVersioningConfig", () -> {
            BeforeEach(() -> {
                context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();
            });
            It("should have an AuthenticationFacade bean", () -> {
                assertThat(context.getBean(AuthenticationFacade.class), is(not(nullValue())));
            });
            It("should have an EntityInformationFacade bean", () -> {
                assertThat(context.getBean(EntityInformationFacade.class), is(not(nullValue())));
            });
            It("should have a LockingService bean", () -> {
                assertThat(context.getBean(LockingService.class), is(not(nullValue())));
            });
            It("should have a VersioningService bean", () -> {
                assertThat(context.getBean(VersioningService.class), is(not(nullValue())));
            });
            It("should have a CloningService bean", () -> {
                assertThat(context.getBean(CloningService.class), is(not(nullValue())));
            });
            It("should have a LockingAndVersioningProxyFactory bean", () -> {
                assertThat(context.getBean(LockingAndVersioningProxyFactory.class), is(not(nullValue())));
            });
        });
    }


    @Test
    public void noop() {
    }

    @Configuration
    @Import(JpaLockingAndVersioningConfig.class)
    public static class TestConfig {

        @Bean
        public DataSource ds() {
            return mock(DataSource.class);
        }

        @Bean
        public PlatformTransactionManager txn() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        public EntityManager em() {
            return mock(EntityManager.class);
        }
    }

}
