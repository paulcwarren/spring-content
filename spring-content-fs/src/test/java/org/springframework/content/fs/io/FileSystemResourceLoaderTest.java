package org.springframework.content.fs.io;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class FileSystemResourceLoaderTest {

	private FileSystemResourceLoader loader = null;

	private String path;

	private String location;
	
	private File parent;
	private File file;
	
	private Exception ex;
	
	{
		Describe("FileSystemResourceLoader", () -> {
			Context("#FileSystemResourceLoader", () -> {
			    JustBeforeEach(() -> {
			        try {
			            loader = new FileSystemResourceLoader(path);
                    } catch (Exception e) {
			            ex = e;
                    }
                });
                Context("given well formed path (has a trailing slash)", () -> {
                    BeforeEach(() -> {
                        path = "/some/well-formed/path/";
                    });
                    It("succeeds", () -> {
                        assertThat(ex, is(nullValue()));
                        assertThat(loader.getResource("/something").getFile().getPath(), is("/some/well-formed/path/something"));
                        assertThat(loader.getResource("/something"), instanceOf(DeletableResource.class));
                    });
                });

                Context("given malformed path without a trailing slash)", () -> {
                    BeforeEach(() -> {
                        path = "/some/malformed/path";
                    });
                    It("succeeds", () -> {
                        assertThat(ex, is(nullValue()));
                        assertThat(loader.getResource("/something").getFile().getPath(), is("/some/malformed/path/something"));
                        assertThat(loader.getResource("/something"), instanceOf(DeletableResource.class));
                    });
                });
			});
		});

		Describe("DeletableResource", () -> {
			Context("#delete", () -> {
				BeforeEach(() -> {
					parent = new File(Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()).toAbsolutePath().toString());
				});
				JustBeforeEach(() -> {
					loader = new FileSystemResourceLoader(parent.getPath() + "/");
					Resource resource = loader.getResource(location);
					assertThat(resource, instanceOf(DeletableResource.class));
					((DeletableResource)resource).delete();
				});
				Context("given a file resource that exists", () -> {
					BeforeEach(() -> {
  						location = "FileSystemResourceLoaderTest.tmp";
  						file = new File(parent, location);
 						FileUtils.touch(file);
 						assertThat(file.exists(), is(true));
					});
					It("should delete the underlying file", () -> {
						assertThat(file.exists(), is(false));
					});
				});
			});
		});
	}
}
