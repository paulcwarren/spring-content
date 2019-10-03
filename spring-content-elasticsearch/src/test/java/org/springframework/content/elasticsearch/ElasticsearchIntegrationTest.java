package org.springframework.content.elasticsearch;

import java.time.Duration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@ContextConfiguration(classes = {EmbeddedElasticConfig.class, ElasticsearchConfig.class})
public class ElasticsearchIntegrationTest {

	@Autowired
	private DocumentRepository repo;

	@Autowired
	private DocumentContentStore store;

	@Autowired
	private EmbeddedElastic server;

	@Autowired
	private RestHighLevelClient client;

	private Document doc1, doc2;
	private String id1, id2 = null;

	{
		Describe("Elasticsearch Integration", () -> {

			Context("given some documents", () -> {

				BeforeEach(() -> {
					doc1 = new Document();
					doc1.setTitle("doc 1");
					doc1.setAuthor("author@email.com");
					store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
					doc1 = repo.save(doc1);

					doc2 = new Document();
					doc2.setTitle("doc 2");
					doc2.setAuthor("author@email.com");
					store.setContent(doc2, this.getClass().getResourceAsStream("/two.rtf"));
					doc2 = repo.save(doc2);
				});

				AfterEach(() -> {
					store.unsetContent(doc1);
					repo.delete(doc1);

					store.unsetContent(doc2);
					repo.delete(doc2);
				});

				It("should index the documents", () -> {
					GetRequest req = new GetRequest("spring-content-fulltext-index", doc1.getClass().getName(), doc1.getContentId());
					GetResponse res = client.get(req, RequestOptions.DEFAULT);
					assertThat(res.isExists(), is(true));

					req = new GetRequest("spring-content-fulltext-index", doc1.getClass().getName(), doc2.getContentId());
					res = client.get(req, RequestOptions.DEFAULT);
					assertThat(res.isExists(), is(true));
				});

				Context("when the content is searched", () -> {

					It("should become searchable", () -> {
						assertThat(() -> store.search("one"), eventuallyEval(
								allOf(
										hasItem(doc1.getContentId()),
										not(hasItem(doc2.getContentId()))
								),
								Duration.ofSeconds(10)));

						assertThat(() -> store.search("two"), eventuallyEval(
								allOf(
										not(hasItem(doc1.getContentId())),
										hasItem(doc2.getContentId())
								),
								Duration.ofSeconds(10)));

						assertThat(() -> store.search("one two"), eventuallyEval(hasItems(doc1.getContentId(), doc2.getContentId()), Duration.ofSeconds(10)));

						assertThat(() -> store.search("+document +one -two"), eventuallyEval(
								allOf(
										hasItem(doc1.getContentId()),
										hasItem(not(doc2.getContentId()))
								),
								Duration.ofSeconds(10)));
					});
				});

				Context("when the content is searched with findKeyword", () -> {

					It("should become searchable", () -> {
						assertThat(() -> store.findKeyword("one"), eventuallyEval(
								allOf(
										hasItem(doc1.getContentId()),
										not(hasItem(doc2.getContentId()))
								),
								Duration.ofSeconds(10)));

						assertThat(() -> store.findKeyword("two"), eventuallyEval(
								allOf(
										not(hasItem(doc1.getContentId())),
										hasItem(doc2.getContentId())
								),
								Duration.ofSeconds(10)));
					});
				});

				Context("given that document is deleted", () -> {

					BeforeEach(() -> {
						id1 = doc1.getContentId();
						store.unsetContent(doc1);
						repo.delete(doc1);

						id2 = doc2.getContentId();
						store.unsetContent(doc2);
						repo.delete(doc2);
					});

					It("should delete the record of the content from the index", () -> {
						GetRequest req = new GetRequest("spring-content-fulltext-index", doc1.getClass().getName(), id1);
						GetResponse res = client.get(req, RequestOptions.DEFAULT);
						assertThat(res.isExists(), is(false));

						req = new GetRequest("spring-content-fulltext-index", doc1.getClass().getName(), id2);
						res = client.get(req, RequestOptions.DEFAULT);
						assertThat(res.isExists(), is(false));
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

	public interface DocumentContentStore extends ContentStore<Document, String>, Searchable<String> {
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

		private String title;
		private String author;
	}
}