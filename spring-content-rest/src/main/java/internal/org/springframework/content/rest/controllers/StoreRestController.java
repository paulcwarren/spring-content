package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.controllers.ContentService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.HeaderUtils;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

@ContentRestController
public class StoreRestController implements InitializingBean  {

    private static final Logger logger = LoggerFactory.getLogger(StoreRestController.class);

    private static final String STORE_REQUEST_MAPPING = "/{store}/**";

    @Autowired
    ApplicationContext context;
    @Autowired(required=false)
    private Repositories repositories;
    @Autowired
    private Stores stores;
    @Autowired(required=false)
    private RepositoryInvokerFactory repoInvokerFactory;
    @Autowired(required=false)
    private PlatformTransactionManager ptm;

    public StoreRestController() {
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.GET)
    public void getContent(HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader HttpHeaders headers,
            @RequestHeader(value = "Accept", required = false) String requestedMimeTypes,
            Resource resource,
            MediaType resourceType,
            Object resourceETag,
            ContentService contentService)
                    throws MethodNotAllowedException {

        if (resource == null || resource.exists() == false) {
            throw new ResourceNotFoundException();
        }

        long lastModified = -1;
        try {
            lastModified = resource.lastModified();
        } catch (IOException e) {}
        if(new ServletWebRequest(request, response).checkNotModified(resourceETag != null ? resourceETag.toString() : null, lastModified)) {
            return;
        }

        contentService.getContent(request, response, headers, requestedMimeTypes, resource, resourceType);
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = {
            "content-type!=multipart/form-data", "accept!=application/hal+json" })
    @ResponseBody
    public void putContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            Resource resource,
            Object resourceETag,
            ContentService contentService)
                    throws IOException, MethodNotAllowedException {

        handleMultipart(response,
                headers,
                contentService,
                resource,
                resourceETag != null ? resourceETag.toString() : null,
                        request.getInputStream(),
                        headers.getContentType(),
                        null);
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
    @ResponseBody
    public void putMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            @RequestParam("file") MultipartFile multiPart,
            Resource resource,
            Object resourceETag,
            ContentService contentService)
                    throws IOException, MethodNotAllowedException {

        handleMultipart(response,
                headers,
                contentService,
                resource,
                resourceETag != null ? resourceETag.toString() : null,
                        multiPart.getInputStream(),
                        MediaType.parseMediaType(multiPart.getContentType()),
                        multiPart.getOriginalFilename());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @ResponseBody
    public void postMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            @RequestParam("file") MultipartFile multiPart,
            Resource resource,
            Object resourceETag,
            ContentService contentService)
                    throws IOException, MethodNotAllowedException {

        handleMultipart(response,
                headers,
                contentService,
                resource,
                resourceETag != null ? resourceETag.toString() : null,
                        multiPart.getInputStream(),
                        MediaType.parseMediaType(multiPart.getContentType()),
                        multiPart.getOriginalFilename());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = {"content-type!=multipart/form-data"})
    @ResponseBody
    public void postContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            Resource resource,
            Object resourceETag,
            ContentService contentService)
                    throws IOException, MethodNotAllowedException {

        handleMultipart(response,
                headers,
                contentService,
                resource,
                resourceETag != null ? resourceETag.toString() : null,
                        request.getInputStream(),
                        headers.getContentType(),
                        null);
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.DELETE, headers = "accept!=application/hal+json")
    public void deleteContent(@RequestHeader HttpHeaders headers, HttpServletResponse response,
            Resource resource,
            Object resourceETag,
            ContentService contentService)
                    throws IOException, MethodNotAllowedException {

        if (!resource.exists()) {
            throw new ResourceNotFoundException();
        } else {
            HeaderUtils.evaluateHeaderConditions(headers, resourceETag != null ? resourceETag.toString() : null, new Date(resource.lastModified()));
        }

        contentService.unsetContent(resource);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void handleMultipart(HttpServletResponse response,
            HttpHeaders headers,
            ContentService contentService,
            Resource resource,
            Object resourceETag,
            InputStream content,
            MediaType mimeType,
            String originalFilename)
                    throws IOException, MethodNotAllowedException {

        boolean isNew = false;

        if (resource.exists()) {
            HeaderUtils.evaluateHeaderConditions(headers, resourceETag != null ? resourceETag.toString() : null, new Date(resource.lastModified()));
        } else {
            isNew = true;
        }

        contentService.setContent(headers, content, mimeType, originalFilename, resource);

        if (isNew) {
            response.setStatus(HttpStatus.CREATED.value());
        }
        else {
            response.setStatus(HttpStatus.OK.value());
        }
    }

    public static Object save(Repositories repositories, Object domainObj)
            throws HttpRequestMethodNotSupportedException {

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories,
                domainObj.getClass());

        if (ri == null) {
            throw new ResourceNotFoundException();
        }

        Class<?> domainObjClazz = ri.getDomainType();

        if (domainObjClazz != null) {
            Optional<Method> saveMethod = ri.getCrudMethods().getSaveMethod();
            if (!saveMethod.isPresent()) {
                throw new HttpRequestMethodNotSupportedException("save");
            }
            domainObj = ReflectionUtils.invokeMethod(saveMethod.get(),
                    repositories.getRepositoryFor(domainObjClazz).get(), domainObj);
        }

        return domainObj;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.repositories = context.getBean(Repositories.class);
        }
        catch (BeansException be) {
            this.repositories = new Repositories(context);
        }
        if (this.repoInvokerFactory == null) {
            this.repoInvokerFactory = new DefaultRepositoryInvokerFactory(this.repositories);
        }
    }
}