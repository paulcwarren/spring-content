package internal.org.springframework.content.rest.storeresolver;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;

import internal.org.springframework.content.rest.support.TestEntity2;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class StoreResolverRestConfigurationIT {

    @Autowired
    private Application.TEntityRepository repo;

    @Autowired
    private Application.TEntityJpaStore jpaStore;

    @Autowired
    private Application.TEntityFsStore fsStore;

    @LocalServerPort
    int port;

    private Application.TEntity tEntity;

    private TestEntity2 existingClaim;

    {
        Describe("JpaRest", () -> {

            BeforeEach(() -> {
                RestAssured.port = port;
            });

            Context("given that claim has existing content", () -> {
                BeforeEach(() -> {
                    tEntity = new Application.TEntity();
                    tEntity = repo.save(tEntity);
                });
                It("should return the content from the correct store", () -> {

                    assertThat(jpaStore, is(not(nullValue())));
                    assertThat(fsStore, is(not(nullValue())));
                    assertThat(repo, is(not(nullValue())));

                    String newContent = "This is some new content";

                    given()
                            .contentType("text/plain")
                            .content(newContent.getBytes())
                            .when()
                            .post("/tEntities/" + tEntity.getId())
                            .then()
                            .statusCode(HttpStatus.SC_CREATED);

                    // refetch
                    tEntity = repo.findById(tEntity.getId()).get();

                    try (InputStream is = fsStore.getContent(tEntity)) {
                        assertThat(IOUtils.toString(is), is(newContent));
                    }

                    try (InputStream is = jpaStore.getContent(tEntity)) {
                        assertThat(is, is(nullValue()));
                    }
                });
            });
        });
    }

    @Test
    public void noop() {}
}
