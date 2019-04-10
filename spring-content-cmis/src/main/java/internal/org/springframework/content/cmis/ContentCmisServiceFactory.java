package internal.org.springframework.content.cmis;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;

public class ContentCmisServiceFactory extends AbstractServiceFactory {

	private ThreadLocal<CallContextAwareCmisService> threadLocalService = new ThreadLocal<CallContextAwareCmisService>();

	private final CmisRepositoryConfiguration config;
	private final CmisServiceBridge bridge;

	public ContentCmisServiceFactory(CmisRepositoryConfiguration config, CmisServiceBridge bridge) {
		this.config = config;
		this.bridge = bridge;
	}

	@Override
	public CmisService getService(CallContext context) {
		CallContextAwareCmisService service = threadLocalService.get();
		if (service == null) {
			service = new ContentCmisService(config, bridge);
			threadLocalService.set(service);
		}

		service.setCallContext(context);

		return service;
	}
}
