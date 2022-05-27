package org.springframework.content.commons.mappingcontext;

import lombok.*;
import org.springframework.content.commons.property.PropertyPath;

import java.io.Serializable;

@Getter
@RequiredArgsConstructor(staticName="of")
@EqualsAndHashCode
//public class ContentPropertyInfo<S> {
public class ContentPropertyInfo<S, SID extends Serializable> {
    private final S entity;
//    private Object contentId;
    private final SID contentId;

    // may be also useful for conversion:
    private final PropertyPath propertyPath;
    private final ContentProperty contentProperty;

}
