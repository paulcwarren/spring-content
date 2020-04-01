package internal.org.springframework.content.solr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.solr.SolrProperties;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class SolrFulltextIndexServiceImplTest {

    private SolrFulltextIndexServiceImpl indexer;

    private TEntity entity;
    private InputStream content;

    private Exception e;

    // mocks
    private SolrClient solr;
    private SolrProperties props;

    {
        Describe("#index", () -> {

            BeforeEach(() -> {
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new TEntity("12345");
                content = new ByteArrayInputStream("foo".getBytes());
            });

            JustBeforeEach(() -> {
                indexer = new SolrFulltextIndexServiceImpl(solr, props);

                try {
                    indexer.index(entity, content);
                } catch (Exception e) {
                    this.e = e;
                }
            });

            for (Exception ex : new Exception[] {new SolrServerException("badness"), new IOException("badness")}) {

                Context(format("when solr throws a %s", ex.getClass().getSimpleName()), () -> {

                    BeforeEach(() -> {
                        when(solr.request(anyObject(), any())).thenThrow(ex);
                    });

                    It("should throw a StoreAccessException", () -> {
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                        assertThat(e.getMessage(), containsString("badness"));
                    });
                });
            }
        });

        Describe("#unindex", () -> {

            BeforeEach(() -> {
                solr = mock(SolrClient.class);
                props = new SolrProperties();

                entity = new TEntity("12345");
            });

            JustBeforeEach(() -> {
                indexer = new SolrFulltextIndexServiceImpl(solr, props);

                try {
                    indexer.unindex(entity);
                } catch (Exception e) {
                    this.e = e;
                }
            });

            for (Exception ex : new Exception[] {new SolrServerException("badness"), new IOException("badness")}) {

                Context(format("when solr throws a %s", ex.getClass().getSimpleName()), () -> {

                    BeforeEach(() -> {
                        when(solr.request(anyObject(), any())).thenThrow(ex);
                    });

                    It("should throw a StoreAccessException", () -> {
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                        assertThat(e.getMessage(), containsString("badness"));
                    });
                });
            }
        });
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class TEntity {

        @ContentId
        private String contentId;
    }
}


