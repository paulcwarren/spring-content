package internal.org.springframework.content.fs.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.core.io.FileSystemResource;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class FileSystemDeletableResourceTest {

	private FileSystemDeletableResource resource;

	private FileSystemResource delegate;

	{
		Describe("FileSystemDeletableResource", () -> {
			BeforeEach(() -> {
				delegate = mock(FileSystemResource.class);
			});
			JustBeforeEach(() -> {
				resource = new FileSystemDeletableResource(delegate);
			});
			It("should delegate isOpen", () -> {
				resource.isOpen();
				verify(delegate).isOpen();
			});
			It("should delegate exists", () -> {
				resource.exists();
				verify(delegate).exists();
			});
			It("should delegate isReadable", () -> {
				resource.isReadable();
				verify(delegate).isReadable();
			});
			It("should delegate getInputStream", () -> {
				resource.getInputStream();
				verify(delegate).getInputStream();
			});
			It("should delegate isWritable", () -> {
				resource.isWritable();
				verify(delegate).isWritable();
			});
			It("should delegate getOutputStream", () -> {
				when(delegate.exists()).thenReturn(true);
				resource.getOutputStream();
				verify(delegate).getOutputStream();
			});
			It("should delegate getURL", () -> {
				resource.getURL();
				verify(delegate).getURL();
			});
			It("should delegate getURI", () -> {
				resource.getURI();
				verify(delegate).getURI();
			});
			It("should delegate getFile", () -> {
				resource.getFile();
				verify(delegate).getFile();
			});
			It("should delegate contentLength", () -> {
				resource.contentLength();
				verify(delegate).contentLength();
			});
			It("should delegate createRelative", () -> {
				resource.createRelative("some-path");
				verify(delegate).createRelative("some-path");
			});
			It("should delegate getFilename", () -> {
				resource.getFilename();
				verify(delegate).getFilename();
			});
			It("should delegate getDescription", () -> {
				resource.getDescription();
				verify(delegate).getDescription();
			});
		});
	}

}
