package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.contentservice.ContentService;
import internal.org.springframework.content.rest.contentservice.ContentServiceFactory;
import internal.org.springframework.content.rest.io.InputStreamResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.HeaderUtils;

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

    @Autowired
    private RestConfiguration config;

    @Autowired
    private StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    @Autowired
    private MappingContext mappingContext;

    private ContentServiceFactory contentServiceFactory;

    public StoreRestController() {
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.GET)
    public void getContent(HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader HttpHeaders headers,
            Resource resource)
                    throws MethodNotAllowedException {

        if (resource == null || resource.exists() == false) {
            throw new ResourceNotFoundException();
        }

        StoreResource storeResource = (StoreResource)resource;

        long lastModified = -1;
        try {
            lastModified = storeResource.lastModified();
        } catch (IOException e) {}
        if(new ServletWebRequest(request, response).checkNotModified(storeResource.getETag() != null ? storeResource.getETag().toString() : null, lastModified)) {
            return;
        }

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        contentService.getContent(request, response, headers, storeResource, storeResource.getMimeType());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = {
            "content-type!=multipart/form-data", "accept!=application/hal+json" })
    @ResponseBody
    public void putContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            Resource resource)
                    throws IOException, MethodNotAllowedException {

        StoreResource storeResource = (StoreResource)resource;

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
                contentService,
                new InputStreamResource(request.getInputStream(), null),
                headers.getContentType(),
                storeResource,
                storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = "content-type=multipart/form-data")
    @ResponseBody
    public void putMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            @RequestParam("file") MultipartFile multiPart,
            StoreResource resource)
                    throws IOException, MethodNotAllowedException {

        StoreResource storeResource = resource;

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
                contentService,
                multiPart.getResource(),
                MediaType.parseMediaType(multiPart.getContentType()),
                storeResource,
                storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @ResponseBody
    public void postMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            @RequestParam("file") MultipartFile multiPart,
            Resource resource)
                    throws IOException, MethodNotAllowedException {

        StoreResource storeResource = (StoreResource)resource;

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
                contentService,
                new InputStreamResource(multiPart.getInputStream(), multiPart.getOriginalFilename()),
                MediaType.parseMediaType(multiPart.getContentType()),
                storeResource,
                storeResource.getETag());

    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = {"content-type!=multipart/form-data"})
    @ResponseBody
    public void postContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
            Resource resource)
                    throws IOException, MethodNotAllowedException {

        StoreResource storeResource = (StoreResource)resource;

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
                contentService,
                new InputStreamResource(request.getInputStream(), null),
                headers.getContentType(),
                storeResource,
                storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.DELETE, headers = "accept!=application/hal+json")
    public void deleteContent(@RequestHeader HttpHeaders headers, HttpServletResponse response,
            Resource resource)
                    throws IOException, MethodNotAllowedException {

        StoreResource storeResource = (StoreResource)resource;

        if (!resource.exists()) {
            throw new ResourceNotFoundException();
        } else {
            HeaderUtils.evaluateHeaderConditions(headers, storeResource.getETag() != null ? storeResource.getETag().toString() : null, new Date(storeResource.lastModified()));
        }

        ContentService contentService = contentServiceFactory.getContentService(storeResource);
        contentService.unsetContent(resource);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void handleMultipart(HttpServletRequest request, HttpServletResponse response,
            HttpHeaders headers,
            ContentService contentService,
            Resource source,
            MediaType sourceMimeType,
            StoreResource target,
            Object targetETag)
                    throws IOException, MethodNotAllowedException {

        boolean isNew = false;

        if (target.exists()) {
            HeaderUtils.evaluateHeaderConditions(headers, targetETag != null ? targetETag.toString() : null, new Date(target.lastModified()));
        } else {
            isNew = true;
        }

        contentService.setContent(request, response, headers, source, sourceMimeType, target);

        if (isNew) {
            response.setStatus(HttpStatus.CREATED.value());
        }
        else {
            response.setStatus(HttpStatus.OK.value());
        }
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

        contentServiceFactory = new ContentServiceFactory(config, repositories, repoInvokerFactory, stores, mappingContext, byteRangeRestRequestHandler);
    }
    }