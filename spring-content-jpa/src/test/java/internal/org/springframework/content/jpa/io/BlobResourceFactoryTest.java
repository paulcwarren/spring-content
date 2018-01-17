package internal.org.springframework.content.jpa.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
public class BlobResourceFactoryTest {

    private BlobResourceFactory factory;

    private String id;
    private JdbcTemplate template;
    private JdbcTemplateServices templateServices;

    private Resource result;

    {
        Describe("BlobResourceFactory", () -> {
            Context("#newBlobResource", () -> {
                JustBeforeEach(() -> {
                    factory = new BlobResourceFactory(template, templateServices);
                    result = factory.newBlobResource(id);
                });
                It("should return a new blob resource", () -> {
                    assertThat(result, instanceOf(MySQLBlobResource.class));
                });
            });
        });
    }
}
