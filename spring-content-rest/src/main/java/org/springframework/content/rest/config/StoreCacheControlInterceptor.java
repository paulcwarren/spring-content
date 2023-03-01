package org.springframework.content.rest.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.utils.StoreUtils;
import lombok.Getter;

public class StoreCacheControlInterceptor implements HandlerInterceptor {

    private Map<String, CacheControl> cacheControlMap = new HashMap<>();
    private List<CacheControlRule> cacheControlRules = new ArrayList<>();
    private URI baseUri;

    public StoreCacheControlInterceptor() {
    }

    public StoreCacheControlConfigurer configurer() {
        return new StoreCacheControlConfigurer(this);
    }

    public void addCacheControl(String mapping, CacheControl cacheControl) {
        cacheControlMap.put(mapping, cacheControl);
    }

    public void addCacheControlRule(CacheControlRule cacheControlRule) {
        cacheControlRules.add(cacheControlRule);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!"GET".equals(request.getMethod())) {
            return true;
        }

        UrlPathHelper pathHelper = UrlPathHelper.defaultInstance;

        String lookupPath = pathHelper.getLookupPathForRequest(request);
        String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, baseUri);

        CacheControl cacheControl = cacheControlMap.get(storeLookupPath);

        for (CacheControlRule rule : cacheControlRules) {

            if (rule.match(storeLookupPath)) {
                response.addHeader("Cache-Control", rule.getCacheControl().getHeaderValue());
            }
        }

        return true;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    @Getter
    static class CacheControlRule {

        private static AntPathMatcher matcher = new AntPathMatcher();

        private String pattern;
        private CacheControl cacheControl;

        public CacheControlRule(String pattern, CacheControl control) {
            this.pattern = pattern;
            this.cacheControl = control;
        }

        public boolean match(String path) {
            return matcher.match(pattern, path);
        }
    }

    public static class StoreCacheControlConfigurer {

        private final StoreCacheControlInterceptor interceptor;

        public StoreCacheControlConfigurer(StoreCacheControlInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        public StoreCacheControlConfigurer antMatcher(String pattern, CacheControl cacheControl) {
            interceptor.addCacheControlRule(new CacheControlRule(pattern, cacheControl));
            return this;
        }
    }
}
