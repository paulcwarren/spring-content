package org.springframework.versions;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;

public interface LockingAndVersioningService {

    void enableLockingAndVersioning(ProxyFactory result, BeanFactory beanFactory);

}
