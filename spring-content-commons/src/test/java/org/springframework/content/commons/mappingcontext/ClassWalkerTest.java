package org.springframework.content.commons.mappingcontext;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;

import java.lang.reflect.Field;
import java.util.UUID;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.util.ReflectionUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ClassWalkerTest {

    private static Field strField = ReflectionUtils.findField(TestClass.class, "str");
    private static Field iField = ReflectionUtils.findField(TestClass.class, "i");
    private static Field contentIdField = ReflectionUtils.findField(TestClass.class, "contentId");
    private static Field contentLengthField = ReflectionUtils.findField(TestClass.class, "contentLength");
    private static Field subContentIdField = ReflectionUtils.findField(TestSubClass.class, "contentId");
    private static Field subContentLengthField = ReflectionUtils.findField(TestSubClass.class, "contentLength");
    private static Field subSubContentIdField = ReflectionUtils.findField(TestSubSubClass.class, "contentId");
    private static Field subSubContentLengthField = ReflectionUtils.findField(TestSubSubClass.class, "contentLength");

    {
        Describe("ClassWalker", () -> {
            It("should visit all fields", () -> {
                ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor("/", ".", new ContentPropertyBuilderVisitor.CanonicalName());
                ClassWalker walker = new ClassWalker(TestClass.class);
                walker.accept(visitor);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("content");
                expectedProperty.setContentIdPropertyPath("contentId");
                expectedProperty.setContentLengthPropertyPath("contentLength");
                expectedProperty.setMimeTypePropertyPath("contentMimeType");
                expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty));

                ContentProperty expectedSubClassProperty = new ContentProperty();
                expectedSubClassProperty.setContentPropertyPath("child.content");
                expectedSubClassProperty.setContentIdPropertyPath("child.contentId");
                expectedSubClassProperty.setContentLengthPropertyPath("child.contentLength");
                expectedSubClassProperty.setMimeTypePropertyPath("child.contentMimeType");
                expectedSubClassProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/content", expectedSubClassProperty));

                ContentProperty expectedSubSubClassProperty = new ContentProperty();
                expectedSubSubClassProperty.setContentPropertyPath("child.subChild.content");
                expectedSubSubClassProperty.setContentIdPropertyPath("child.subChild.contentId");
                expectedSubSubClassProperty.setContentLengthPropertyPath("child.subChild.contentLength");
                expectedSubSubClassProperty.setMimeTypePropertyPath("child.subChild.contentMimeType");
                expectedSubSubClassProperty.setOriginalFileNamePropertyPath("child.subChild.contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/subChild", expectedSubSubClassProperty));
                assertThat(visitor.getProperties(), hasEntry("child/subChild/content", expectedSubSubClassProperty));
            });
        });

        Context("given a class with uncorrelated attributes", () -> {
            It("should return two content properties", () -> {
                ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor("/", ".", new ContentPropertyBuilderVisitor.CanonicalName());
                ClassWalker walker = new ClassWalker(UncorrelatedAttrClass.class);
                walker.accept(visitor);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("content");
                expectedProperty.setContentIdPropertyPath("contentId");
                expectedProperty.setContentLengthPropertyPath("len");
                expectedProperty.setMimeTypePropertyPath("mimeType");
                expectedProperty.setOriginalFileNamePropertyPath("originalFileName");
                assertThat(visitor.getProperties(), hasEntry("", expectedProperty));

                ContentProperty expectedProperty2 = new ContentProperty();
                expectedProperty2.setContentPropertyPath("content");
                expectedProperty2.setContentIdPropertyPath("contentId");
                expectedProperty2.setContentLengthPropertyPath("len");
                expectedProperty2.setMimeTypePropertyPath("mimeType");
                expectedProperty2.setOriginalFileNamePropertyPath("originalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty2));
            });
        });

        Context("given a class with a child with uncorrelated attributes", () -> {
            It("should return two content properties", () -> {
                ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor("/", ".", new ContentPropertyBuilderVisitor.CanonicalName());
                ClassWalker walker = new ClassWalker(TestClass2.class);
                walker.accept(visitor);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("child.content");
                expectedProperty.setContentIdPropertyPath("child.contentId");
                expectedProperty.setContentLengthPropertyPath("child.len");
                expectedProperty.setMimeTypePropertyPath("child.mimeType");
                expectedProperty.setOriginalFileNamePropertyPath("child.originalFileName");
                assertThat(visitor.getProperties(), hasEntry("child", expectedProperty));

                ContentProperty expectedProperty2 = new ContentProperty();
                expectedProperty2.setContentPropertyPath("child.content");
                expectedProperty2.setContentIdPropertyPath("child.contentId");
                expectedProperty2.setContentLengthPropertyPath("child.len");
                expectedProperty2.setMimeTypePropertyPath("child.mimeType");
                expectedProperty2.setOriginalFileNamePropertyPath("child.originalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/content", expectedProperty2));
            });
        });

        Context("given a class with relation attributes", () -> {
            It("should not traverse them", () -> {
                ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor("/", ".", new ContentPropertyBuilderVisitor.CanonicalName());
                ClassWalker walker = new ClassWalker(TestClass3.class);
                walker.accept(visitor);

                assertThat(visitor.getProperties(), aMapWithSize(0));
            });
        });

        Context("given a class with content properties in its super class", () -> {
            It("should visit them", () -> {
                ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor("/", ".", new ContentPropertyBuilderVisitor.CanonicalName());
                ClassWalker walker = new ClassWalker(TestClass4.class);
                walker.accept(visitor);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("content");
                expectedProperty.setContentIdPropertyPath("contentId");
                expectedProperty.setContentLengthPropertyPath("contentLength");
                expectedProperty.setMimeTypePropertyPath("contentMimeType");
                expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty));
            });
        });
    }

    public static class TestClass {

        private TestClass circlularRef;

        private String str;
        private int i;
        @ContentId
        private String contentId;
        @ContentLength
        private Long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private TestSubClass child;
    }

    public static class TestSubClass {
        @ContentId
        private String contentId;
        @ContentLength
        private Long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
        private TestSubSubClass subChild;
    }

    public static class TestSubSubClass {
        @ContentId
        private String contentId;
        @ContentLength
        private Long contentLength;
        @MimeType
        private Long contentMimeType;
        @OriginalFileName
        private Long contentOriginalFileName;
    }

    public static class UncorrelatedAttrClass {
        private @Id @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
        private @OriginalFileName String originalFileName;
        private String title;
    }

    public static class TestClass2 {
        private UncorrelatedAttrClass child;
    }

    public static class TestClass3 {

        @OneToOne
        private TestSubClass oneToOne;
        @OneToMany
        private TestSubClass oneToMany;
        @ManyToOne
        private TestSubClass manyToOne;
        @ManyToMany
        private TestSubClass manyToMany;
        @DBRef
        private TestSubClass docRef;
    }

    public static class TestClass4 extends TestSubSubClass {
    }
}
