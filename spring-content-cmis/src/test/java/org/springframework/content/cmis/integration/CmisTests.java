package org.springframework.content.cmis.integration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.IOUtils;
import org.springframework.content.cmis.support.DocumentRepository;
import org.springframework.content.cmis.support.FolderRepository;

import lombok.Setter;

public class CmisTests {

	@Setter
	private int port;

	@Setter
	private DocumentRepository documentRepository;

	@Setter
	private FolderRepository folderRepository;

	private Session s;
	private Folder root, folder, subfolder;
	private Document doc, pwc;

	private String mimetype = "text/plain; charset=UTF-8";
	private String content = "This is some test content.";
	private String filename = "some-file.txt";
	private String description = "This is a test description.";

	{
		Describe("given a repository", () -> {
			BeforeEach(() -> {
				SessionFactory f = SessionFactoryImpl.newInstance();
				Map<String, String> parameter = new HashMap<String, String>();

				parameter.put(SessionParameter.BROWSER_URL, "http://localhost:" + port + "/browser");
				parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
				parameter.put(SessionParameter.REPOSITORY_ID, "1");

				parameter.put(SessionParameter.USER, "test");
				parameter.put(SessionParameter.PASSWORD, "test");

				parameter.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
				parameter.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "en");

				s = f.createSession(parameter);
				assertThat(s, is(not(nullValue())));
			});

			It("should have correct repository info", () -> {
				RepositoryInfo info = s.getRepositoryInfo();
				assertThat(info.getId(), is("1"));
				assertThat(info.getName(), is("Example"));
				assertThat(info.getDescription(), is("Spring Content Example"));
				assertThat(info.getVendorName(), is("Spring Content"));
				assertThat(info.getProductName(), is("Spring Content CMIS Connector"));
				assertThat(info.getProductVersion(), is("1.0.0"));
			});

			It("should not have any children", () -> {
				Folder root = s.getRootFolder();
				assertThat(root.getChildren().getTotalNumItems(), is(-1L));
			});

			Context("given a folder is created in the root", () -> {
				BeforeEach(() -> {
					root = s.getRootFolder();

					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put(PropertyIds.NAME, "folder1");
					properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
					properties.put(PropertyIds.DESCRIPTION, description);

					folder = root.createFolder(properties);
				});

				It("should be present in the root", () -> {
					ItemIterable<CmisObject> children = root.getChildren();
					assertThat(children, hasItem(hasProperty("name", is("folder1"))));
				});

				It("should contain all properties", () -> {
					ItemIterable<CmisObject> children = root.getChildren();

					Iterator<CmisObject> i = children.iterator();
					while (i.hasNext()) {
						CmisObject object = i.next();
						assertThat(object.getDescription(), equalTo(description));
					}
					//TODO
					//cmis:secondaryObjectTypeIds
					//cmis:creationDate
					//cmis:lastModificationDate
					//cmis:changeToken
					//cmis:allowedChildObjectTypeIds
				});

				It("should be deletable", () -> {
				    String folderId = folder.getId();
                    assertThat(folderRepository.existsById(Long.parseLong(folderId)), is(true));
                    folder.delete();
                    assertThat(folderRepository.existsById(Long.parseLong(folderId)), is(false));
				});

				Context("given a subfolder is created", () -> {
					BeforeEach(() -> {
						Map<String, Object> properties = new HashMap<String, Object>();
						properties.put(PropertyIds.NAME, "subfolder");
						properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");

						subfolder = folder.createFolder(properties);
					});
					It("should be present in the root folder", () -> {
						ItemIterable<CmisObject> children = folder.getChildren();
						assertThat(children, hasItem(hasProperty("name", is("subfolder"))));
					});

					Context("given a document is created", () -> {

						BeforeEach(() -> {
							Map<String, Object> properties = new HashMap<String, Object>();
							properties.put(PropertyIds.NAME, "doc1");
							properties.put(PropertyIds.DESCRIPTION, "a description");
							properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

							doc = subfolder.createDocument(properties, new ContentStreamImpl("test", "text/plain", ""), VersioningState.NONE);
						});

						It("should be present in the subfolder", () -> {
							ItemIterable<CmisObject> children = subfolder.getChildren();
							assertThat(children, hasItem(hasProperty("name", is("doc1"))));
						});

						It("should have the appropriate properties", () -> {
							assertThat(doc.getName(), is("doc1"));
							assertThat(doc.getDescription(), is("a description"));
						});

						It("should be delete-able", () -> {
		                    String docId = doc.getId();
		                    assertThat(documentRepository.existsById(Long.parseLong(docId)), is(true));
		                    doc.delete(false);
		                    assertThat(documentRepository.existsById(Long.parseLong(docId)), is(false));

							ItemIterable<CmisObject> children = subfolder.getChildren();
							assertThat(children, not(hasItem(hasProperty("name", is("doc1")))));
						});

						Context("given the document's properties are updated", () -> {

							BeforeEach(() -> {
								Map<String, Object> properties = new HashMap<String, Object>();
								properties.put(PropertyIds.DESCRIPTION, "an updated description");

								doc = (Document) doc.updateProperties(properties);
							});

							It("should set the properties", () -> {
								assertThat(doc.getDescription(), is("an updated description"));
							});
						});

						Context("given the document has content", () -> {

							BeforeEach(() -> {
								mimetype = "text/plain; charset=UTF-8";
								content = "This is some test content.";
								filename = "some-file.txt";

								byte[] buf = content.getBytes("UTF-8");
								ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes("UTF-8"));

								ContentStream contentStream = s.getObjectFactory().createContentStream(filename, buf.length, mimetype, input);
								doc.setContentStream(contentStream, false);
							});

							It("should be retrievable", () -> {
								ContentStream contentStream = doc.getContentStream();
								assertThat(contentStream, is(not(nullValue())));

								InputStream actualStream = contentStream.getStream();
								InputStream expectedStream = new ByteArrayInputStream(content.getBytes());
								IOUtils.contentEquals(expectedStream, actualStream);
								IOUtils.closeQuietly(expectedStream, actualStream);
							});

							It("should be update-able", () -> {
								content = "This is some updated test content.";

								byte[] buf = content.getBytes("UTF-8");
								ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes("UTF-8"));

								ContentStream contentStream = s.getObjectFactory().createContentStream(filename, buf.length, mimetype, input);
								doc.setContentStream(contentStream, false);

								contentStream = doc.getContentStream();
								assertThat(contentStream, is(not(nullValue())));
								InputStream actualStream = contentStream.getStream();
								InputStream expectedStream = new ByteArrayInputStream(content.getBytes());
								IOUtils.contentEquals(expectedStream, actualStream);
								IOUtils.closeQuietly(expectedStream, actualStream);
							});

							Context("given a private working copy is created", () -> {

								BeforeEach(() -> {
									ObjectId id = doc.checkOut();
									doc = (Document) s.getObject(id);
									assertThat(doc.isVersionSeriesCheckedOut(), is(true));
									pwc = (Document) s.getObject(doc.getVersionSeriesCheckedOutId());
								});

								It("should have a new id and be flagged as a working copy", () -> {
									assertThat(doc.getId(), is(not(pwc.getId())));
									assertThat(pwc.isPrivateWorkingCopy(), is(true));
									assertThat(pwc.isVersionSeriesCheckedOut(), is(true));
									assertThat(doc.isVersionSeriesCheckedOut(), is(true));
									assertThat(pwc.getVersionSeriesCheckedOutBy(), is("test"));
								});

								Context("given the working copy is updated and checked in", () -> {

									BeforeEach(() -> {
										Map<String, Object> properties = new HashMap<>();
										properties.put(PropertyIds.DESCRIPTION, "a description");

										ObjectId id = pwc.checkIn(true,
												properties,
												s.getObjectFactory().createContentStream(filename, 0, mimetype, new ByteArrayInputStream("".getBytes())),
												"a check-in comment");

										doc = (Document) s.getObject(id);
									});

									It("should promote the working copy to the latest", () -> {
										assertThat(doc.getId(), is(pwc.getId()));
										assertThat(doc.getPropertyValue("cmis:description"), is("a description"));
										assertThat(doc.isLatestVersion(), is(true));
										assertThat(doc.isLatestMajorVersion(), is(true));
										assertThat(doc.isVersionSeriesCheckedOut(), is(false));
										assertThat(doc.getCheckinComment(), is("a check-in comment"));
									});

									It("should delete all versions", () -> {

									    List<String> ids = new ArrayList<>();
									    doc.getAllVersions().forEach((doc) -> {
									        ids.add(doc.getId());
									    });

									    doc.delete(true);

									    ids.forEach((id) -> {
									        assertThat(documentRepository.existsById(new Long(id)), is(false));
									    });
									});
								});

								Context("given the working copy is cancelled", () -> {

									BeforeEach(() -> {
										String id = doc.getId();
										doc.cancelCheckOut();
										doc = (Document) s.getObject(id);
									});

									It("should re-instate the document", () -> {
										assertThat(doc.isVersionSeriesCheckedOut(), is(false));
										assertThat(doc.isLatestVersion(), is(true));
									});
								});
							});
						});
					});
				});
			});
		});
	}
}
