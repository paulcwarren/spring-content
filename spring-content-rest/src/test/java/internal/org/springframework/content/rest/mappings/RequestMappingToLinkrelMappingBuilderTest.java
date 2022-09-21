package internal.org.springframework.content.rest.mappings;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import java.util.UUID;

import internal.org.springframework.content.rest.mappingcontext.RequestMappingToLinkrelMappingBuilder;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.mappingcontext.ClassWalker;
import org.springframework.content.rest.RestResource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import javax.persistence.Embedded;
import javax.persistence.Id;

@RunWith(Ginkgo4jRunner.class)
public class RequestMappingToLinkrelMappingBuilderTest {

    {
        Describe("RequestMappingToLinkrelMappingBuilder", () -> {
            It("create a map of request to linkrel mappings", () -> {
                RequestMappingToLinkrelMappingBuilder visitor = new RequestMappingToLinkrelMappingBuilder();
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass.class);

                assertThat(visitor.getRequestMappings(), hasEntry("child/child/content", "one/two/three"));
                assertThat(visitor.getRequestMappings(), hasEntry("child/child/preview", "one/two/preview"));
                assertThat(visitor.getRequestMappings(), hasEntry("child/child/thumbnail", "one/two/thumbnail"));
                assertThat(visitor.getRequestMappings(), hasEntry("child/childWithout/content", "one/childWithout/three"));
                assertThat(visitor.getRequestMappings(), hasEntry("child/childWithout/preview", "one/childWithout/preview"));
                assertThat(visitor.getRequestMappings(), hasEntry("child/childWithout/thumbnail", "one/childWithout/thumbnail"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/child/content", "childWithout/two/three"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/child/preview", "childWithout/two/preview"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/child/thumbnail", "childWithout/two/thumbnail"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/childWithout/content", "childWithout/childWithout/three"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/childWithout/preview", "childWithout/childWithout/preview"));
                assertThat(visitor.getRequestMappings(), hasEntry("childWithout/childWithout/thumbnail", "childWithout/childWithout/thumbnail"));
            });

            It("create a map of request to linkrel mappings for issue #1049", () -> {
                RequestMappingToLinkrelMappingBuilder visitor = new RequestMappingToLinkrelMappingBuilder();
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(Contract.class);

                assertThat(visitor.getRequestMappings(), hasEntry("_package", "package"));
                assertThat(visitor.getRequestMappings(), hasEntry("customer/_private", "primary-customer/private"));
            });
        });
    }

    public static class TestSubClass {
        @RestResource(linkRel = "two")
        private TestSubSubClass child;
        private TestSubSubClass childWithout;
    }

    public static class TestClass {
        @RestResource(linkRel = "one")
        private TestSubClass child;
        private TestSubClass childWithout;
    }

    public static class TestSubSubClass {
        @RestResource(linkRel = "three")
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        @RestResource(linkRel = "should-be-ignored")
        private String shouldBeIgnored;

        private @ContentId UUID previewId;
        private @ContentLength Long previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;

        @RestResource(exported = true)
        private @ContentId UUID thumbnailId;
        private @ContentLength Long thumbnailLen;
        private @MimeType String thumbnailMimeType;
    }

    public class Contract {
        @Id
        private Long id;

        @Embedded
        @RestResource(linkRel="package")
        private Content _package;

        @Embedded
        @RestResource(linkRel="primary-customer")
        private Customer customer;
    }

    public class Customer {
        @Embedded
        @RestResource(linkRel="private")
        private Content _private;
    }

    public class Content {
        @ContentId
        private UUID id;

        @ContentLength
        private long len;
    }
}
