package org.springframework.content.commons.utils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(Ginkgo4jRunner.class)
public class PlacementServiceImplTest {

	private PlacementServiceImpl placer = null;

	{
		Describe("PlacementServiceImpl", () -> {
			BeforeEach(() -> {
				placer = new PlacementServiceImpl();
			});
			Context("given a placement service", () -> {
				It("should have removed the FallbackObjectToStringConverter", () -> {
					assertThat(placer.canConvert(Object.class, String.class), is(false));
				});
			});
		});
	}
}
