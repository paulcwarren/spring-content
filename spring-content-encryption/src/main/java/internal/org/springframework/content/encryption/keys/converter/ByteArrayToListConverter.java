package internal.org.springframework.content.encryption.keys.converter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

@RequiredArgsConstructor
public class ByteArrayToListConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if(sourceType.getObjectType() != byte[].class) {
            return false;
        }
        if(!targetType.isCollection()) {
            return false;
        }
        return conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(byte[].class, Collection.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        var list = new ArrayList<>();
        var bb = ByteBuffer.wrap((byte[])source);

        if(bb.getChar() != 'L') { // Marker check
            return null;
        }

        while(bb.hasRemaining()) {
            var itemSize = bb.getInt();
            var item = new byte[itemSize];
            bb.get(item);
            list.add(conversionService.convert(item, targetType.getElementTypeDescriptor()));
        }
        return list;
    }
}
