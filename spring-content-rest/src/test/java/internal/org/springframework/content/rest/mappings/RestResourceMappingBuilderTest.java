package internal.org.springframework.content.rest.mappings;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.rest.mappingcontext.RestResourceMappingBuilder;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.mappingcontext.ClassWalker;
import org.springframework.content.rest.RestResource;

import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

@RunWith(Ginkgo4jRunner.class)
public class RestResourceMappingBuilderTest {

    {
        Describe("RestResourceMappingBuilder", () -> {
            It("should create a map of content property paths to request mapping paths", () -> {
                RestResourceMappingBuilder visitor = new RestResourceMappingBuilder((restResourceAnnotation) -> restResourceAnnotation.path());
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass.class);

                assertThat(visitor.getMappings(), hasEntry("child/child/content", "one/two/three"));
                assertThat(visitor.getMappings(), hasEntry("child/child/preview", "one/two/preview"));
                assertThat(visitor.getMappings(), hasEntry("child/child/thumbnail", "one/two/thumbnail"));
                assertThat(visitor.getMappings(), hasEntry("child/child/idcardFront", "one/two/idcard-front"));
                assertThat(visitor.getMappings(), hasEntry("child/childWithout/content", "one/childWithout/three"));
                assertThat(visitor.getMappings(), hasEntry("child/childWithout/preview", "one/childWithout/preview"));
                assertThat(visitor.getMappings(), hasEntry("child/childWithout/thumbnail", "one/childWithout/thumbnail"));
                assertThat(visitor.getMappings(), hasEntry("child/childWithout/idcardFront", "one/childWithout/idcard-front"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/child/content", "childWithout/two/three"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/child/preview", "childWithout/two/preview"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/child/thumbnail", "childWithout/two/thumbnail"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/child/idcardFront", "childWithout/two/idcard-front"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/childWithout/content", "childWithout/childWithout/three"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/childWithout/preview", "childWithout/childWithout/preview"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/childWithout/thumbnail", "childWithout/childWithout/thumbnail"));
                assertThat(visitor.getMappings(), hasEntry("childWithout/childWithout/idcardFront", "childWithout/childWithout/idcard-front"));

                assertThat(visitor.getInverseMappings(), hasEntry("one/two/three", "child/child/content"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/two/preview", "child/child/preview"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/two/thumbnail", "child/child/thumbnail"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/two/idcard-front", "child/child/idcardFront"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/childWithout/three", "child/childWithout/content"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/childWithout/preview", "child/childWithout/preview"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/childWithout/thumbnail", "child/childWithout/thumbnail"));
                assertThat(visitor.getInverseMappings(), hasEntry("one/childWithout/idcard-front", "child/childWithout/idcardFront"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/two/three", "childWithout/child/content"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/two/preview", "childWithout/child/preview"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/two/thumbnail", "childWithout/child/thumbnail"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/two/idcard-front", "childWithout/child/idcardFront"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/childWithout/three", "childWithout/childWithout/content"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/childWithout/preview", "childWithout/childWithout/preview"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/childWithout/thumbnail", "childWithout/childWithout/thumbnail"));
                assertThat(visitor.getInverseMappings(), hasEntry("childWithout/childWithout/idcard-front", "childWithout/childWithout/idcardFront"));
            });
        });
    }

    public static class TestSubClass {
        @RestResource(path = "two")
        private TestSubSubClass child;
        private TestSubSubClass childWithout;
    }

    public static class TestClass {
        @RestResource(path = "one")
        private TestSubClass child;
        private TestSubClass childWithout;
    }

    public static class TestSubSubClass {
        @RestResource(path = "three")
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        @RestResource(path = "should-be-ignored")
        private String shouldBeIgnored;

        private @ContentId UUID previewId;
        private @ContentLength Long previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;

        @RestResource(exported = true)
        private @ContentId UUID thumbnailId;
        private @ContentLength Long thumbnailLen;
        private @MimeType String thumbnailMimeType;

        @RestResource(path="idcard-front")
        private @ContentId UUID idcardFrontId;
        private @ContentLength Long idcardFrontLen;
        private @MimeType String idcardFrontMimeType;
    }
}
