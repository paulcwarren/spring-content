package internal.org.springframework.content.encryption.keys.converter;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

@RequiredArgsConstructor
public class ListToByteArrayConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if(targetType.getObjectType() != byte[].class) {
            return false;
        }
        if(!sourceType.isCollection()) {
            return false;
        }
        return conversionService.canConvert(sourceType.getElementTypeDescriptor(), targetType);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(Collection.class, byte[].class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        List<byte[]> converted = ((List)source).stream()
                .map(item -> conversionService.convert(item, sourceType.getElementTypeDescriptor(), targetType))
                .toList();
        var convertedTotalSize = converted.stream().mapToInt(b -> b.length).sum();

        var bb = ByteBuffer.allocate(Character.BYTES+converted.size()*Integer.BYTES + convertedTotalSize);
        bb.putChar('L'); // Marker
        for (var item : converted) {
            bb.putInt(item.length);
            bb.put(item);
        }

        return bb.array();
    }
}
