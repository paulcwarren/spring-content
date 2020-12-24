package org.springframework.content.commons.utils;

import java.io.File;
import java.io.IOException;

public interface FileService {

	void mkdirs(File file) throws IOException;

	/**
	 * Removes directories.
	 *
	 * Starting at the leaf of {@code from} and working upwards removes directories if, and only if, empty
	 * until {@code to} is reached.

	 * @param from
	 * 			the directory path to be removed
	 * @param to
	 * 			the sub-directory to preserve.  Maybe null
	 * @throws IOException
	 *          when the directories cannot be removed
	 */
	void rmdirs(File from, File to) throws IOException;

	/**
	 * Removes directories.
	 *
	 * Starting at the leaf of {@code from} and working upwards removes directories if, and only if, empty.

	 * @param from
	 * 			the directory path to be removed
	 * @throws IOException
     *          when the directories cannot be removed
	 */
	void rmdirs(File from) throws IOException;
}
