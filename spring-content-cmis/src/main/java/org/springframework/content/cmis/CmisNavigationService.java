package org.springframework.content.cmis;

import java.util.List;

public interface CmisNavigationService<P> {

	/**
	 * Called by the CMIS Bridge when it needs to fulfil navigation requests.
	 *
	 * @param parent the parent, or null if the request is for the root of the repository
	 * @return children
	 */
	List getChildren(P parent);

}
