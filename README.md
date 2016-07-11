![Alt text](https://travis-ci.org/paulcwarren/spring-content.svg?branch=master)

# Getting Started With Spring Content

This guide walks you through building an application that uses Spring Content to store and retrieve  content in a database.

## What you'll build

You'll build an application that stores Document POJOs in a Mongo database.

## What you'll need

- About 15 minutes
- A favorite text editor or IDE
- JDK 1.8 or later
- Maven 3.0+
- MongoDB 3.0.7
- You can also import the code from this guide as well as view the web page directly into Spring Tool Suite (STS) and work your way through it from there.

## How to complete this guide

Like most Spring Getting Started guides, you can start form scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To start from scratch, move on to Build with Maven.

To skip the basics, do the following:

- Download and unzip the source repository for this guide, or clone it using Git: `git clone https://paulcwarren@bitbucket.org/paulcwarren/spring-content.git`
- cd into spring-content/spring-gs-accessing-data-mongo/initial
- Jump ahead to `Define a simple entity`.
When you’re finished, you can check your results against the code in `spring-content/spring-gs-accessing-content-mongo/complete`.

## Build with Maven

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Maven](https://maven.apache.org/) is included here. If you’re not familiar with Maven, refer to [Building Java Projects with Maven](http://spring.io/guides/gs/maven). 
 
### Create a directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

```
∟ src
   ∟ main
       ∟ java
           ∟ docs
```

`pom.xml`


	<?xml version="1.0" encoding="UTF-8"?>
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
		<modelVersion>4.0.0</modelVersion>
	
		<groupId>org.springframework.content.gs</groupId>
		<artifactId>gs-accessing-content-jpa</artifactId>
		<version>0.1.0</version>
	
		<parent>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-parent</artifactId>
			<version>1.2.7.RELEASE</version>
		</parent>
	
		<dependencies>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-starter-data-mongodb</artifactId>
			</dependency>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-starter-data-rest</artifactId>
			</dependency>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-starter-web</artifactId>
			</dependency>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-starter-content-mongo</artifactId>
				<version>1.2.1.RELEASE</version>
			</dependency>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-starter-content-rest</artifactId>
				<version>1.2.1.RELEASE</version>
			</dependency>
		</dependencies>
	
		<properties>
			<java.version>1.8</java.version>
		</properties>
	
		<build>
			<plugins>
				<plugin>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-maven-plugin</artifactId>
				</plugin>
			</plugins>
		</build>
	</project>

The [Spring Boot Maven plugin](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-tools/spring-boot-maven-plugin) provides many convenient features:

- It collects all the jars on the classpath and builds a single, runnable "über-jar", which makes it more convenient to execute and transport your service.
- It searches for the `public static void main()` method to flag as a runnable class.
- It provides a built-in dependency resolver that sets the version number to match [Spring Boot dependencies](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-dependencies/pom.xml). You can override any version you wish, but it will default to Boot’s chosen set of versions.

### Define a simple entity with content

In this example, you store Document objects, annotated as a Mongo entity with Content.

`src/main/java/docs/SpringDocument.java`

	package docs;
	
	import org.springframework.content.annotations.Content;
	import org.springframework.content.annotations.ContentId;
	import org.springframework.content.annotations.ContentLength;
	import org.springframework.data.annotation.Id;
	import org.springframework.data.mongodb.core.mapping.Document;
	
	@Document
	public class SpringDocument {
	
		@Id
		private String id;
		
		private String title;
		private List<String> keywords;
		
		@Content
		private ContentMetadata content;
	
		... getters and setters ...
		
		public static class ContentMetadata {
			
			public ContentMetadata() {}
			
			@ContentId
			private String id;
			
			@ContentLength
			private long length;
			
			@MimeType
			private String mimeType;
	
			.. getters and setters ...
			
		}
	}



Here you have a standard Spring Data entity bean class `SpringDocument` class with several attributes, `id`, `title`, `keywords`.

>**Note:** For more information on Spring Data annotations see the relevant [Spring Data](http://docs.spring.io/spring-data/commons/docs/current/reference/html/) documentation.

In addition this entity bean also has the `content` attribute annotated with the Spring Content annotation `@Content`, indicating that instances of `ContentMetadata` are content entities.  These entities will be mapped to Mongo's GridFS.  

`ContentMetadata` has three attributes, `id`, `length` and `mimeType`.  The `id` attribute is annotated with `@ContentId` so that Spring Content will recognize it as the content entity's ID.  

The `length` attribute is annotated with `@ContentLength` so that Spring Content will recognize it as the content entity's content length.  

Finally, the `mimeType` attribute is annotated with `@MimeType` so that Spring Content REST will recognize it as the content entity's mime type.

All of these annotated attributes will be managed by Spring Content. 

### Create a Spring Data Repository

So that we can perform simple CRUD operations, over a hypermedia-based API, create a simple repository for the `SpringDocument` class annotated with as a `@RepositoryRestResource`.

`src/main/java/docs/SpringDocumentRepository.java`

	package docs;
	
	import org.springframework.data.repository.CrudRepository;
	import org.springframework.data.rest.core.annotation.RepositoryRestResource;
	
	@RepositoryRestResource(path="/docs", collectionResourceRel="docs")
	public interface SpringDocumentRepository extends CrudRepository<SpringDocument, String> {
	
	}

### Add a Spring Content ContentStore

Just like Spring Data focuses on storing data in a database, Spring Content focuses on storing content in various stores, in this case in Mongo GridFS store.  It's most compelling feature is the ability to create content store implementations automatically, at runtime, from a content store interface.

To see how this works, create a content store interface that works with SpringDocument's `ContentMetadata` entity:

`src/main/java/docs/ContentMetadataContentStore.java`

	package docs;
	
	import org.springframework.content.common.repository.ContentStore;
	
	import docs.SpringDocument.ContentMetadata;
	import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
	
	@ContentStoreRestResource
	public interface ContentMetadataContentStore extends ContentStore<ContentMetadata, String> {
	}
 
`ContentMetadataContentStore` extends the `ContentStore` interface.  The type of the content entity and the type of the content entity's ID, `ContentMetadata` and `String` respectively, are specified in the generic parameters on `ContentStore`.  By extending `ContentStore`, `ContentMetadataContentStore` inherits several methods for working with persisted content.

In a java application, you would expect to write an implementation for the `ContentMetadaDataContentStore` class.  But what makes Spring Content so powerful is that you don't have to do this.  Spring Content will create an implementation on the fly when you run the application.

Likewise, in a web-based application you would also expect to implement HTTP handlers allowing you to PUT and GET content over HTTP.  With Spring Content REST these are also implemented for you when you add the `ContentStoreRestResource` annotation to your content store.          

Let's wire this up and see what it looks like!

### Create an application class

Spring Content integrates seamlessly into Spring Boot, therefore you create the standard Application class. 

`src/main/java/docs/Application.java`

	package docs;
	
	import org.springframework.boot.SpringApplication;
	import org.springframework.boot.autoconfigure.SpringBootApplication;
	
	@SpringBootApplication
	public class ContentApplication {
	
		public static void main(String[] args) {
			SpringApplication.run(ContentApplication.class);
		}
	}

### Build an executable JAR

You can build a single executable JAR file that contains all the necessary dependencies, classes and resources.  This makes it easy to ship, version and deploy.

If you are using Maven, you can run the application using `mvn spring-boot:run`. Or you can build the JAR file with `mvn clean package` and run the JAR by typing:

	java -jar target/spring-gs-accessing-content-mongo-0.1.0.jar

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](http://spring.io/guides/gs/convert-jar-to-war/) instead.

### Handle Content

First create an instance of a `SpringDocument`.  Using curl, issue the following command:

	curl -XPOST -H 'Content-Type:application/json' -d '{"title":"test doc","keywords":["one","two"]}' http://localhost:8080/docs

Check that this `SpringDocument` was created by issing the following command:

	curl http://localhost:8080/docs

and this should respond with:

	{
	  "_embedded" : {
	    "docs" : [ {
	      "title" : "test doc",
	      "keywords" : [ "one", "two" ],
	      "content" : null,
	      "_links" : {
	        "self" : {
	          "href" : "http://localhost:8080/docs/5636224fa82677aa529322b6"
	        }
	      }
	    } ]
	  }
	}
 
which shows us there is one document that may be fetched with a `GET` request to `http://localhost:8080/docs/5636224fa82677aa529322b6`

> **Note:** you're IDs will obviously be different, adjust as appropriate
 
Notice, that the `content` attribute is null.  That is because we haven't added any content yet so let's add some, issue the following command:

	curl -XPOST -F file=@/tmp/test.txt http://localhost:8080/docs/5636224fa82677aa529322b6/content

> **Note:** In our example /tmp/test.txt contains the simple plain text `Hello Spring Content World!` but this could be any binary content

Now, re-query the original `SpringDocument` again:-

	curl http://localhost:8080/docs/5636224fa82677aa529322b6
  
This time it should respond with:

	{
	  "title" : "test doc",
	  "keywords" : [ "one", "two" ],
	  "content" : {
	    "length" : 28,
	    "mimeType" : "text/plain"
	  },
	  "_links" : {
	    "self" : {
	      "href" : "http://localhost:8080/docs/5636224fa82677aa529322b6"
	    },
	    "content" : {
	      "href" : "http://localhost:8080/docs/5636224fa82677aa529322b6/content/64bf2339-c8e5-44f1-b960-aeb9ea8e4a7e"
	    }
	  }
	}

We see the `content` attribute now contains useful information about the document's content, namely `length` and `mimeType`.  These were set automatically by Spring Content.

Similarly, `_links` also now contains a linkrel for `content` allowing clients to navigate from this `SpringDocument` resource to it's content.  Let's do that now, issue the command:

	http://localhost:8080/docs/5636224fa82677aa529322b6/content/64bf2339-c8e5-44f1-b960-aeb9ea8e4a7e

which responds:

	Hello Spring Content World!

### Summary 

Congratulations!  You've written a simple application that uses Spring Content and Spring Content REST to save objects with content to a database and to fetch them again using a hypermedia-based REST API - all without writing a single concrete implementation class. 
  
### Want to know more

Read more about the project including it's anticipated backlog [here](spring-content.md).
