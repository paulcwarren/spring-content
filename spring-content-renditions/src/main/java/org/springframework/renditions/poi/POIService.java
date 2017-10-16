package org.springframework.renditions.poi;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;

public interface POIService {

    XWPFDocument xwpfDocument(InputStream stream) throws IOException;

}
