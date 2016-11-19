package internal.org.springframework.content.commons.repository.factory;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;

public class ContentRepositoryMethodInteceptor implements MethodInterceptor {

	private RenditionService renditions;
	
	private Method getRenditionMethod; 
	private Method convertMethod;
	private Method getContentMethod;
	
	public ContentRepositoryMethodInteceptor(RenditionService renditions) {
		this.renditions = renditions;
		
		Class<?> clazz  = Renderable.class;
		try {
			getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
			Class<?> storeClazz  = ContentStore.class;
			getContentMethod = storeClazz.getMethod("getContent", Object.class);
			convertMethod = renditions.getClass().getMethod("convert", String.class, InputStream.class, String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (invocation.getMethod().equals(getRenditionMethod)) {
			try {
				String fromMimeType = null;
				if (BeanUtils.hasFieldWithAnnotation(invocation.getArguments()[0], MimeType.class)) {
					fromMimeType = (String)BeanUtils.getFieldWithAnnotation(invocation.getArguments()[0], MimeType.class);
				}
				String toMimeType = (String) invocation.getArguments()[1];
				
				if (renditions.canConvert(fromMimeType, toMimeType)) {
					InputStream content = (InputStream) getContentMethod.invoke(invocation.getThis(), invocation.getArguments()[0]);
					InputStream rendition = (InputStream) convertMethod.invoke(renditions, fromMimeType, content, toMimeType);
					return rendition;
				} else {
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
		return invocation.proceed();
	}
}
