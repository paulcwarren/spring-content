package internal.org.springframework.renditions.context;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@ContextConfiguration(classes = {RenditionsConfig.class})
public class RenditionsIntegrationTest {

	@Autowired
	private RenditionServiceImpl renditionService;

	@Autowired
	private DocumentRepository repo;

	@Autowired
	private DocumentContentStore store;

	private Document doc1, doc2;
	private String id1, id2 = null;

	{
		Describe("Renditions Integration", () -> {

			Context("given a document", () -> {

				BeforeEach(() -> {
					doc1 = new Document();
					doc1.setTitle("doc 1");
					doc1.setAuthor("author@email.com");
					doc1.setMimeType("image/png");
					store.setContent(doc1, this.getClass().getResourceAsStream("/image.png"));
					doc1 = repo.save(doc1);
				});

				AfterEach(() -> {
					store.unsetContent(doc1);
					repo.delete(doc1);
				});

				It("should be capable of serving a rendition", () -> {

					InputStream rendition = store.getRendition(doc1, "text/plain");
					assertThat(rendition, is(nullValue()));
				});

				Context("given a configured renderer", () -> {

					BeforeEach(() -> {
						renditionService.setProviders(new RenditionProvider[]{new ImageToTestRender()});
					});

					AfterEach(() -> {
						renditionService.setProviders(new RenditionProvider[]{});
					});

					It("should be capable of serving a rendition", () -> {

						InputStream rendition = store.getRendition(doc1, "text/plain");
						assertThat(rendition, is(not(nullValue())));
						try {
							assertThat(IOUtils.toString(rendition), is("image rendition"));
						} finally {
							IOUtils.closeQuietly(rendition);
						}
					});
				});
			});
		});
	}

	@Test
	public void noop() {
	}

	public interface DocumentRepository extends CrudRepository<Document, Long> {
		//
	}

	public interface DocumentContentStore extends ContentStore<Document, String>, Renderable<Document> {
		//
	}

	@Entity
	@NoArgsConstructor
	@Getter
	@Setter
	public static class Document {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ContentId
		private String contentId;

		@MimeType
		private String mimeType;

		private String title;
		private String author;
	}

	public static class ImageToTestRender implements RenditionProvider {

		@Override
		public String consumes() {
			return "image/png";
		}

		@Override
		public String[] produces() {
			return new String[] {"text/plain"};
		}

		@Override
		public InputStream convert(InputStream fromInputSource, String toMimeType) {
			return new ByteArrayInputStream("image rendition".getBytes());
		}
	}
}