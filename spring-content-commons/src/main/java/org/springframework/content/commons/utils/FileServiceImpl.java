package org.springframework.content.commons.utils;

import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;

public class FileServiceImpl implements FileService {
    public FileServiceImpl() {
    }

    @Override
    public void mkdirs(File file) throws IOException {
        Assert.notNull(file, "file must not be null");
        FileUtils.forceMkdir(file);
    }
}
