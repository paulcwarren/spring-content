package internal.org.springframework.renditions.poi;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class POIServiceTest {

    private POIService poi;

    private InputStream stream;

    {
        Describe("POIService", () -> {
            BeforeEach(() -> {
                poi = new POIServiceImpl();
            });
            Context("#xwpfDocument", () -> {
                Context("given an input stream", () -> {
                    BeforeEach(()-> {
                        stream = this.getClass().getResourceAsStream("/sample-docx.docx");
                    });
                    It("should return an instance of an XPWFDocument", () -> {
                        assertThat(poi.xwpfDocument(stream), is(not(nullValue())));
                    });
                });
                Context("given a null inputstream", () -> {
                   It("should throw an exception", () -> {
                       try {
                           poi.xwpfDocument(stream);
                           fail("no exception thrown");
                       } catch (Exception e) {
                           assertThat(e, is(not(nullValue())));
                           assertThat(e, is(instanceOf(IllegalArgumentException.class)));
                       }
                   });
                });
                Context("given an invalid inputstream", () -> {
                    BeforeEach(() -> {
                        stream = new ByteArrayInputStream("".getBytes());
                    });
                    It("should throw an exception", () -> {
                        try {
                            poi.xwpfDocument(stream);
                            fail("no exception thrown");
                        } catch (Exception e) {
                            assertThat(e, is(not(nullValue())));
                            assertThat(e, is(instanceOf(NotOfficeXmlFileException.class)));
                        }
                    });
                });
            });
        });
    }
}
