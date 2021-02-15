package org.springframework.content.elasticsearch;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.matchers.Eventually.eventually;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ElasticsearchIT {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private DocumentRepository repo;
    private DocumentContentStore store;
    private DocumentStoreSearchable searchableStore;

    private RestHighLevelClient client;

    private Document doc1, doc2;
    private UUID id1, id2 = null;

    private String indexName;

    private static Class<?>[] indexStrategyContexts = new Class<?>[]{GlobalIndexingStrategy.class/*, EntityIndexingStrategy.class*/};

    {
        for (Class<?> indexStrategyContext : indexStrategyContexts) {

            Describe(format("Index Strategy %s", strategyName(indexStrategyContext)), () -> {

                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(indexStrategyContext);
                    context.register(ElasticsearchConfig.class);
                    context.refresh();

                    repo = context.getBean(DocumentRepository.class);
                    store = context.getBean(DocumentContentStore.class);
                    client = context.getBean(RestHighLevelClient.class);
                    ((IndexingStrategy)context.getBean(indexStrategyContext)).setup();
                    indexName = ((IndexingStrategy)context.getBean(indexStrategyContext)).indexName();
                });

                AfterEach(() -> {
                    assertThat(context, is(not(nullValue())));

                    try {
                        // assert the right index exists as a double check we are testing the correct thing!
                        if (client != null) {
                            GetIndexRequest gir = new GetIndexRequest(indexName);
                            GetIndexResponse resp = client.indices().get(gir, RequestOptions.DEFAULT);
                            assertThat(resp.getIndices().length, is(1));
                        }
                    } catch (ElasticsearchStatusException ese) {}

                    try {
                        DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                        client.indices().delete(dir, RequestOptions.DEFAULT);
                    } catch (ElasticsearchStatusException ese) {}
                });

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
                        if (doc1 != null) {
                            store.unsetContent(doc1);
                            repo.delete(doc1);
                        }

                        if (doc2 != null) {
                            store.unsetContent(doc2);
                            repo.delete(doc2);
                        }
                    });

                    It("should index the documents", () -> {
                        GetRequest req = new GetRequest(indexName, doc1.getClass().getName(), doc1.getContentId().toString());
                        GetResponse res = client.get(req, RequestOptions.DEFAULT);
                        assertThat(res.isExists(), is(true));

                        req = new GetRequest(indexName, doc1.getClass().getName(), doc2.getContentId().toString());
                        res = client.get(req, RequestOptions.DEFAULT);
                        assertThat(res.isExists(), is(true));
                    });

                    It("should be possible to close the index", () -> {
                        IndexService indexer = (context.getBean(IndexService.class));
                        indexer.index(doc1, new ByteArrayInputStream("customized index".getBytes()));

                        AcknowledgedResponse resp = client.indices().close(new CloseIndexRequest(indexName), RequestOptions.DEFAULT);
                        assertThat(resp.isAcknowledged(), is(true));

                        String command = format("curl -X GET http://localhost:9200/_cat/indices/%s?h=status", indexName);
                        Process process = Runtime.getRuntime().exec(command);

                        InputStream inputStream = process.getInputStream();
                        process.waitFor();

                        int exitCode = process.exitValue();
                        assertThat(exitCode, is(0));

                        assertThat(IOUtils.toString(inputStream), containsString("close"));
                    });

                    Context("when the content is searched", () -> {

                        It("should return the matches", () -> {

                            eventually(
                                    () -> {
                                        return store.search("one"); },
                                    (result) -> {
                                        assertThat(result,allOf(
                                                hasItem(doc1.getContentId()),
                                                not(hasItem(doc2.getContentId()))
                                                ));
                                        }
                            );

                            eventually(
                                    () -> {
                                        return store.search("two"); },
                                    (result) -> {
                                        assertThat(result, allOf(
                                                not(hasItem(doc1.getContentId())),
                                                hasItem(doc2.getContentId())
                                                ));
                                        }
                            );

                            eventually(
                                    () -> {
                                        return store.search("one two"); },
                                    (result) -> {
                                        assertThat(result, hasItems(doc1.getContentId(), doc2.getContentId()));
                                        }
                            );

                            eventually(
                                    () -> {
                                        return store.search("+document +one -two"); },
                                    (result) -> {
                                        assertThat(result, allOf(
                                                hasItem(doc1.getContentId()),
                                                hasItem(not(doc2.getContentId()))
                                                ));
                                        }
                            );

                        });
                    });

                    Context("given a text extracting renderer", () -> {

                        BeforeEach(() -> {
                            doc1 = new Document();
                            doc1.setTitle("doc 1");
                            doc1.setAuthor("author@email.com");
                            doc1.setMimeType("image/png");
                            doc1 = store.setContent(doc1, this.getClass().getResourceAsStream("/image.png"));
                            doc1 = repo.save(doc1);
                        });

                        It("should index the documents", () -> {

                            eventually(
                                    () -> {
                                        return store.search("wisdom"); },
                                    (result) -> {
                                        assertThat(result, hasItem(doc1.getContentId()));
                                        }
                            );
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

                        AfterEach(() -> {
                            doc1 = null;
                            doc2 = null;
                        });

                        It("should delete the record of the content from the index", () -> {
                            GetRequest req = new GetRequest(indexName, doc1.getClass().getName(), id1.toString());
                            GetResponse res = client.get(req, RequestOptions.DEFAULT);
                            assertThat(res.isExists(), is(false));

                            req = new GetRequest(indexName, doc1.getClass().getName(), id2.toString());
                            res = client.get(req, RequestOptions.DEFAULT);
                            assertThat(res.isExists(), is(false));
                        });
                    });
                });
            });
        }

        Describe("Paging", () -> {

            BeforeEach(() -> {
                context = new AnnotationConfigApplicationContext();
                context.register(EntityIndexingStrategy.class);
                context.register(ElasticsearchConfig.class);
                context.refresh();

                repo = context.getBean(DocumentRepository.class);
                store = context.getBean(DocumentContentStore.class);
                client = context.getBean(RestHighLevelClient.class);

                for (int i=0; i < 10; i++) {
                    Document doc = new Document();
                    doc.setTitle(format("doc %s", i));
                    doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                    repo.save(doc);
                }
            });

            AfterEach(() -> {
                assertThat(context, is(not(nullValue())));

                if (client != null) {
                    DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                    client.indices().delete(dir, RequestOptions.DEFAULT);
                }
            });

            It("should return results in pages", () -> {

                eventually(
                        () -> {return store.search("one", PageRequest.of(0, 3));},
                        (page) -> {
                            assertThat(page.getTotalElements(), is(10L));
                            assertThat(page.getTotalPages(), is(4));
                            assertThat(page.getNumberOfElements(), is(3));
                            assertThat(page.getContent().size(), is(3));
                        });

                eventually(
                        () -> {return store.search("one", PageRequest.of(1, 3));},
                        (page) -> {
                            assertThat(page.getTotalElements(), is(10L));
                            assertThat(page.getTotalPages(), is(4));
                            assertThat(page.getNumberOfElements(), is(3));
                            assertThat(page.getContent().size(), is(3));
                        });

                eventually(
                        () -> {return store.search("one", PageRequest.of(2, 3));},
                        (page) -> {
                            assertThat(page.getTotalElements(), is(10L));
                            assertThat(page.getTotalPages(), is(4));
                            assertThat(page.getNumberOfElements(), is(3));
                            assertThat(page.getContent().size(), is(3));
                        });

                eventually(
                        () -> {return store.search("one", PageRequest.of(3, 3));},
                        (page) -> {
                            assertThat(page.getTotalElements(), is(10L));
                            assertThat(page.getTotalPages(), is(4));
                            assertThat(page.getNumberOfElements(), is(1));
                            assertThat(page.getContent().size(), is(1));
                        });
            });
        });

        Describe("Custom Attributes", () -> {

            Context("given a context configured to sync attributes and provide a filter query", () -> {

                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(EntityIndexingStrategy.class);
                    context.register(CustomAttributesConfig.class);
                    context.refresh();

                    repo = context.getBean(DocumentRepository.class);
                    store = context.getBean(DocumentContentStore.class);
                    client = context.getBean(RestHighLevelClient.class);

                    doc1 = new Document();
                    doc1.setTitle(format("doc 1"));
                    doc1.setAuthor("Buck Rogers");
                    doc1 = store.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                    repo.save(doc1);

                    doc2 = new Document();
                    doc2.setTitle(format("doc 2"));
                    doc1.setAuthor("Wilma Deering");
                    doc2 = store.setContent(doc2, this.getClass().getResourceAsStream("/one.docx"));
                    repo.save(doc2);
                });

                AfterEach(() -> {
                    assertThat(context, is(not(nullValue())));

                    if (client != null) {
                        DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                        client.indices().delete(dir, RequestOptions.DEFAULT);
                    }
                });

                It("should return the specified attributes", () -> {
                    assertThat(() -> store.search("one", PageRequest.of(0, 10)),
                            eventuallyEval(
                                    allOf(
                                            hasItem(doc1.getContentId()),
                                            not(hasItem(doc2.getContentId()))),
                                    Duration.ofSeconds(10)));
                });
            });
        });

        Describe("Custom Return Types", () -> {

            BeforeEach(() -> {
                context = new AnnotationConfigApplicationContext();
                context.register(EntityIndexingStrategy.class);
                context.register(CustomAttributesConfig.class);
                context.refresh();

                repo = context.getBean(DocumentRepository.class);
                searchableStore = context.getBean(DocumentStoreSearchable.class);
                client = context.getBean(RestHighLevelClient.class);

                doc1 = new Document();
                doc1.setTitle(format("A document about one"));
                doc1.setAuthor("Buck Rogers");
                doc1 = searchableStore.setContent(doc1, this.getClass().getResourceAsStream("/one.docx"));
                repo.save(doc1);
            });

            AfterEach(() -> {
                assertThat(context, is(not(nullValue())));

                if (client != null) {
                    DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                    client.indices().delete(dir, RequestOptions.DEFAULT);
                }
            });

            It("should return results using the custom return type", () -> {

                eventually(
                        () -> {
                        return searchableStore.search("one");},
                    (result) -> {
                        assertThat(result, is(not(nullValue())));
                        Iterator<FulltextInfo> iterator = result.iterator();
                        assertThat(iterator.hasNext(), is(true));
                        assertThat(iterator.next(), allOf(
                                hasProperty("contentId", is(doc1.getContentId())),
                                hasProperty("highlight", containsString("<em>one</em>")),
                                hasProperty("author", containsString("Buck Rogers"))
                            ));
                        assertThat(iterator.hasNext(), is(false));
                    }
                );
            });
        });
    }

    private String strategyName(Class<?> indexStrategyContext) {
        return indexStrategyContext.getSimpleName();
    }

    public interface DocumentRepository extends CrudRepository<Document, Long> {
        //
    }

    public interface DocumentContentStore extends ContentStore<Document, UUID>, Searchable<UUID>, Renderable<Document> {
        //
    }

    public interface DocumentStoreSearchable extends ContentStore<Document, UUID>, Searchable<FulltextInfo> {
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
        private UUID contentId;

        @MimeType
        private String mimeType;

        private String title;
        private String author;
    }


    @Getter
    @Setter
    public static class FulltextInfo {

        @ContentId
        private UUID contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "author")
        private String author;
    }
}