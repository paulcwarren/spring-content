package org.springframework.versions;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;

public interface LockingAndVersioningProxyFactory {

    void apply(ProxyFactory result/*, BeanFactory beanFactory*/);

}
