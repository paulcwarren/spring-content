package internal.org.springframework.content.commons.placementstrategy;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.runner.RunWith;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class UUIDPlacementStrategyTest {

	private UUIDPlacementStrategy strategy;
	
	private String result;
	private UUID contentId;
	private Exception exception;
	
	{
		Describe("UUIDPlacementStrategy", () -> {
			BeforeEach(() -> {
				strategy = new UUIDPlacementStrategy();
			});
			JustBeforeEach(() -> {
				try {
					result = strategy.getLocation(contentId);
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
				Context("given a contentId", () -> {
					BeforeEach(() -> {
						contentId = UUID.fromString("d0689992-14ac-11e7-93ae-92361f002671");
					});
					It("should return a path", () -> {
						assertThat(result, is("/d0689992/14ac/11e7/93ae/92361f002671"));
					});
				});
			});
		});
	}
}
