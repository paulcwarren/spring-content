package org.springframework.content.fs.boot;

import org.junit.runner.RunWith;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;

import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;

@RunWith(Ginkgo4jRunner.class)
public class FilesystemPropertiesTest {

	private FilesystemContentAutoConfiguration.FilesystemProperties props;

	{
		Describe("FilesystemProperties", () -> {
			Context("given a filesystem properties with no root set", () -> {
				BeforeEach(() -> {
					props = new FilesystemContentAutoConfiguration.FilesystemProperties();
				});
				It("should return a JAVA.IO.TMPDIR based default",() -> {
					assertThat(props.getFilesystemRoot(), startsWith(System.getProperty("java.io.tmpdir")));
				});
			});
			Context("given a filesystem properties with root set", () -> {
				BeforeEach(() -> {
					props = new FilesystemContentAutoConfiguration.FilesystemProperties();
					props.setFilesystemRoot("/some/random/path");
				});
				It("should return a JAVA.IO.TMPDIR based default",() -> {
					assertThat(props.getFilesystemRoot(), is("/some/random/path"));
				});
			});
		});
	}
}
