package org.springframework.content.rest.boot;

import java.net.URI;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;
import org.junit.runner.RunWith;

import org.springframework.content.rest.config.RestConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class SpringBootContentRestConfigurerTest {

    private SpringBootContentRestConfigurer configurer;

    private ContentRestProperties properties;

    // mocks
    private RestConfiguration restConfig;

    {
        Describe("SpringBootContentRestConfigurer", () -> {

            Context("#configure", () -> {

                BeforeEach(() -> {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                });

                JustBeforeEach(() -> {
                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);
                });

                Context("given a base uri property", () -> {

                    BeforeEach(() -> {
                        properties.setBaseUri(URI.create("/test"));
                    });

                    It("should set the property on the RestConfiguration", () -> {
                        verify(restConfig).setBaseUri(eq(properties.getBaseUri()));
                    });
                });

                Context("given a fullyQualifiedLinks property setting", () -> {

                    BeforeEach(() -> {
                        properties.setFullyQualifiedLinks(true);
                    });

                    It("should set the property on the RestConfiguration", () -> {
                        verify(restConfig).setFullyQualifiedLinks(eq(true));
                    });
                });

                Context("given a null base uri property", () -> {

                    It("should not set the property on the RestConfiguration", () -> {
                        verify(restConfig, never()).setBaseUri(anyObject());
                    });
                });

                Context("given a null properties", () -> {

                    BeforeEach(() -> {
                        properties = null;
                    });

                    It("should not set the property on the RestConfiguration", () -> {
                        verify(restConfig, never()).setBaseUri(anyObject());
                        verify(restConfig, never()).setFullyQualifiedLinks(anyBoolean());
                    });
                });
            });
        });
    }
}
