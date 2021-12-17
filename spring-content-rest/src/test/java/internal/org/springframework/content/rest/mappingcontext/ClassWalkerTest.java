package internal.org.springframework.content.rest.mappingcontext;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;

import org.junit.runner.RunWith;
import org.springframework.util.ReflectionUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ClassWalkerTest {

    private static Field strField = ReflectionUtils.findField(TestClass.class, "str");
    private static Field iField = ReflectionUtils.findField(TestClass.class, "i");

    {
        Describe("ClassWalker", () -> {
            It("should visit all fields", () -> {
                ClassVisitor visitor = new ClassVisitor();
                ClassWalker walker = new ClassWalker(TestClass.class);
                walker.accept(visitor);

                assertThat(visitor.getFields(), hasItems(strField, iField));
            });
        });
    }

    public static class TestClass {

        private String str;
        private int i;
    }
}
