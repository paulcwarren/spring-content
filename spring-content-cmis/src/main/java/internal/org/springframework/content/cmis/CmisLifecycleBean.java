package internal.org.springframework.content.cmis;

import java.util.HashMap;

import javax.servlet.ServletContext;

import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

public class CmisLifecycleBean implements ServletContextAware, InitializingBean, DisposableBean
{
	private ServletContext servletContext;
	private CmisServiceFactory factory;

	public CmisLifecycleBean(CmisServiceFactory cmisServiceFactory) {
		this.factory = cmisServiceFactory;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void afterPropertiesSet()
	{
		if (factory != null) {
			factory.init(new HashMap<>());
			servletContext.setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, factory);
		}
	}

	@Override
	public void destroy()
	{
		if (factory != null) {
			factory.destroy();
		}
	}
}