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

	@Override
	public void rmdirs(File from, File to) throws IOException {
		if (from.isFile()) {
			throw new IOException("Not a directory");
		}
		File dir = from;
		while (dir != null && dir.listFiles().length == 0 && dir.equals(to) == false) {
			File temp = dir.getParentFile();
			dir.delete();
			dir = temp;
		}
	}

	@Override
	public void rmdirs(File from) throws IOException {
		if (from.isFile()) {
			throw new IOException("Not a directory");
		}
		File dir = from;
		while (dir != null && dir.listFiles().length == 0) {
			File temp = dir.getParentFile();
			dir.delete();
			dir = temp;
		}
	}
}
