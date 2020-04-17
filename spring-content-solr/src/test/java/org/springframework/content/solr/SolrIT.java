package org.springframework.content.solr;

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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@ContextConfiguration(classes = {SolrITConfig.class})
public class SolrIT {

	@Autowired
	private DocumentRepository docRepo;
	@Autowired
	private DocumentContentRepository docContentRepo;
	@Autowired
	private SolrClient solr; //for tests
	@Autowired
	private SolrProperties solrProperties;
	private Document doc;
	private String id = null;

	{
		Describe("Index", () -> {
			BeforeEach(() -> {
				solrProperties.setUser(System.getenv("SOLR_USER"));
				solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

				doc = new Document();
				doc.setTitle("title of document 1");
				doc.setAuthor("author@email.com");
				docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
				try {
					doc = docRepo.save(doc);
				} catch (Exception e) {
					int i=0;
				}
			});
			AfterEach(() -> {
				if (docContentRepo != null) {
					docContentRepo.unsetContent(doc);
				}
				if (docRepo != null) {
					docRepo.delete(doc);
				}
				if (solr != null) {
					UpdateRequest req = new UpdateRequest();
					req.deleteByQuery("*");
					req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
					req.process(solr, null);
					req.commit(solr, null);
				}
			});
			It("should index the content of that document", () -> {
				assertThat(() -> {
					SolrQuery query = new SolrQuery();
					query.setQuery("foo");
					String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
					query.addFilterQuery(fq);
					QueryRequest request = new QueryRequest(query);
					request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
					QueryResponse response = null;
					try {
						response = request.process(solr);
						return response.getResults();
					}
					catch (Exception e) {
						fail(e.getMessage());
					}

					return null;
				}, eventuallyEval(hasSize(1),Duration.ofSeconds(10)));
			});
			Context("when the content is searched", () -> {
				It("should return the searched content", () -> {
					Iterable<String> content = docContentRepo.search("one");
					assertThat(content, CoreMatchers.hasItem(doc.getContentId()));
				});
			});
			Context("given that documents content is updated", () -> {
				BeforeEach(() -> {
					docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/two.rtf"));
					docRepo.save(doc);
				});
				It("should index the new content", () -> {
					assertThat(() -> {
						SolrQuery query = new SolrQuery();
						query.setQuery("bar");
						String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
						query.addFilterQuery(fq);
						QueryRequest request = new QueryRequest(query);
						request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
						QueryResponse response = null;
						try {
							response = request.process(solr);
							return response.getResults();
						}
						catch (Exception e) {
							fail(e.getMessage());
						}

						return null;
					}, eventuallyEval(hasSize(1),Duration.ofSeconds(10)));
				});
			});
			Context("given that document is deleted", () -> {
				BeforeEach(() -> {
					id = doc.getContentId().toString();
					docContentRepo.unsetContent(doc);
					docRepo.delete(doc);
				});
				It("should delete the record of the content from the index", () -> {
					SolrQuery query = new SolrQuery();
					query.setQuery("one");
					query.addFilterQuery("id:" + "examples.models.Document\\:" + id);
					query.setFields("content");

					QueryRequest request = new QueryRequest(query);
					request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());

					QueryResponse response = request.process(solr);
					SolrDocumentList results = response.getResults();

					assertThat(results.size(), is(0));
				});
			});
		});

		Describe("Paging", () -> {
			Describe("Paging", () -> {

				BeforeEach(() -> {
					solrProperties.setUser(System.getenv("SOLR_USER"));
					solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

					for (int i=0; i < 10; i++) {
						Document doc = new Document();
						doc.setTitle(format("doc %s", i));
						doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
						docRepo.save(doc);
					}
				});

				AfterEach(() -> {
					if (docRepo != null && docContentRepo != null) {
						for (Document doc : docRepo.findAll()) {
							doc = docContentRepo.unsetContent(doc);
						}
						for (Document doc : docRepo.findAll()) {
							docRepo.deleteById(doc.getId());
						}
					}
					if (solr != null) {
						UpdateRequest req = new UpdateRequest();
						req.deleteByQuery("*");
						req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
						req.process(solr, null);
						req.commit(solr, null);
					}
				});

				It("should return results in pages", () -> {
					MatcherAssert.assertThat(() -> docContentRepo.search("foo", PageRequest.of(0, 3)),
							eventuallyEval(hasSize(3), Duration.ofSeconds(10)));

					MatcherAssert.assertThat(() -> docContentRepo.search("foo", PageRequest.of(1, 3)),
							eventuallyEval(hasSize(3), Duration.ofSeconds(10)));

					MatcherAssert.assertThat(() -> docContentRepo.search("foo", PageRequest.of(2, 3)),
							eventuallyEval(hasSize(3), Duration.ofSeconds(10)));

					MatcherAssert.assertThat(() -> docContentRepo.search("foo", PageRequest.of(3, 3)),
							eventuallyEval(hasSize(1), Duration.ofSeconds(10)));
				});

				It("should return specific result page", () -> {
					MatcherAssert.assertThat(() -> docContentRepo.search("foo", PageRequest.of(3, 3)),
							eventuallyEval(hasSize(1), Duration.ofSeconds(10)));
				});
			});
		});
	}

	@Test
	public void noop() {
	}

	@Entity
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Document {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String title;
		private String author;

		@ContentId
		private String contentId;
	}

	public interface DocumentRepository extends CrudRepository<Document, Long> {
	}

	public interface DocumentContentRepository extends ContentStore<Document, String>, Searchable<String> {
	}
}