package internal.org.springframework.content.rest.it.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.mockstore.MockContentStore;
import internal.org.springframework.content.rest.support.mockstore.MockStoreFactoryBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class PreferResourceForPutsAndPostsIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private PreferResourceForPutsAndPostsStore store;

    @LocalServerPort
    int port;

    private TestEntity2 existingClaim;

    {
        Describe("PreferResourceForPutsAndPosts", () -> {

            BeforeEach(() -> {
                RestAssured.port = port;
            });

            It("should use the setContent(S, Resource) method", () -> {

                TestEntity tentity = new TestEntity();
                tentity = repo.save(tentity);

                given()
                        .contentType("text/plain")
                        .content("some content".getBytes())
                        .when()
                        .post("/testEntities/" + tentity.getId());

                MockStoreFactoryBean storeFactory = context.getBean(MockStoreFactoryBean.class);

                verify(storeFactory.getMock()).setContent(argThat(isA(TestEntity.class)), (Resource)argThat(isA(Resource.class)));
            });
        });
    }

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<TestEntity, Long> {
    }

    private interface PreferResourceForPutsAndPostsStore extends MockContentStore<TestEntity, UUID> {
    }

    @Test
    public void noop() {
    }
}
