package internal.org.springframework.content.rest.mappings;

import java.util.UUID;

import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.Store;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;

@RunWith(Ginkgo4jRunner.class)
public class CorsConfigurationBuilderTest {

	private CorsConfigurationBuilder builder;
	private CorsConfiguration config;
	
	private Class<?> storeInterface;
	
	private Exception e;
	
	{
		Describe("CorsConfigurationBuilderTest", () -> {
			Context("#build", () -> {
				JustBeforeEach(() -> {
					builder = new CorsConfigurationBuilder();
					try {
						config = builder.build(storeInterface);
					} catch (Exception e) {
						this.e = e;
					}
				});
				Context("given no CrossOrigin annotation", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithNoCrossOrigin.class;
					});
					It("should not a cors configuration", () -> {
						assertThat(config, is(nullValue()));
					});
				});
				Context("given an origins value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithOrigins.class;
					});
					It("should create a cors configuration with those origins", () -> {
						assertThat(config.getAllowedOrigins(), hasItems("http://domain1.com", "http://domain2.com"));
					});
				});
				Context("given an empty origins value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithEmptyOrigins.class;
					});
					It("should set the default origin", ()-> {
						assertThat(config.getAllowedOrigins(), hasItems("*"));
					});
				});
				Context("given an allowedMethods value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithAllowedMethods.class;
					});
					It("should create a cors configuration with those allowed methods", () -> {
						assertThat(config.getAllowedMethods(), hasItems("GET", "PUT", "POST", "DELETE"));
					});
				});
				Context("given an empty allowedMethods value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithEmptyAllowedMethods.class;
					});
					It("should set the default allowedMethods", ()-> {
						assertThat(config.getAllowedMethods(), hasItems("GET", "PUT", "POST", "DELETE", "HEAD", "PATCH", "OPTIONS", "TRACE"));
					});
				});
				Context("given an allowedHeaders value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithAllowedHeaders.class;
					});
					It("should create a cors configuration with those allowed headers", () -> {
						assertThat(config.getAllowedHeaders(), hasItems("header1", "header2"));
					});
				});
				Context("given an empty allowedHeaders value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithEmptyAllowedHeaders.class;
					});
					It("should set the default allowedHeaders", ()-> {
						assertThat(config.getAllowedHeaders(), hasItems("*"));
					});
				});
				Context("given an exposedHeaders value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithExposedHeaders.class;
					});
					It("should create a cors configuration with those exposed headers", () -> {
						assertThat(config.getExposedHeaders(), hasItems("exposed1", "exposed2"));
					});
				});
				Context("given an empty exposedHeaders value", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithEmptyExposedHeaders.class;
					});
					It("should create a cors configuration with a null exposedHeaders", ()-> {
						assertThat(config.getExposedHeaders(), is(nullValue()));
					});
				});
				Context("given an allowCredentials value of 'true'", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithAllowCredentials.class;
					});
					It("should create a cors configuration with that value", () -> {
						assertThat(config.getAllowCredentials(), is(true));
					});
				});
				Context("given an allowCredentials value of 'false'", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithDisallowCredentials.class;
					});
					It("should create a cors configuration with that value", () -> {
						assertThat(config.getAllowCredentials(), is(false));
					});
				});
				Context("given an allowCredentials value of 'something-else'", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithMisconfiguredAllowCredentials.class;
					});
					It("should create a cors configuration with that value", () -> {
						assertThat(e, is(not(nullValue())));
					});
				});
				Context("given an allowCredentials value of ''", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithEmptyAllowCredentials.class;
					});
					It("should create a cors configuration with the default allow credentials value", () -> {
						assertThat(config.getAllowCredentials(), is(true));
					});
				});
				Context("given a positive max-age specification", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithMaxAge.class;
					});
					It("should create a cors configuration with that max age", () -> {
						assertThat(config.getMaxAge(), is(1000L));
					});
				});
				Context("given an zero max-age specification", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithZeroMaxAge.class;
					});
					It("should create a cors configuration with that max age", () -> {
						assertThat(config.getMaxAge(), is(0L));
					});
				});
				Context("given an negative max-age specification", () -> {
					BeforeEach(() -> {
						storeInterface = StoreWithNegativeMaxAge.class;
					});
					It("should create a cors configuration with the default max age", () -> {
						assertThat(config.getMaxAge(), is(1800L));
					});
				});
			});
		});
	}
	
	public interface StoreWithNoCrossOrigin extends Store<UUID> {}

	@CrossOrigin(origins={"http://domain1.com", "http://domain2.com"})
	public interface StoreWithOrigins extends Store<UUID> {}

	@CrossOrigin(origins={})
	public interface StoreWithEmptyOrigins extends Store<UUID> {}

	@CrossOrigin(allowedHeaders={"header1", "header2"})
	public interface StoreWithAllowedHeaders extends Store<UUID> {}

	@CrossOrigin(allowedHeaders={})
	public interface StoreWithEmptyAllowedHeaders extends Store<UUID> {}

	@CrossOrigin(exposedHeaders={"exposed1", "exposed2"})
	public interface StoreWithExposedHeaders extends Store<UUID> {}

	@CrossOrigin(exposedHeaders={})
	public interface StoreWithEmptyExposedHeaders extends Store<UUID> {}

	@CrossOrigin(methods={RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.DELETE})
	public interface StoreWithAllowedMethods extends Store<UUID> {}

	@CrossOrigin(methods={})
	public interface StoreWithEmptyAllowedMethods extends Store<UUID> {}

	@CrossOrigin(allowCredentials="true")
	public interface StoreWithAllowCredentials extends Store<UUID> {}

	@CrossOrigin(allowCredentials="false")
	public interface StoreWithDisallowCredentials extends Store<UUID> {}

	@CrossOrigin(allowCredentials="something-else")
	public interface StoreWithMisconfiguredAllowCredentials extends Store<UUID> {}

	@CrossOrigin(allowCredentials="")
	public interface StoreWithEmptyAllowCredentials extends Store<UUID> {}

	@CrossOrigin(maxAge=1000L)
	public interface StoreWithMaxAge extends Store<UUID> {}

	@CrossOrigin(maxAge=0L)
	public interface StoreWithZeroMaxAge extends Store<UUID> {}

	@CrossOrigin(maxAge=-1000L)
	public interface StoreWithNegativeMaxAge extends Store<UUID> {}
}
