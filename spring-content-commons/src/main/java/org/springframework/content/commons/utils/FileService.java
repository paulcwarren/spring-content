package org.springframework.content.commons.utils;

import java.io.File;
import java.io.IOException;

public interface FileService {
    void mkdirs(File file) throws IOException;
}
