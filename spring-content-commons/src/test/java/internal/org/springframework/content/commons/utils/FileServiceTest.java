package internal.org.springframework.content.commons.utils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.FileServiceImpl;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class FileServiceTest {

    private FileService fileService;
    private File file;
    private File parent;
    private Exception ex;

    {
        Describe("mkdirs", () -> {
            BeforeEach(() -> {
                parent = new File(System.getProperty("java.io.tmpdir") + UUID.randomUUID());
            });

            JustBeforeEach(() -> {
                fileService = new FileServiceImpl();
                try {
                    fileService.mkdirs(file);
                } catch (Exception e) {
                    ex = e;
                }
            });

            Context("when passed in a file that exists", () -> {
                BeforeEach(() -> {
                   file = new File(parent, "something.txt");
                   FileUtils.touch(file);
                   assertThat(file.exists(), is(true));
                });
                AfterEach(() -> {
                    file.delete();
                });
                It("should throw an IOException", () -> {
                    assertThat(ex, is(not(nullValue())));
                    assertThat(ex, instanceOf(IOException.class));
                });
            });

            Context("when passed in a file that does not exist", () -> {
                BeforeEach(() -> {
                    file = new File(parent, "something.txt");
                    assertThat(file.exists(), is(false));
                });
                AfterEach(() -> {
                    file.delete();
                });

                It("should not throw an exception", () -> {
                   assertThat(ex, is(nullValue()));
                });

                It("should create the directory", () -> {
                    assertThat(file.isDirectory(), is(true));
                    assertThat(file.exists(), is(true));
                });
            });

            Context("when passed in a directory that exists", () -> {
                BeforeEach(() -> {
                    file = new File(parent, "something");
                    file.mkdirs();
                    assertThat(file.exists(), is(true));
                });
                AfterEach(() -> {
                    file.delete();
                });
                It("should succeed", () -> {
                    assertThat(ex, is(nullValue()));
                    assertThat(file.exists(), is(true));
                    assertThat(file.isDirectory(), is(true));
                });
            });

            Context("when passed in a directory that does not exist", () -> {
                BeforeEach(() -> {
                    file = new File(parent, "something");
                    assertThat(file.exists(), is(false));
                });
                AfterEach(() -> {
                    file.delete();
                });
                It("should succeed", () -> {
                    assertThat(ex, is(nullValue()));
                    assertThat(file.exists(), is(true));
                    assertThat(file.isDirectory(), is(true));
                });

            });

            Context("when passed null", () -> {
                BeforeEach(() -> {
                    file = null;
                });
                It("should throw an IllegalArgumentException", () -> {
                    assertThat(ex, is(not(nullValue())));
                    assertThat(ex, instanceOf(IllegalArgumentException.class));
                });
            });
        });
    }


}
