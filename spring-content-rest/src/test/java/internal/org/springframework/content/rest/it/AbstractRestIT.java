package internal.org.springframework.content.rest.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.ByteArrayInputStream;

import javax.sql.DataSource;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;

import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import net.bytebuddy.utility.RandomString;

public abstract class AbstractRestIT {

    @Autowired
    private TestEntity2Repository claimRepo;

    @Autowired
    private TestEntityChildContentRepository claimFormStore;

    @LocalServerPort
    int port;

    private TestEntity2 existingClaim;

    {
        Describe("JpaRest", () -> {

            Context("Spring Content REST", () -> {
                BeforeEach(() -> {
                    RestAssured.port = port;

                    // delete any existing claim forms
                    Iterable<TestEntity2> existingClaims = claimRepo.findAll();
                    for (TestEntity2 existingClaim : existingClaims) {
                        if (existingClaim.getChild() != null) {
                            claimFormStore.unsetContent(existingClaim.getChild());
                        }
                    }

                    // and claims
                    for (TestEntity2 existingClaim : existingClaims) {
                        claimRepo.delete(existingClaim);
                    }
                });
                Context("given a claim", () -> {
                    BeforeEach(() -> {
                        existingClaim = new TestEntity2();
                        claimRepo.save(existingClaim);
                    });
                    It("should be POSTable with new content with 201 Created", () -> {
                        // assert content does not exist
                        when()
                        .get("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .assertThat()
                        .statusCode(HttpStatus.SC_NOT_FOUND);

                        String newContent = "This is some new content";

                        // POST the new content
                        given()
                        .contentType("text/plain")
                        .content(newContent.getBytes())
                        .when()
                        .post("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);

                        // assert that it now exists
                        given()
                        .header("accept", "text/plain")
                        .get("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .assertThat()
                        .contentType(Matchers.startsWith("text/plain"))
                        .body(Matchers.equalTo(newContent));
                    });
                    Context("given that claim has existing content", () -> {
                        BeforeEach(() -> {
                            existingClaim.setChild(new TestEntityChild());
                            existingClaim.getChild().setMimeType("text/plain");
                            claimFormStore.setContent(existingClaim.getChild(), new ByteArrayInputStream("This is plain text content!".getBytes()));
                            claimRepo.save(existingClaim);
                        });
                        It("should return the content with 200 OK", () -> {
                            given()
                            .header("accept", "text/plain")
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .assertThat()
                            .contentType(Matchers.startsWith("text/plain"))
                            .body(Matchers.equalTo("This is plain text content!"));
                        });
                        It("should be POSTable with new content with 201 Created", () -> {
                            String newContent = "This is new content";

                            given()
                            .contentType("text/plain")
                            .content(newContent.getBytes())
                            .when()
                            .post("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK);

                            given()
                            .header("accept", "text/plain")
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .assertThat()
                            .contentType(Matchers.startsWith("text/plain"))
                            .body(Matchers.equalTo(newContent));
                        });
                        It("should be DELETEable with 204 No Content", () -> {
                            given()
                            .delete("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_NO_CONTENT);

                            // and make sure that it is really gone
                            when()
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_NOT_FOUND);
                        });
                    });
                });
            });
        });
    }

    protected String getId() {
        RandomString random  = new RandomString(5);
        return "/store-tests/" + random.nextString();
    }

    public static String getContextName(Class<?> configClass) {
        return configClass.getSimpleName().replaceAll("Config", "");
    }

    @Test
    public void noop() {}
}
