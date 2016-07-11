package internal.org.springframework.content.commons.renditions;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.common.renditions.RenditionProvider;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.common.renditions.RenditionServiceImpl;

@RunWith(Ginkgo4jRunner.class)
public class RenditionServiceImplTest {

	private RenditionServiceImpl renditionService;
	
	private RenditionProvider mockProvider = null;
	{
		Describe("RenditionServiceImpl", () -> {
			Context("#canConvert", () -> {
				BeforeEach(() -> {
					renditionService = new RenditionServiceImpl();
				});
				Context("given no providers", () -> {
					It("should always return false", () -> {
						SecureRandom random = new SecureRandom();
						String from = new BigInteger(130, random).toString(32);
						String to = new BigInteger(130, random).toString(32);
						assertThat(renditionService.canConvert(from, to), is(false));
					});
				});
				Context("given a provider", () -> {
					BeforeEach(() -> {
						mockProvider = mock(RenditionProvider.class);
						when(mockProvider.consumes()).thenReturn("something");
						when(mockProvider.produces()).thenReturn(new String[]{"something/else"});
						renditionService.setProviders(mockProvider);
					});
					It("should return true for a consumes/produces match", () -> {
						assertThat(renditionService.canConvert("something", "something/else"), is(true));
					});
					It("should return false if only consumes matches", () -> {
						assertThat(renditionService.canConvert("something", "no-match-here"), is(false));
					});
					It("should return false if only produces matches", () -> {
						assertThat(renditionService.canConvert("no-match-here", "something/else"), is(false));
					});
				});
			});
			Context("#convert", () -> {
				BeforeEach(() -> {
					renditionService = new RenditionServiceImpl();
				});
				Context("given no providers", () -> {
					It("should always return null", () -> {
						assertThat(renditionService.convert("something", null, "something/else"), is(nullValue()));
					});
				});
				Context("given a provider", () -> {
					BeforeEach(() -> {
						mockProvider = mock(RenditionProvider.class);
						when(mockProvider.consumes()).thenReturn("something");
						when(mockProvider.produces()).thenReturn(new String[]{"something/else"});
						renditionService.setProviders(mockProvider);
					});
					It("should return true for a consumes/produces match", () -> {
						renditionService.convert("something", null, "something/else");
						verify(mockProvider).convert(anyObject(), eq("something/else"));
					});
					It("should return false if only consumes matches", () -> {
						renditionService.convert("something", null, "no-match-here");
						verify(mockProvider, never()).convert(anyObject(), eq("something/else"));
					});
					It("should return false if only produces matches", () -> {
						renditionService.convert("no-match-here", null, "something/else");
						verify(mockProvider, never()).convert(anyObject(), eq("something/else"));
					});
				});
			});
		});
	}
	
	@Test
	public void noop() {
	}
}
