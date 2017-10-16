package internal.org.springframework.renditions.poi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

public class POIServiceImpl implements org.springframework.renditions.poi.POIService {

    private static Log logger = LogFactory.getLog(POIServiceImpl.class);

    @Override
    public XWPFDocument xwpfDocument(InputStream stream) throws IOException {
        Assert.notNull(stream);
        return new XWPFDocument(stream);
    }
}
