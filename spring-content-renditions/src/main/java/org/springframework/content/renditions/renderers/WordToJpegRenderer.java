package org.springframework.content.renditions.renderers;

import internal.org.springframework.renditions.poi.POIServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.repository.StoreExtensionException;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.POIService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

@Service
public class WordToJpegRenderer implements RenditionProvider {

    private static Log logger = LogFactory.getLog(WordToJpegRenderer.class);

    private POIService poi = null;

    public WordToJpegRenderer() {
        poi = new POIServiceImpl();
    };

    public WordToJpegRenderer(POIService poi) {
        this.poi = poi;
    }

    @Override
    public String consumes() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    @Override
    public String[] produces() {
        return new String[] {"image/jpg"};
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream convert(InputStream fromInputSource, String toMimeType) {

        Assert.notNull(fromInputSource, "input source must not be null");

        XWPFDocument wordDoc = null;
        try {
            wordDoc = poi.xwpfDocument(fromInputSource);
        } catch (Exception e) {
            throw new RenditionException(String.format("Unexpected error reading input attempting to get mime-type rendition %s", toMimeType), e);
        }

        if (wordDoc != null) {
            try {
                POIXMLProperties props = wordDoc.getProperties();
                return props.getThumbnailImage();
            } catch (Exception e) {
                throw new RenditionException(String.format("Unexpected error getting thumbnail for mime-type rendition %s", toMimeType), e);
            }
        }
        return null;
    }
}
