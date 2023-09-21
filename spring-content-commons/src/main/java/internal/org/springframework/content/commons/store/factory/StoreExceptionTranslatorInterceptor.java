package internal.org.springframework.content.commons.store.factory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.content.commons.store.StoreAccessException;

public class StoreExceptionTranslatorInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (RuntimeException re) {
            if (re instanceof StoreAccessException) {
                throw re;
            }
            throw new StoreAccessException(re.getMessage(), re);
        }
    }
}
