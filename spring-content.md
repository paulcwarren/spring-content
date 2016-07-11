# Spring Content (incl. Spring Content Rest)

(Hopefully) a new spring community project adding basic content handling patterns to Spring for both java and hypermedia-based REST.  This is done through a central "content store" abstraction with basic content store implementations for JPA, Mongo databases and the popular S3 object store.

## Projects

- spring-content-commons; common core
- spring-content-jpa; JPA implementation of a ContentStore that stores content as LOBs
- spring-content-mongo; mongo implementation of a ContentStore that stores content in GridFs
- spring-content-s3; s3 implementation of a ContentStore that stores content in an Amazon S3 bucket
- spring-content-rest; a REST layer on to of spring content that adds content links to spring data rest

- spring-boot-starter-content-jpa; spring boot starter including autoconfiguration for spring-content-jpa
- spring-boot-starter-content-mongo; spring boot starter including autoconfiguration for spring-content-mongo
- spring-boot-starter-content-s3; spring boot starter including autoconfiguration for spring-content-s3
- spring-boot-starter-content-rest; spring boot starter incl. autoconfiguration for spring-content-rest

### Example/Test Projects
- spring-eg-content-jpa; tests spring-content-jpa Java API
- spring-eg-content-mongo; tests spring-content-mongo Java API
- spring-eg-content-s3; tests spring-content-mongo Java API
- spring-eg-starter-content-jpa; tests spring-content-jpa autoconfiguration 
- spring-eg-starter-content-mongo; tests spring-content-mongo autoconfiguration
- spring-eg-starter-content-s3; tests spring-content-s3 autoconfiguration
- spring-eg-content-rest; tests spring content REST API (on top of a mongo content store) 

### Getting Started
- spring-gs-accessing-content-mongo; standard spring.io getting started guide project
- [spring-quickclaim](https://bitbucket.org/paulcwarren/spring-content/src/ef9e0716a56310fac5e6390233a6cd73ad4a28e8/spring-gs-content-quickclaim/readme.md?at=master); example spring-boot based application based on an insurance companies claim system showing spring content and spring content REST in action

## Todo
- --Add S3 implementation--
- Add support for basic transformations; i.e. (doc -> pdf)
- Add File System implementation
- Add a CDN implementation (TBD: which CDN?)
- Add ALPs support
- Add support for fulltext query methods similar to SD's findBy methods (look at Solr)
- Add support for byte range content handling for clients like Adobe's PDF Reader
- Possibly add a Spring Content-based Webdav library implementation?
- Possibly add a Spring Content-based CMIS implementation
