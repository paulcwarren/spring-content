package internal.org.springframework.content.commons.repository;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentRepositoryEventHandler;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.repository.events.HandleBeforeUnsetContent;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class AnnotatedContentRepositoryEventHandlerTest {
	
	private AnnotatedContentRepositoryEventHandler handler;
	{
		Describe("#postProcessAfterInitialization", () -> {
			Context("when initialized with a ContentRepositoryEventHandler bean", () -> {
				JustBeforeEach(() -> {
					handler = new AnnotatedContentRepositoryEventHandler();
					handler.postProcessAfterInitialization(new CustomEventHandler(), "custom-bean");
				});
				It("register the handlers", () -> {
					assertThat(handler.getHandlers().get(BeforeGetContentEvent.class).size(), is(1));
					assertThat(handler.getHandlers().get(AfterGetContentEvent.class).size(), is(1));
					assertThat(handler.getHandlers().get(BeforeSetContentEvent.class).size(), is(1));
					assertThat(handler.getHandlers().get(AfterSetContentEvent.class).size(), is(1));
					assertThat(handler.getHandlers().get(BeforeUnsetContentEvent.class).size(), is(1));
					assertThat(handler.getHandlers().get(AfterUnsetContentEvent.class).size(), is(1));
				});
			});
		});
	}
	
	@ContentRepositoryEventHandler
	public class CustomEventHandler {
		
		@HandleBeforeGetContent
		public void beforeGetContent(Object contentObject) {}

		@HandleAfterGetContent
		public void afterGetContent(Object contentObject, InputStream stream) {}

		@HandleBeforeSetContent
		public void beforeSetContent(Object contentObject, InputStream stream) {}

		@HandleAfterSetContent
		public void afterSetContent(Object contentObject, InputStream stream) {}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(Object contentObject) {}

		@HandleAfterUnsetContent
		public void afterUnsetContent(Object contentObject) {}
	}
	
	@Test
	public void noop() {
		fail("test");
	}
}
