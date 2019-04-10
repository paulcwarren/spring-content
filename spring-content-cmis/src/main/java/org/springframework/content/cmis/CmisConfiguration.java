package org.springframework.content.cmis;

import internal.org.springframework.content.cmis.CmisLifecycleBean;
import internal.org.springframework.content.cmis.CmisRepositoryConfiguration;
import internal.org.springframework.content.cmis.CmisServiceBridge;
import internal.org.springframework.content.cmis.ContentCmisServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet;
import org.apache.chemistry.opencmis.server.shared.AbstractCmisHttpServlet;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CmisConfiguration {

	@Bean
	public CmisServiceFactory cmisServiceFactory(CmisRepositoryConfiguration config, CmisServiceBridge cmisServiceBridge) {
		return new ContentCmisServiceFactory(config, cmisServiceBridge);
	}

	@Bean
	public CmisServiceBridge cmisServiceBridge(CmisRepositoryConfiguration cmisRepositoryConfiguration) {
		return new CmisServiceBridge(cmisRepositoryConfiguration);
	}

	@Bean
	public CmisLifecycleBean cmisLifecycle(CmisRepositoryConfiguration cmisRepositoryConfiguration) {
		return new CmisLifecycleBean(cmisServiceFactory(cmisRepositoryConfiguration, cmisServiceBridge(cmisRepositoryConfiguration)));
	}

	@Bean
	public CmisBrowserBindingServlet cmisBrowserServlet() {
		CmisBrowserBindingServlet servlet = new CmisBrowserBindingServlet();
		return servlet;
	}

	@Bean
	public ServletRegistrationBean cmisBrowserServletRegistration() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean(cmisBrowserServlet(), "/browser/*");
		registrationBean.setName("cmisbrowser");
		registrationBean.getInitParameters().put(AbstractCmisHttpServlet.PARAM_CALL_CONTEXT_HANDLER,"org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler");
		return registrationBean;
	}

}
