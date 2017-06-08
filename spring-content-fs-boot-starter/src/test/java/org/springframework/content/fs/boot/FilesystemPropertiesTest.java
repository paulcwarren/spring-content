package org.springframework.content.fs.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

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
