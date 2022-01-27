package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;

public class ClassWalker {

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

        Field[] fields = this.klazz.getDeclaredFields();
        for (Field field : fields) {
            fContinue &= visitor.visitField(field);
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd(klazz);
    }
}
