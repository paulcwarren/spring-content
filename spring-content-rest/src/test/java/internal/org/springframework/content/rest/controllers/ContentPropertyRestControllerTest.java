package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@RunWith(Ginkgo4jRunner.class)
public class ContentPropertyRestControllerTest {
	
	private ContentPropertyRestController controller = null;
	private ResponseEntity<InputStreamResource> resource = null;
	
	@Mock RootResourceInformation rootResourceInfoMock;
	@Mock RepositoryInvoker invokerMock;
	@Mock ContentStoreService contentRepoService;
	@Mock ContentStoreInfo contentRepoInfo;
	@Mock ContentStore<Object, Serializable> contentRepo;
	{
		Describe("ContentPropertyRestController", () -> {
			BeforeEach(() -> {
				MockitoAnnotations.initMocks(this);
				controller = new ContentPropertyRestController(contentRepoService);
			});
			
			Context("given a content entity get request", () -> {
				BeforeEach(() -> {
					//RootResourceInformation rootResourceInfoMock = mock(RootResourceInformation.class);
					//RepositoryInvoker invokerMock = mock(RepositoryInvoker.class);
					when(rootResourceInfoMock.getInvoker()).thenReturn(invokerMock);
					when(invokerMock.hasFindOneMethod()).thenReturn(true);
					when(invokerMock.invokeFindOne("12345")).thenReturn(new ContentEntity("12345"));
					when(contentRepoService.getContentStores()).thenReturn(new ContentStoreInfo[] {contentRepoInfo});
					when(contentRepoInfo.getImplementation()).thenReturn(contentRepo);
					Mockito.doReturn(ContentEntity.class).when(contentRepoInfo).getDomainObjectClass();
					Mockito.doReturn(ContentEntityContentRepository.class).when(contentRepoInfo).getInterface();
					ContentEntity contentBean = new ContentEntity("12345");
					InputStream in = new ByteArrayInputStream("Hello Spring Content Rest World!".getBytes());
					when(contentRepo.getContent(anyObject())).thenReturn(in);
					
					resource = controller.getContent(rootResourceInfoMock, "files", "12345", "content", "12345", "*/*");
				});
				
				It("should return content", () -> {
					assertThat(resource.getStatusCode(), is(HttpStatus.OK));
					assertThat(resource.getBody(), is(not(nullValue())));
					assertThat(IOUtils.toString(resource.getBody().getInputStream()), is("Hello Spring Content Rest World!"));
				});
			});
		});
	}

	@Content
	public class ContentEntity {
		public String id;

		public ContentEntity(String id) {
			super();
			this.id = id;
		}
	}
	
	@ContentStoreRestResource(path="content")
	public interface ContentEntityContentRepository extends ContentStore<Object, Serializable> {
	}
}
