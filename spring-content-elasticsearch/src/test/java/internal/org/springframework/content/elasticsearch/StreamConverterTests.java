package internal.org.springframework.content.elasticsearch;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class StreamConverterTests {
	
	private StreamConverter converter;
	
	{
		Describe("StreamConverter", () -> {
			Context("#convert", () -> {
				It("should convert a stream into a byte array", () -> {
					
				});
			});
		});
	}

}
