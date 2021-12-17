package internal.org.springframework.content.rest.mappingcontext;

import java.lang.reflect.Field;

public class ClassWalker {

    private Class<?> klazz;

    public ClassWalker(Class<?> klazz) {
        this.klazz = klazz;
    }

    public void accept(ClassVisitor visitor) {

        Field[] fields = this.klazz.getDeclaredFields();

        for (Field field : fields) {
            visitor.visitField(field);
        }
    }
}
