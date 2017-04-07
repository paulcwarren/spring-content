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

import java.util.Collections;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.placement.PlacementStrategy;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class PlacementServiceTest {

	private PlacementService service;
	
	private String result = null;
	private Exception exception = null;
	
	//mocks
	private PlacementStrategy<String> strategy;
	private PlacementStrategy<String> spy;
	private Object contentId;

	{
		Describe("PlacementServiceImpl", () -> {
			BeforeEach(() -> {
				strategy = new PlacementStrategy<String>() {
					@Override
					public String getLocation(String contentId) {
						return null;
					}
				};
				spy = Mockito.spy(strategy);
				
				service = new PlacementServiceImpl(Collections.singletonList(spy));
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
						contentId = "12345";
					});
					It("should return null", () -> {
						assertThat(result, is(nullValue()));
					});
				});
				Context("given a placement strategy plugin", () -> {
					Context("given a contentId type that matches the placement strategy", () -> {
						BeforeEach(() -> {
							contentId = "12345";
						});
						It("should call that placement strategy", () -> {
							verify(spy).getLocation(argThat(is("12345")));
						});
					});
					Context("given a contentId type that does not matches the placement strategy", () -> {
						BeforeEach(() -> {
							contentId = 12345L;
						});
						It("should call that placement strategy", () -> {
							verify(spy, never()).getLocation(anyObject());
						});
					});
				});
			});
		});
	}
}
