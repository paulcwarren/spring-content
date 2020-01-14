package internal.org.springframework.versions.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.versions.interceptors.OptimisticLockingInterceptor;
import org.springframework.versions.interceptors.PessimisticLockingInterceptor;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;

@RunWith(Ginkgo4jRunner.class)
public class JpaLockingAndVersioningProxyFactoryImplIT {

    private JpaLockingAndVersioningProxyFactoryImpl factory;

	private AnnotationConfigApplicationContext context;
	
    private PlatformTransactionManager txn;
    private EntityManager em;
    private LockingService locker;
    private AuthenticationFacade auth;

    private ProxyFactory proxyFactory;

    {
        Describe("JpaLockingAndVersioningProxyFactoryImpl", () -> {
			Context("given a context with a repository and a store", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
					
					txn = context.getBean(PlatformTransactionManager.class);
					EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
					em = emf.createEntityManager();
					locker = context.getBean(LockingService.class);
					auth = context.getBean(AuthenticationFacade.class);
				});
	            JustBeforeEach(() -> {
	                factory = new JpaLockingAndVersioningProxyFactoryImpl(context, txn, em, locker, auth);
	            });
	            Context("#apply", () -> {
	            	
	            	Context("given no existng advise", () -> {
	            		BeforeEach(() -> {
	            			proxyFactory = new ProxyFactory();
	            		});
	            		JustBeforeEach(() -> {
	            			factory.apply(proxyFactory);
	            		});
	            		It("should apply the txn advice", () -> {
	            			Advisor[] advices = proxyFactory.getAdvisors();
	            			assertThat(advices.length, is(3));
	            			assertThat(advices[0].getAdvice(), is(instanceOf(TransactionInterceptor.class)));
	            			assertThat(advices[1].getAdvice(), is(instanceOf(OptimisticLockingInterceptor.class)));
	            			assertThat(advices[2].getAdvice(), is(instanceOf(PessimisticLockingInterceptor.class)));
	            		});
	            	});
	            	Context("given an existng txn advise", () -> {
	            		BeforeEach(() -> {
	            			proxyFactory = new ProxyFactory();
	            			proxyFactory.addAdvice(new TransactionInterceptor());
	            		});
	            		JustBeforeEach(() -> {
	            			factory.apply(proxyFactory);
	            		});
	            		It("should not apply the advice again", () -> {
	            			Advisor[] advices = proxyFactory.getAdvisors();
	            			assertThat(advices.length, is(3));
	            			assertThat(advices[0].getAdvice(), is(instanceOf(TransactionInterceptor.class)));
	            			assertThat(advices[1].getAdvice(), is(instanceOf(OptimisticLockingInterceptor.class)));
	            			assertThat(advices[2].getAdvice(), is(instanceOf(PessimisticLockingInterceptor.class)));
	            		});
	            	});
	            });
			});
        });
    }
    
	@Configuration
	@EnableJpaRepositories
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
	}
}
