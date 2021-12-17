package internal.org.springframework.content.rest.mappingcontext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

@Getter
public class ClassVisitor {

    private Set<Field> fields = new HashSet<>();

    boolean visitField(Field f) {
        fields.add(f);
        return true;
    }
}
