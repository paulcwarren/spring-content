package internal.org.springframework.content.fs.operations;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FContext;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.config.FilesystemProperties;
import internal.org.springframework.content.fs.repository.ContextFileSystemResourceLoader;

@RunWith(Ginkgo4jRunner.class)
public class FileResourceTemplateTest {

	private FileResourceTemplate template;
	private FilesystemProperties props;
	private ContextFileSystemResourceLoader loader;
	
	private File location;
	private String resourcePath;
	private String resourcePathHome;
	
	private Resource result;
	private Exception resultEx;
	
	{
		Describe("FileResourceTemplate", () -> {
			
			FContext("check our understanding of file and fileoutputstream", () -> {
				It("this one fails", () -> {
					OutputStream stream = new FileOutputStream(new File(System.getProperty("java.io.tmpdir") + UUID.randomUUID() + "/some/deep/folder/structure/test.txt"));
					stream.write("this is a test".getBytes());
					stream.flush();
					stream.close();
				});
				It("but I reckon this one will work", () -> {
					File file = new File(System.getProperty("java.io.tmpdir") + UUID.randomUUID() + "/some/deep/folder/structure/test.txt");
					file.getParentFile().mkdirs();
					OutputStream stream = new FileOutputStream(file);
					stream.write("this is a test".getBytes());
					stream.flush();
					stream.close();
				});
			});

			
			
			Context("#create", () -> {
				BeforeEach(() -> {
					resourcePathHome = System.getProperty("java.io.tmpdir") + UUID.randomUUID();
					props = new FilesystemProperties();
					props.setFilesystemRoot(resourcePathHome);
					loader = new ContextFileSystemResourceLoader(props.getFilesystemRoot());
					template = new FileResourceTemplate(loader);
				});
				
				JustBeforeEach(() -> {
					try {
						result = template.create(resourcePath);
					} catch (Exception e) {
						this.resultEx = e;
					}
				});
				
				AfterEach(() -> {
					try {
						File toDelete = new File(resourcePathHome);
						FileUtils.deleteDirectory(toDelete);
					} catch (Exception e) {
						// best effort...dont care...
					}
				});

				Context("given a null location", () -> {
					BeforeEach(() -> {
						location = null;
					});
					It("should throw an exception", () -> {
						assertThat(resultEx, is(not(nullValue())));
					});
				});
				
				Context("given a deep folder hierarchy that doesn't exist", () -> {
					BeforeEach(() -> {
						resourcePath = "/some/deep/file/path";
						location = new File(resourcePathHome, resourcePath);
						assertThat(location.exists(), is(false));
					});
					It("should create the folder hierachy", () -> {
						assertThat(location.getParentFile().exists(), is(true));
						assertThat(location.getParentFile().isDirectory(), is(true));
					});
					It("should return a resource", () -> {
						assertThat(result, is(not(nullValue())));
						assertThat(result.exists(), is(false));
					});
				});

				Context("given a deep folder hierarchy that already exists", () -> {
					BeforeEach(() -> {
						resourcePath = "/some/other/deep/file/path";
						location = new File(resourcePathHome, resourcePath);
						File parent = location.getParentFile();
						parent.mkdirs();

						assertThat(parent.exists(), is(true));
						assertThat(location.exists(), is(false));
					});
					It("should create the folder hierachy", () -> {
						assertThat(location.getParentFile().exists(), is(true));
						assertThat(location.getParentFile().isDirectory(), is(true));
					});
					It("should return a resource", () -> {
						assertThat(result, is(not(nullValue())));
						assertThat(result.exists(), is(false));
					});
				});

				Context("given a deep folder hierarchy partially exists", () -> {
					BeforeEach(() -> {
						resourcePath = "/some/mega/deep/file/path";
						location = new File(resourcePathHome, resourcePath);
						File parent = location.getParentFile();
						File grandParent = parent.getParentFile();
						grandParent.mkdirs();

						assertThat(grandParent.exists(), is(true));
						assertThat(parent.exists(), is(false));
						assertThat(location.exists(), is(false));
					});
					It("should create the folder hierachy", () -> {
						assertThat(location.getParentFile().exists(), is(true));
						assertThat(location.getParentFile().isDirectory(), is(true));
					});
					It("should return a resource", () -> {
						assertThat(result, is(not(nullValue())));
						assertThat(result.exists(), is(false));
					});
				});
			});
		});
	}
}
