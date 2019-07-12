package org.springframework.content.commons.utils;

import java.lang.reflect.Field;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.hamcrest.CoreMatchers;
import org.junit.runner.RunWith;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class BeanUtilsTest {

	private TestEntity testEntity;

	{
		Describe("BeanUtils", () -> {
			Context("#setFieldWithAnnotation", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
					});
					It("should set field directly", () -> {
						BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class,"a value");
						assertThat(testEntity.fieldOnly, is("a value"));
					});
					It("should set field via its setter", () -> {
						BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class,
								"b value");
						assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
					});
					It("should not fail when told to set on a missing field", () -> {
						try {
							BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
									"value");
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
					});
					It("should set field directly", () -> {
						BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class,
								"a value");
						assertThat(testEntity.fieldOnly, is("a value"));
					});
					It("should set field via its setter", () -> {
						BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class,
								"b value");
						assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
					});
					It("should not fail when told to set on a missing field", () -> {
						try {
							BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
									"value");
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#setFieldWithAnnotationConditionally", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
					});
					It("should set field if the condition matches", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentId.class, "a value", new MatchingCondition() {
								});
						assertThat(testEntity.fieldOnly, is("a value"));
					});
					It("should set field via its setter if the condition matches", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentLength.class, "b value", new MatchingCondition() {
								});
						assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
					});
					It("should not set field if the condition does not match", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentId.class, "a value", new UnmatchingCondition() {
								});
						assertThat(testEntity.fieldOnly, is(nullValue()));
					});
					It("should not set field via its setter if the condition does not match",
							() -> {
								BeanUtils.setFieldWithAnnotationConditionally(testEntity,
										ContentLength.class, "b value",
										new UnmatchingCondition() {
										});
								assertThat(testEntity.getFieldWithGetterSetter(),
										is(nullValue()));
							});
					It("should not fail when told to set on a missing field", () -> {
						try {
							BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
									"value");
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
					});
					It("should set field if the condition matches", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentId.class, "a value", new MatchingCondition() {
								});
						assertThat(testEntity.fieldOnly, is("a value"));
					});
					It("should set field via its setter if the condition matches", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentLength.class, "b value", new MatchingCondition() {
								});
						assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
					});
					It("should not set field if the condition does not match", () -> {
						BeanUtils.setFieldWithAnnotationConditionally(testEntity,
								ContentId.class, "a value", new UnmatchingCondition() {
								});
						assertThat(testEntity.fieldOnly, is(nullValue()));
					});
					It("should not set field via its setter if the condition does not match",
							() -> {
								BeanUtils.setFieldWithAnnotationConditionally(testEntity,
										ContentLength.class, "b value",
										new UnmatchingCondition() {
										});
								assertThat(testEntity.getFieldWithGetterSetter(),
										is(nullValue()));
							});
					It("should not fail when told to set on a missing field", () -> {
						try {
							BeanUtils.setFieldWithAnnotation(testEntity, Override.class,
									"value");
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#getFieldWithAnnotation", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
						testEntity.fieldOnly = "a value";
						testEntity.setFieldWithGetterSetter("b value");
					});
					It("should get field directly", () -> {
						Object value = BeanUtils.getFieldWithAnnotation(testEntity,
								ContentId.class);
						assertThat(value, is("a value"));
					});
					It("should get field via its getter", () -> {
						Object value = BeanUtils.getFieldWithAnnotation(testEntity,
								ContentLength.class);
						assertThat(value, is("b value"));
					});
					It("should not fail when told to get on a missing field", () -> {
						try {
							BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
						testEntity.fieldOnly = "a value";
						testEntity.setFieldWithGetterSetter("b value");
					});
					It("should get field directly", () -> {
						Object value = BeanUtils.getFieldWithAnnotation(testEntity,
								ContentId.class);
						assertThat(value, is("a value"));
					});
					It("should get field via its getter", () -> {
						Object value = BeanUtils.getFieldWithAnnotation(testEntity,
								ContentLength.class);
						assertThat(value, is("b value"));
					});
					It("should not fail when told to get on a missing field", () -> {
						try {
							BeanUtils.getFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#hasFieldWithAnnotation", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
					});
					It("should return true for annotated public fields", () -> {
						assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
								ContentId.class), is(true));
					});
					It("should return true for annotated private fields with getter",
							() -> {
								assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
										ContentLength.class), is(true));
							});
					It("should not fail when about a missing field", () -> {
						try {
							BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
					});
					It("should return true for annotated public fields", () -> {
						assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
								ContentId.class), is(true));
					});
					It("should return true for annotated private fields with getter",
							() -> {
								assertThat(BeanUtils.hasFieldWithAnnotation(testEntity,
										ContentLength.class), is(true));
							});
					It("should not fail when about a missing field", () -> {
						try {
							BeanUtils.hasFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#getFieldWithAnnotationType", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
					});
					It("should return true for annotated public fields", () -> {
						assertThat(
								BeanUtils.getFieldWithAnnotationType(testEntity,
										ContentId.class),
								is(CoreMatchers.<Class<?>>equalTo(String.class)));
					});
					It("should return true for annotated private fields with getter",
							() -> {
								assertThat(
										BeanUtils.getFieldWithAnnotationType(testEntity,
												ContentLength.class),
										is(CoreMatchers.<Class<?>>equalTo(String.class)));
							});
					It("should not fail when asked about a missing field", () -> {
						try {
							BeanUtils.getFieldWithAnnotationType(testEntity,
									Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
					});
					It("should return true for annotated public fields", () -> {
						assertThat(
								BeanUtils.getFieldWithAnnotationType(testEntity,
										ContentId.class),
								is(CoreMatchers.<Class<?>>equalTo(String.class)));
					});
					It("should return true for annotated private fields with getter",
							() -> {
								assertThat(
										BeanUtils.getFieldWithAnnotationType(testEntity,
												ContentLength.class),
										is(CoreMatchers.<Class<?>>equalTo(String.class)));
							});
					It("should not fail when asked about a missing field", () -> {
						try {
							BeanUtils.getFieldWithAnnotationType(testEntity,
									Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#findFieldWithAnnotation", () -> {
				Context("given a simple, non-inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new TestEntity();
					});
					It("should find fields", () -> {
						assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
								ContentId.class), is(not(nullValue())));
					});
					It("should find fields with getters", () -> {
						assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
								ContentLength.class), is(not(nullValue())));
					});
					It("should not fail when about a missing field", () -> {
						try {
							BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
				Context("given an inheriting class", () -> {
					BeforeEach(() -> {
						testEntity = new InheritingTestEntity();
					});
					It("should find fields", () -> {
						assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
								ContentId.class), is(not(nullValue())));
					});
					It("should find fields with getters", () -> {
						assertThat(BeanUtils.findFieldWithAnnotation(testEntity,
								ContentLength.class), is(not(nullValue())));
					});
					It("should not fail when about a missing field", () -> {
						try {
							BeanUtils.findFieldWithAnnotation(testEntity, Override.class);
						}
						catch (Exception e) {
							fail("should not fail");
						}
					});
				});
			});

			Context("#findFieldsWithAnnotation", () -> {

				It("should find fields", () -> {
					assertThat(BeanUtils.findFieldsWithAnnotation(TestEntity2.class, MimeType.class, new BeanWrapperImpl(new TestEntity2())).length, is(1));
				});
			});

			Context("#getFieldsWithAnnotation", () -> {

				It("should find fields", () -> {
					TestEntity2 t = new TestEntity2();
					t.setContentId("100");
					t.setOtherContentId("200");

					assertThat(BeanUtils.getFieldsWithAnnotation(t, ContentId.class), is(new Object[]{"100", "200"}));
				});
			});
		});
	}

	public static class TestEntity {
		@ContentId
		public String fieldOnly;
		@ContentLength
		private String fieldWithGetterSetter;

		public TestEntity() {
		}

		public String getFieldWithGetterSetter() {
			return fieldWithGetterSetter;
		}

		public void setFieldWithGetterSetter(String fieldWithGetterSetter) {
			this.fieldWithGetterSetter = fieldWithGetterSetter;
		}
	}

	public static class InheritingTestEntity extends TestEntity {}

	public static class TestEntity2 {
		@ContentId public String contentId;
		@ContentLength private String contentLen;
		@MimeType private String mimeType;

		@ContentId public String otherContentId;
		@ContentLength private String otherContentLen;

		public String getContentId() {
			return contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public String getContentLen() {
			return contentLen;
		}

		public void setContentLen(String contentLen) {
			this.contentLen = contentLen;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getOtherContentId() {
			return otherContentId;
		}

		public void setOtherContentId(String otherContentId) {
			this.otherContentId = otherContentId;
		}

		public String getOtherContentLen() {
			return otherContentLen;
		}

		public void setOtherContentLen(String otherContentLen) {
			this.otherContentLen = otherContentLen;
		}
	}

	public static class MatchingCondition implements Condition {
		@Override
		public boolean matches(Field field) {
			return true;
		}
	}

	public static class UnmatchingCondition implements Condition {
		@Override
		public boolean matches(Field field) {
			return false;
		}
	}
}
