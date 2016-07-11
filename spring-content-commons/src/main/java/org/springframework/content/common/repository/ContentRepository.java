package org.springframework.content.common.repository;

import java.io.Serializable;

/**
 * Marker interface.  All content repositories interfaces must ultimately implement this marker interface.
 * 
 * @author warrep
 */
public interface ContentRepository<T, ID extends Serializable>  {

}
