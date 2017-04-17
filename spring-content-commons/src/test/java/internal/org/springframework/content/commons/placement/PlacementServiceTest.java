package internal.org.springframework.content.commons.placement;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.placement.PlacementStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class PlacementServiceTest {

	private PlacementService service;
	
	private String result = null;
	private Exception exception = null;
	
	//mocks
	private PlacementStrategy<String> strategy1;
	private PlacementStrategy<String> strategy2;
	private PlacementStrategy<String> strategy1Spy;
	private Object contentId;

	{
		Describe("PlacementServiceImpl", () -> {
			BeforeEach(() -> {
				strategy1 = new PlacementStrategy<String>() {
					@Override
					public String getLocation(String contentId) {
						return null;
					}
				};
				strategy1Spy = Mockito.spy(strategy1);
				
				service = new PlacementServiceImpl(Collections.singletonList(strategy1Spy));
			});
			JustBeforeEach(() -> {
				try {
					result = service.getLocation(contentId);
				} catch (IllegalArgumentException eae) {
					exception = eae;
				}
			
			});
			Context("#getLocation", () -> {
				Context("given contentId is null", () -> {
					BeforeEach(() -> {
						contentId = null;
					});
					It("should return a Exception", () -> {
						assertThat(exception, is(not(nullValue())));
					});
				});
				Context("given no placement strategy plugins", () -> {
					BeforeEach(() -> {
						service = new PlacementServiceImpl();
						
						contentId = "12345";
					});
					It("should return the given content ID", () -> {
						assertThat(result, is("12345"));
					});
					Context("when the content ID isnt a String", () -> {
						BeforeEach(() -> {
							contentId = 12345L;
						});
						It("should return the given content ID as a String", () -> {
							assertThat(result, is("12345"));
						});
					});
				});
				Context("given a placement strategy plugin", () -> {
					Context("given a contentId type that matches the placement strategy", () -> {
						BeforeEach(() -> {
							contentId = "12345";
						});
						It("should call that placement strategy", () -> {
							verify(strategy1Spy).getLocation(argThat(is("12345")));
						});
					});
					Context("given a contentId type that does not matches the placement strategy", () -> {
						BeforeEach(() -> {
							contentId = 12345L;
						});
						It("should call that placement strategy", () -> {
							verify(strategy1Spy, never()).getLocation(anyObject());
						});
					});
				});
				Context("given two placement strategy plugins handling the same type", () -> {
					BeforeEach(() -> {
						contentId = "12345";

						strategy1 = new StringPlacementStrategy();
						strategy2 = new AnotherStringPlacementStrategy();

						List<PlacementStrategy<?>> plugins = new ArrayList<>();
						plugins.add(strategy2);
						plugins.add(strategy1);
						service = new PlacementServiceImpl(plugins);
						((InitializingBean)service).afterPropertiesSet();
					});
					It("should call that higher priority plugin", () -> {
						assertThat(((StringPlacementStrategy)strategy1).invoked, is(true));
						assertThat(((AnotherStringPlacementStrategy)strategy2).invoked, is(false));
					});
				});
			});
		});
	}
	
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static class StringPlacementStrategy implements PlacementStrategy<String> {

		public boolean invoked = false;
		
		@Override
		public String getLocation(String contentId) {
			invoked = true;
			return contentId;
		}
		
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class AnotherStringPlacementStrategy implements PlacementStrategy<String> {

		public boolean invoked = false;
		
		@Override
		public String getLocation(String contentId) {
			invoked = true;
			return contentId;
		}
	}
}
