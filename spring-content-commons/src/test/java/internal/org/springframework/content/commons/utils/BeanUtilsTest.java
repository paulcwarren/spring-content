package internal.org.springframework.content.commons.utils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.utils.BeanUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class BeanUtilsTest {

	private TestEntity testEntity;
	{
		Describe("BeanUtils", () -> {
			Context("setFieldWithAnnotation",  () -> {
				BeforeEach(() -> {
					testEntity = new TestEntity();
				});
				It("should set field directly", () -> {
					BeanUtils.setFieldWithAnnotation(testEntity, ContentId.class, "a value");
					assertThat(testEntity.fieldOnly, is("a value"));
				});
				It("should set field via its setter", () -> {
					BeanUtils.setFieldWithAnnotation(testEntity, ContentLength.class, "b value");
					assertThat(testEntity.getFieldWithGetterSetter(), is("b value"));
				});
			});

			Context("getFieldWithAnnotation",  () -> {
				BeforeEach(() -> {
					testEntity = new TestEntity();
					testEntity.fieldOnly = "a value";
					testEntity.setFieldWithGetterSetter("b value");
				});
				It("should get field directly", () -> {
					Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentId.class);
					assertThat(value, is("a value"));
				});
				It("should get field via its getter", () -> {
					Object value = BeanUtils.getFieldWithAnnotation(testEntity, ContentLength.class);
					assertThat(value, is("b value"));
				});
			});

			Context("hasFieldWithAnnotation",  () -> {
				BeforeEach(() -> {
					testEntity = new TestEntity();
				});
				It("should return true for annotated public fields", () -> {
					assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentId.class), is(true));
				});
				It("should return true for annotated private fields with getter", () -> {
					assertThat(BeanUtils.hasFieldWithAnnotation(testEntity, ContentLength.class), is(true));
				});
			});
			
			Context("getFieldWithAnnotationType", () -> {
				BeforeEach(() -> {
					testEntity = new TestEntity();
				});
				It("should return true for annotated public fields", () -> {
					assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentId.class), is(CoreMatchers.<Class<?>>equalTo(String.class)));
				});
				It("should return true for annotated private fields with getter", () -> {
					assertThat(BeanUtils.getFieldWithAnnotationType(testEntity, ContentLength.class), is(CoreMatchers.<Class<?>>equalTo(String.class)));
				});
			});
		});
	}
	
	public class TestEntity {
		@ContentId public String fieldOnly;
		@ContentLength private String fieldWithGetterSetter;
		public String getFieldWithGetterSetter() {
			return fieldWithGetterSetter;
		}
		public void setFieldWithGetterSetter(String fieldWithGetterSetter) {
			this.fieldWithGetterSetter = fieldWithGetterSetter;
		}
	}
}
