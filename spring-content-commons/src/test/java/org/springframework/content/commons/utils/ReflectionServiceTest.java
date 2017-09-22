package org.springframework.content.commons.utils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.runner.RunWith;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.util.ReflectionUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ReflectionServiceTest {

	private ReflectionService reflectionService;
	
	// mocks
	private HelloWorldService service;
	
	{
		Describe("ReflectionService", () -> {
			Context("invokeMethod",  () -> {
				BeforeEach(() -> {
					service = mock(HelloWorldService.class);
				});
				JustBeforeEach(() -> {
					reflectionService = new ReflectionServiceImpl();
					reflectionService.invokeMethod(ReflectionUtils.findMethod(HelloWorldService.class, "helloWorld"), service, new Object[]{});
				});
				It("should invoke the method", () -> {
					verify(service).helloWorld();
				});
			});
		});
	}
	
	public interface HelloWorldService {
		void helloWorld();
	}
}
