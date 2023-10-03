package internal.org.springframework.content.commons.store.factory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreExceptionTranslator;

import java.util.ArrayList;
import java.util.List;

public class StoreExceptionTranslatorInterceptor implements MethodInterceptor {
    private final BeanFactory beanFactory;
    private List<StoreExceptionTranslator> translators = null;

    public StoreExceptionTranslatorInterceptor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (RuntimeException re) {
            if (re instanceof StoreAccessException) {
                throw re;
            }
            if (translators == null) {
                translators = new ArrayList<>();
                beanFactory.getBeanProvider(StoreExceptionTranslator.class).orderedStream().forEach(translators::add);
            }
            StoreAccessException sae = null;
            for (int i=0; i < translators.size() && sae == null; i++) {
                sae = translators.get(i).translate(re);
            }
            throw sae != null ? sae : re;
        }
    }
}
