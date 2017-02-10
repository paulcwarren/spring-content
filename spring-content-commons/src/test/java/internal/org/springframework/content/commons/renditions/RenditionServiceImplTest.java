package internal.org.springframework.content.commons.renditions;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Set;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.repository.ContentRepositoryInvoker;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class RenditionServiceImplTest {

	private RenditionServiceImpl renditionService;
	private Object rc;
	
	// mocks
	private MethodInvocation invocation;
	private ContentRepositoryInvoker repoInvoker;
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
		Describe("ContentRepositoryExtension", () -> {
			BeforeEach(() -> {
				renditionService = new RenditionServiceImpl();
			});
			Context("#getMethods", () -> {
				It("should return a set containing the getRendition method", () -> {
					Class<?> clazz  = Renderable.class;
					Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
					
					Set<Method> methods = renditionService.getMethods();
					assertThat(methods.size(), is(1));
					assertThat(methods, hasItem(getRenditionMethod));
				});
			});
			
			Context("#invoke", () -> {
				JustBeforeEach(() -> {
					rc = renditionService.invoke(invocation, repoInvoker);
				});
				Context("given a provider", ()->{
					BeforeEach(() -> {
						mockProvider = mock(RenditionProvider.class);
						when(mockProvider.consumes()).thenReturn("something");
						when(mockProvider.produces()).thenReturn(new String[]{"something/else"});
						renditionService.setProviders(mockProvider);
					});
					
					Context("given a renderable invocation", () -> {
						BeforeEach(() -> {
							Class<?> clazz  = Renderable.class;
							Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
							
							invocation = mock(MethodInvocation.class);
							when(invocation.getMethod()).thenReturn(getRenditionMethod);
							when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("something"), "something/else"});
							
							repoInvoker = mock(ContentRepositoryInvoker.class);
							when(repoInvoker.invokeGetContent()).thenReturn(new ByteArrayInputStream("some content".getBytes()));
						});
						
						It("should convert the content", () -> {
							verify(mockProvider).convert(anyObject(), eq("something/else"));
						});
					});
					
					Context("given a ContentObject with no mime-type", () -> {
						BeforeEach(() -> {
							Class<?> clazz  = Renderable.class;
							Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
							
							invocation = mock(MethodInvocation.class);
							when(invocation.getMethod()).thenReturn(getRenditionMethod);
							when(invocation.getArguments()).thenReturn(new Object[] {new NoMimeTypeContentObject(), "something/else"});
						});
						
						It("should not convert the content and return null", () -> {
							verify(mockProvider, never()).convert(anyObject(), anyString());
							assertThat(rc, is(nullValue()));
						});
					});

					Context("given an unsupported 'from' mime-type", () -> {
						BeforeEach(() -> {
							Class<?> clazz  = Renderable.class;
							Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
							
							invocation = mock(MethodInvocation.class);
							when(invocation.getMethod()).thenReturn(getRenditionMethod);
							when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("somehting/bad"), "something/else"});
						});
						
						It("should not convert the content and return null", () -> {
							verify(mockProvider, never()).convert(anyObject(), anyString());
							assertThat(rc, is(nullValue()));
						});
					});

					Context("given an unsupported 'to' mime-type", () -> {
						BeforeEach(() -> {
							Class<?> clazz  = Renderable.class;
							Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
							
							invocation = mock(MethodInvocation.class);
							when(invocation.getMethod()).thenReturn(getRenditionMethod);
							when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("somehting"), "something/bad"});
						});
						
						It("should not convert the content and return null", () -> {
							verify(mockProvider, never()).convert(anyObject(), anyString());
							assertThat(rc, is(nullValue()));
						});
					});
				});
			});
		});
	}
	
	@Test
	public void noop() {
	}
	
	public static class ContentObject {
		@MimeType
		public String mimeType;
		
		public ContentObject(String mimeType) {
			this.mimeType = mimeType; 
		}
	}

	public static class NoMimeTypeContentObject {
	}
}
