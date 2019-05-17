package org.springframework.content.elasticsearch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.DeletePipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.ingest.WritePipelineResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.runner.RunWith;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.CLUSTER_NAME;

@RunWith(Ginkgo4jRunner.class)
//@Ginkgo4jConfiguration(threads=1)
public class ElasticsearchTest {

	private static final String HTTP_BASE_URL = "http://localhost";
	private static final String HTTP_PORT = "9205";
	private static final String HTTP_TRANSPORT_PORT = "9305";
	private static final String elasticSearchBaseUrl = HTTP_BASE_URL + ":" + HTTP_PORT;
	private static final String ES_WORKING_DIR = "target/es";
	private static Node node;

	private static EmbeddedElastic embeddedElastic = null;
	private static Integer refCount = 0;

	private RestHighLevelClient client = null;

	private WritePipelineResponse wpr = null;

	private String id, id2 = null;

	private String attachment;

	private boolean embeddedServer = true;

	{
		Describe("ElasticsearchIndexer", () -> {

			BeforeEach(() -> {

				if (embeddedServer) {
					System.out.println("Thread " + Thread.currentThread().getId() + " waiting");
					synchronized (refCount) {
						System.out.println("Thread " + Thread.currentThread().getId() + " got lock");
						if (refCount == 0) {
							System.out.println("Thread " + Thread.currentThread().getId() + " deploying elasticsearch");
							embeddedElastic = EmbeddedElastic.builder()
									.withElasticVersion("6.4.3")
									.withSetting(CLUSTER_NAME, "testCluster")
									.withSetting("discovery.zen.ping_timeout", 0)
									.withPlugin("ingest-attachment")
									.withEsJavaOpts("-Xms128m -Xmx512m")
									.withStartTimeout(1, MINUTES)
									.build()
									.start();
						}
						else {
							System.out.println("Thread " + Thread.currentThread().getId() + " skipping elasticsearch");
						}
						refCount++;
						System.out.println("Thread " + Thread.currentThread()
								.getId() + " incremented refcount: " + refCount);
					}
					client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
				} else {
					client = new RestHighLevelClient(RestClient.builder(new HttpHost("search-test2-tnipwmdwedcr6d2rmale5bjp7u.eu-west-2.es.amazonaws.com")));
				}

				attachment = "attachment_" + UUID.randomUUID().toString();
			});

			AfterEach(() -> {
				if (embeddedServer) {
					synchronized (refCount) {
						refCount--;
						System.out.println("Thread " + Thread.currentThread()
								.getId() + " decrementing refcount: " + refCount);
						if (refCount == 0 && embeddedElastic != null) {
							System.out.println("Thread " + Thread.currentThread().getId() + " stopping elasticsearch");
							embeddedElastic.stop();
						}
						else {
							System.out.println("Thread " + Thread.currentThread()
									.getId() + " skipping elasticsearch stop");
						}
					}
				}
			});

			It("the attachment pipeline should not exist", () -> {
				GetPipelineRequest getRequest = new GetPipelineRequest(attachment);
				GetPipelineResponse res = client.ingest().getPipeline(getRequest, RequestOptions.DEFAULT);
				assertThat(res.isFound(), is(false));
			});

			Context("when the attachment pipeline is created", () -> {
				BeforeEach(() -> {
					String source = "{\"description\":\"Extract attachment information encoded in Base64 with UTF-8 charset\"," +
							"\"processors\":[{\"attachment\":{\"field\":\"data\"}}]}";
					PutPipelineRequest put = new PutPipelineRequest(attachment,
							new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
							XContentType.JSON);
					wpr = client.ingest().putPipeline(put, RequestOptions.DEFAULT);

					assertThat(eval(() -> {
						GetPipelineRequest gpr = new GetPipelineRequest(attachment);
						GetPipelineResponse gpres = null;
						gpres = client.ingest().getPipeline(gpr, RequestOptions.DEFAULT);
						return gpres.isFound();
					}), eventuallyEval(is(true)));

				});

				AfterEach(() -> {
					System.out.println("deleting attachment pipeline");
					DeletePipelineRequest del = new DeletePipelineRequest(attachment);
					wpr = client.ingest().deletePipeline(del, RequestOptions.DEFAULT);
					assertThat(wpr.isAcknowledged(), is(true));
				});

				It("should succeed", () -> {
					assertThat(wpr.isAcknowledged(), is(true));
				});

				Context("when a document is indexed", () -> {

					BeforeEach(() -> {
						id = UUID.randomUUID().toString();

						IndexRequest req = new IndexRequest("test_index", "test_type", id);
						req.setPipeline(attachment);

						String source = "{" +
								"\"data\": \"UWJveCBlbmFibGVzIGxhdW5jaGluZyBzdXBwb3J0ZWQsIGZ1bGx5LW1hbmFnZWQsIFJFU1RmdWwgRWxhc3RpY3NlYXJjaCBTZXJ2aWNlIGluc3RhbnRseS4g\"" +
								"}";

						req.source(source, XContentType.JSON);
						IndexResponse res = client.index(req, RequestOptions.DEFAULT);
						assertThat(res.getResult(), is(DocWriteResponse.Result.CREATED));
					});

					AfterEach(() -> {
						DeleteRequest req = new DeleteRequest("test_index", "test_type", id);
						DeleteResponse res = client.delete(req, RequestOptions.DEFAULT);
						assertThat(res.getResult(), is(DocWriteResponse.Result.DELETED));
					});

					It("should be searchable", () -> {

						GetRequest req = new GetRequest("test_index", "test_type", id);
						GetResponse res2 = client.get(req, RequestOptions.DEFAULT);
						assertThat(res2.isExists(), is(true));

						assertThat(eval(() -> {
							SearchRequest searchRequest = new SearchRequest("test_index");
							searchRequest.types("test_type");

							SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
							sourceBuilder.query(QueryBuilders.queryStringQuery("Service"));

							SearchResponse res = client.search(searchRequest, RequestOptions.DEFAULT);

							for (SearchHit hit : res.getHits().getHits()) {
								if (hit.getId().equals(id)) {
									return true;
								}
							}
							return false;
						}), eventuallyEval(is(true)));
					});

					Context("when other documents exist of a different type", () -> {

						BeforeEach(() -> {
							id2 = UUID.randomUUID().toString();

							IndexRequest req = new IndexRequest("test_index_two", "test_type_two", id2);
							req.setPipeline(attachment);

							String source = "{" +
									"\"data\": \"UWJveCBlbmFibGVzIGxhdW5jaGluZyBzdXBwb3J0ZWQsIGZ1bGx5LW1hbmFnZWQsIFJFU1RmdWwgRWxhc3RpY3NlYXJjaCBTZXJ2aWNlIGluc3RhbnRseS4g\"" +
									"}";

							req.source(source, XContentType.JSON);
							IndexResponse res = client.index(req, RequestOptions.DEFAULT);
							assertThat(res.getResult(), is(DocWriteResponse.Result.CREATED));
						});

						AfterEach(() -> {
							DeleteRequest req = new DeleteRequest("test_index_two", "test_type_two", id2);
							DeleteResponse res = client.delete(req, RequestOptions.DEFAULT);
							assertThat(res.getResult(), is(DocWriteResponse.Result.DELETED));
						});

						It("should be still be searchable", () -> {

							assertThat(eval(() -> {
								SearchRequest searchRequest = new SearchRequest("test_index");
								searchRequest.types("test_type");

								SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
								sourceBuilder.query(QueryBuilders.queryStringQuery("Service"));

								SearchResponse res = client.search(searchRequest, RequestOptions.DEFAULT);

								for (SearchHit hit : res.getHits().getHits()) {
									if (hit.getId().equals(id)) {
										return true;
									}
								}

								return false;
							}), eventuallyEval(is(true)));

							assertThat(eval(() -> {
								SearchRequest searchRequest = new SearchRequest("test_index");
								searchRequest.types("test_type");

								SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
								sourceBuilder.query(QueryBuilders.queryStringQuery("Service"));

								SearchResponse res = client.search(searchRequest, RequestOptions.DEFAULT);

								for (SearchHit hit : res.getHits().getHits()) {
									if (hit.getId().equals(id2)) {
										return true;
									}
								}

								return false;
							}), eventuallyEval(is(false)));
						});
					});
				});
			});
		});
	}

	@FunctionalInterface
	public interface ThrowingSupplier {
		boolean accept() throws IOException;
	}

	public static Supplier<Boolean> eval(ThrowingSupplier t) {
		return () -> {
			try {
				return t.accept();
			} catch (IOException ex) {
				throw new AssertionError(ex);
			}
		};
	}
}
