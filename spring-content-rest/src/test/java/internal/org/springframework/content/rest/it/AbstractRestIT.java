package internal.org.springframework.content.rest.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.ByteArrayInputStream;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.content.commons.property.PropertyPath;

import com.jayway.restassured.RestAssured;

import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2JpaStore;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import net.bytebuddy.utility.RandomString;

public abstract class AbstractRestIT {

    @Autowired
    private TestEntity2Repository claimRepo;

    @Autowired
    private TestEntity2JpaStore claimFormStore;

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
                            claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
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
                            claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
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
