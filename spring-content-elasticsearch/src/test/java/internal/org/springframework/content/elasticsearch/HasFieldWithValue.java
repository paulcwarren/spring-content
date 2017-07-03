package internal.org.springframework.content.elasticsearch;

import com.vtence.hamcrest.jpa.Reflection;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.lang.reflect.Field;

import static org.hamcrest.Matchers.anything;

public class HasFieldWithValue<T, U> extends TypeSafeDiagnosingMatcher<T> {

    private final String fieldName;
    private final Matcher<? super U> valueMatcher;

    public HasFieldWithValue(String fieldName, Matcher<? super U> valueMatcher) {
        this.fieldName = fieldName;
        this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(T argument, Description mismatchDescription) {
        Field field = getField(argument, mismatchDescription);
        if (field == null) return false;

        Object fieldValue = Reflection.readField(argument, field);
        boolean valueMatches = valueMatcher.matches(fieldValue);
        if (!valueMatches) {
            mismatchDescription.appendText("\"" + fieldName + "\" ");
            valueMatcher.describeMismatch(fieldValue, mismatchDescription);
        }
        return valueMatches;
    }

    private Field getField(T argument, Description mismatchDescription) {
        Field field = null;
        Class clazz = argument.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    break;
                }
            } catch (NoSuchFieldException e) {
                // ignore
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            mismatchDescription.appendText("no field \"" + fieldName + "\"");
        }
        return field;
    }

    public void describeTo(Description description) {
        description.appendText("has field \"");
        description.appendText(fieldName);
        description.appendText("\": ");
        description.appendDescriptionOf(valueMatcher);
    }

    public static <T, U> Matcher<T> hasField(String field, Matcher<? super U> value) {
        return new HasFieldWithValue<T, U>(field, value);
    }

    public static <T> Matcher<T> hasField(String field) {
        return new HasFieldWithValue<>(field, anything());
    }
}
