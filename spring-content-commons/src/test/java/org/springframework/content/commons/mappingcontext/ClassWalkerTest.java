package org.springframework.content.commons.mappingcontext;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

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
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass.class);

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
                assertThat(visitor.getProperties(), hasEntry("child", expectedSubClassProperty));

                ContentProperty expectedSubSubClassProperty = new ContentProperty();
                expectedSubSubClassProperty.setContentPropertyPath("child.subChild.content");
                expectedSubSubClassProperty.setContentIdPropertyPath("child.subChild.contentId");
                expectedSubSubClassProperty.setContentLengthPropertyPath("child.subChild.contentLength");
                expectedSubSubClassProperty.setMimeTypePropertyPath("child.subChild.contentMimeType");
                expectedSubSubClassProperty.setOriginalFileNamePropertyPath("child.subChild.contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/subChild", expectedSubSubClassProperty));
            });
        });

        Context("given a class with uncorrelated attributes", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(UncorrelatedAttrClass.class);

                ContentProperty expectedProperty2 = new ContentProperty();
                expectedProperty2.setContentPropertyPath("content");
                expectedProperty2.setContentIdPropertyPath("contentId");
                expectedProperty2.setContentLengthPropertyPath("len");
                expectedProperty2.setMimeTypePropertyPath("mimeType");
                expectedProperty2.setOriginalFileNamePropertyPath("originalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty2));
            });
        });

        Context("given a class with correlated attributes", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(CorrelatedAttrClass.class);

                ContentProperty expectedProperty2 = new ContentProperty();
                expectedProperty2.setContentPropertyPath("content");
                expectedProperty2.setContentIdPropertyPath("contentId");
                expectedProperty2.setContentLengthPropertyPath("contentLen");
                expectedProperty2.setMimeTypePropertyPath("contentMimeType");
                expectedProperty2.setOriginalFileNamePropertyPath("contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty2));
            });
        });

        Context("given a class with a child with uncorrelated attributes", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass2.class);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("child.content");
                expectedProperty.setContentIdPropertyPath("child.contentId");
                expectedProperty.setContentLengthPropertyPath("child.len");
                expectedProperty.setMimeTypePropertyPath("child.mimeType");
                expectedProperty.setOriginalFileNamePropertyPath("child.originalFileName");
                assertThat(visitor.getProperties(), hasEntry("child", expectedProperty));
            });
        });

        Context("given a class with relation attributes", () -> {
            It("should not traverse them", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass3.class);

                assertThat(visitor.getProperties().size(), is(0));
            });
        });

        Context("given a class with content properties in its super class", () -> {
            It("should visit them", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass4.class);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("content");
                expectedProperty.setContentIdPropertyPath("contentId");
                expectedProperty.setContentLengthPropertyPath("contentLength");
                expectedProperty.setMimeTypePropertyPath("contentMimeType");
                expectedProperty.setOriginalFileNamePropertyPath("contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("content", expectedProperty));
            });
        });

        Context("given a class with multple child content property objects", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass5.class);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("child1.content");
                expectedProperty.setContentIdPropertyPath("child1.contentId");
                expectedProperty.setContentLengthPropertyPath("child1.len");
                expectedProperty.setMimeTypePropertyPath("child1.mimeType");
                expectedProperty.setOriginalFileNamePropertyPath("child1.originalFileName");
                assertThat(visitor.getProperties(), hasEntry("child1", expectedProperty));

                ContentProperty expectedProperty3 = new ContentProperty();
                expectedProperty3.setContentPropertyPath("child2.content");
                expectedProperty3.setContentIdPropertyPath("child2.contentId");
                expectedProperty3.setContentLengthPropertyPath("child2.len");
                expectedProperty3.setMimeTypePropertyPath("child2.mimeType");
                expectedProperty3.setOriginalFileNamePropertyPath("child2.originalFileName");
                assertThat(visitor.getProperties(), hasEntry("child2", expectedProperty3));
            });
        });

        Context("given a class with a child with multiple content property objects", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass6.class);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("child.content");
                expectedProperty.setContentIdPropertyPath("child.contentId");
                expectedProperty.setContentLengthPropertyPath("child.contentLen");
                expectedProperty.setMimeTypePropertyPath("child.contentMimeType");
                expectedProperty.setOriginalFileNamePropertyPath("child.contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/content", expectedProperty));

                ContentProperty expectedProperty3 = new ContentProperty();
                expectedProperty3.setContentPropertyPath("child.preview");
                expectedProperty3.setContentIdPropertyPath("child.previewId");
                expectedProperty3.setContentLengthPropertyPath("child.previewLen");
                expectedProperty3.setMimeTypePropertyPath("child.previewMimeType");
                expectedProperty3.setOriginalFileNamePropertyPath("child.previewOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/preview", expectedProperty3));
            });
        });

        Context("given a class with a child with a child with multiple content property objects", () -> {
            It("should return two content properties", () -> {
                ContentPropertyMappingContextVisitor visitor = new ContentPropertyMappingContextVisitor("/", ".");
                ClassWalker walker = new ClassWalker(visitor);
                walker.accept(TestClass7.class);

                ContentProperty expectedProperty = new ContentProperty();
                expectedProperty.setContentPropertyPath("child.child.content");
                expectedProperty.setContentIdPropertyPath("child.child.contentId");
                expectedProperty.setContentLengthPropertyPath("child.child.contentLen");
                expectedProperty.setMimeTypePropertyPath("child.child.contentMimeType");
                expectedProperty.setOriginalFileNamePropertyPath("child.child.contentOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/child/content", expectedProperty));

                ContentProperty expectedProperty2 = new ContentProperty();
                expectedProperty2.setContentPropertyPath("child.child.preview");
                expectedProperty2.setContentIdPropertyPath("child.child.previewId");
                expectedProperty2.setContentLengthPropertyPath("child.child.previewLen");
                expectedProperty2.setMimeTypePropertyPath("child.child.previewMimeType");
                expectedProperty2.setOriginalFileNamePropertyPath("child.child.previewOriginalFileName");
                assertThat(visitor.getProperties(), hasEntry("child/child/preview", expectedProperty2));
            });
        });
    }

    public static class TestClass {

        private TestEnum enums;

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

    public static class CorrelatedAttrClass {
        private @Id @GeneratedValue Long id;
        private String name;
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;
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

    public static class TestClass5 {
        private UncorrelatedAttrClass child1;
        private UncorrelatedAttrClass child2;
    }

    public static class TestClass6 {
        private TestMultipleAttrs child;
    }

    public static class TestMultipleAttrs {
        private @ContentId UUID contentId;
        private @ContentLength Long contentLen;
        private @MimeType String contentMimeType;
        private @OriginalFileName String contentOriginalFileName;

        private @ContentId UUID previewId;
        private @ContentLength Long previewLen;
        private @MimeType String previewMimeType;
        private @OriginalFileName String previewOriginalFileName;
    }

    public static class TestClass7 {
        private TestClass6 child;
    }

    public static enum TestEnum {
        A, B, C, D
    }
}
