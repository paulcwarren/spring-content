package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClassWalker {

    private static final Log LOGGER = LogFactory.getLog(ClassWalker.class);

    private Class<?> klazz;

    public ClassWalker(Class<?> klazz) {
        this.klazz = klazz;
    }

    public void accept(ContentPropertyBuilderVisitor visitor) {

        boolean fContinue = true;

        fContinue &= visitor.visitClass(klazz);
        if (!fContinue) {
            return;
        }

        List<Field>fields = new ArrayList<>();
        fields = getAllFields(fields, klazz);
        for (Field field : fields) {
            fContinue &= visitor.visitField(field);
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd(klazz);
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }
}
