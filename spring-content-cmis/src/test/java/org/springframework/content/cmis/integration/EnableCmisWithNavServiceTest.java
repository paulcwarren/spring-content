package org.springframework.content.cmis.integration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.content.cmis.support.ApplicationWithNavService;
import org.springframework.content.cmis.support.DocumentRepository;
import org.springframework.content.cmis.support.FolderRepository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = ApplicationWithNavService.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnableCmisWithNavServiceTest {

	static {
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(false);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private DocumentRepository docRepo;

    @Autowired
    private FolderRepository folderRepo;

	private CmisTests cmisTests;

	{
		Describe("CMIS with CmisNavigationService", () -> {
			BeforeEach(() -> {
                cmisTests.setPort(port);
                cmisTests.setDocumentRepository(docRepo);
                cmisTests.setFolderRepository(folderRepo);
			});

			cmisTests = new CmisTests();
		});
	}

	@Test
	public void noop(){}
}
