package internal.org.springframework.content.rest.storeresolver;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.support.TestEntity2;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Application.TEntity tEntity;

    private TestEntity2 existingClaim;

    {
        Describe("JpaRest", () -> {

            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
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
                            .body(newContent.getBytes())
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
