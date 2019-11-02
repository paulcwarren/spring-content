package internal.org.springframework.content.rest.controllers;

import lombok.Getter;
import lombok.Setter;

import org.springframework.test.web.servlet.MockMvc;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@Setter
public class Cors {

	private MockMvc mvc;
	private String url;

	public static Cors tests(){
		return new Cors();
	}

	{
		Context("an OPTIONS request from a known host", () -> {
			It("should return the relevant CORS headers and OK", () -> {
				mvc.perform(options(url)
						.header("Access-Control-Request-Method", "DELETE")
						.header("Origin", "http://www.someurl.com"))
						.andExpect(status().isOk())
						.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
			});
		});
		Context("an OPTIONS request from an unknown host", () -> {
			It("should be forbidden", () -> {
				mvc.perform(options(url)
						.header("Access-Control-Request-Method", "DELETE")
						.header("Origin", "http://www.someotherurl.com"))
						.andExpect(status().isForbidden());
			});
		});
	}
}
