package internal.org.springframework.content.encryption.keys.converter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayToListConverterTest {

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();
    private static final TypeDescriptor STRING_LIST_TYPE = TypeDescriptor.collection(List.class,
            TypeDescriptor.valueOf(String.class));

    static {
        conversionService.addConverter(new ByteArrayToListConverter(conversionService));
        conversionService.addConverter(new ListToByteArrayConverter(conversionService));

        conversionService.addConverter(String.class, byte[].class, (s) -> s.getBytes(StandardCharsets.UTF_8));
        conversionService.addConverter(byte[].class, String.class, (b) -> new String(b, StandardCharsets.UTF_8));
    }

    @Test
    public void encodesAndDecodes() {
        var data = List.of("abc", "def");

        var encodedList = conversionService.convert(data, STRING_LIST_TYPE, TypeDescriptor.valueOf(byte[].class));

        var decodedList = conversionService.convert(encodedList, STRING_LIST_TYPE);

        assertThat(decodedList, is(equalTo(data)));
    }

    @Test
    public void encodesAndDecodesEmptyList() {
        var data = List.of();

        var encodedList = conversionService.convert(data, STRING_LIST_TYPE, TypeDescriptor.valueOf(byte[].class));

        var decodedList = conversionService.convert(encodedList, STRING_LIST_TYPE);

        assertThat(decodedList, is(equalTo(data)));
    }

}