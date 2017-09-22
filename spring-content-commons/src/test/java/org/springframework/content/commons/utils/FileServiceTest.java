package org.springframework.content.commons.utils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.FileServiceImpl;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

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
                parent = new File(Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()).toAbsolutePath().toString());
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
