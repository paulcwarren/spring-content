package internal.org.springframework.content.encryption.keys.converter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ByteBufferCodec<D, B> {
    private static final Map<Class<?>, BytesConverter<?>> CONVERTERS = Map.of(
            byte[].class, new BytesConverter<>(
                    b -> b.length,
                    ByteBuffer::put,
                    (bb, len) -> {
                        byte[] data = new byte[len];
                        bb.get(data);
                        return data;
                    }
            ),
            String.class, new BytesConverter<>(
                    String::length,
                    (bb, s) -> {
                        bb.put(s.getBytes(StandardCharsets.UTF_8));
                    },
                    (bb, len) -> {
                        byte[] data = new byte[len];
                        bb.get(data);
                        return new String(data, StandardCharsets.UTF_8);
                    }
            )
    );

    @RequiredArgsConstructor
    private static class BytesConverter<T> {
        private final Function<T, Integer> length;
        private final BiConsumer<ByteBuffer, T> write;
        private final BiFunction<ByteBuffer, Integer, T> read;

        public int length(T value) {
            return this.length.apply(value);
        }

        public T read(ByteBuffer bb) {
            var len = bb.getInt();
            var startPos = bb.position();
            var result = read.apply(bb, len);
            var endPos = bb.position();
            var readBytes = endPos - startPos;
            if(len != readBytes) {
                throw new IllegalArgumentException("Read function did not read declared number of bytes (declared %d; read %d)".formatted(len, readBytes));
            }
            return result;
        }

        public void write(ByteBuffer bb, T value) {
            var len = length.apply(value);
            bb.putInt(len);
            var startPos = bb.position();
            write.accept(bb, value);
            var endPos = bb.position();
            var writtenBytes = endPos - startPos;

            if(writtenBytes != len) {
                throw new IllegalStateException("Write function did not write requested number of bytes (requested %d; written %d)".formatted(len, writtenBytes));
            }
        }
    }

    private static BytesConverter<Object> getConverter(Field<?, ?, ?> field) {
        var converter = CONVERTERS.get(field.type());
        if(converter == null) {
            throw new IllegalArgumentException("Can not convert "+field.type());
        }
        return (BytesConverter<Object>) converter;
    }

    private final char marker;

    private final List<Field<D, B, ?>> fields;

    private final Supplier<B> builderCreator;
    private final Function<B, D> builderFinalizer;

    public record Field<D, B, T>(
            Class<T> type,
            Function<D, T> getter,
            BiFunction<B, T, B> builderSetter
    ) {
        T get(D data) {
            return getter.apply(data);
        }

        B with(B builder, T item) {
            return builderSetter.apply(builder, item);
        }
    }

    public byte[] encode(D object) {
        var fieldLength = fields.stream()
                .mapToInt(field -> getConverter(field).length(field.get(object)) + Integer.BYTES)
                .sum();

        var bb = ByteBuffer.allocate(Character.BYTES+Integer.BYTES+fieldLength);
        bb.putChar(marker);
        bb.putInt(fields.size());
        for(var field : fields) {
            var converter = getConverter(field);
            converter.write(bb, field.get(object));
        }

        return bb.array();
    }

    public D decode(byte[] data) {
        var bb = ByteBuffer.wrap(data);
        var foundMarker = bb.getChar();

        if(foundMarker != marker) {
            return null;
        }

        var fieldLength = bb.getInt();
        var builder = builderCreator.get();
        for(int i = 0; i < fieldLength; i++) {
            var field = (Field<D, B, Object>) fields.get(i);
            var converter = getConverter(field);
            builder = field.with(builder, converter.read(bb));
        }

        return builderFinalizer.apply(builder);
    }

}
