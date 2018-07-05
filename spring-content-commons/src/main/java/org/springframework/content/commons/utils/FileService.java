package org.springframework.content.commons.utils;

import java.io.File;
import java.io.IOException;

public interface FileService {

	void mkdirs(File file) throws IOException;

	/**
	 * Removes directories.
	 *
	 * Starting at the leaf of {@link from} and working upwards removes directories if, and only if, empty
	 * until {@link to} is reached.

	 * @param from
	 * 			the directory path to be removed
	 * @param to
	 * 			the sub-directory to preserve.  Maybe null
	 */
	void rmdirs(File from, File to) throws IOException;
}
