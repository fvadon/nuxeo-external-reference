package org.nuxeo.labs.reference.operation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateVersion;
import org.nuxeo.ecm.automation.core.operations.document.PublishDocument;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.reference.constants.ExternalReferenceConstant;
import org.nuxeo.labs.reference.operation.AddExternalReference;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author fvadon
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.labs.reference" })
public class ExternalRefTest extends AbstractExternalReferenceActions {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;

    protected DocumentModel section;

    protected DocumentModel docToPublish;

    protected DocumentModel publishedDoc;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        section = session.createDocumentModel("/", "Section", "Section");
        section.setPropertyValue("dc:title", "Section");
        section = session.createDocument(section);
        session.save();
        section = session.getDocument(section.getRef());

        docToPublish = session.createDocumentModel("/Folder", "docToPublish",
                "File");
        docToPublish.setPropertyValue("dc:title", "File");
        docToPublish = session.createDocument(docToPublish);
        session.save();
        docToPublish = session.getDocument(docToPublish.getRef());

    }

    @Test
    public void externalReferencTest() throws OperationException {
        OperationContext ctx = new OperationContext(session);

        ctx.setInput(docToPublish);
        OperationChain initChain = new OperationChain("testpublish");
        initChain.add(FetchContextDocument.ID);
        initChain.add(PublishDocument.ID).set("target", section.getId());
        publishedDoc = (DocumentModel) service.run(ctx, initChain);
        assertNotNull(publishedDoc);
        // Modifying the version number of docToPublish
        docToPublish.setPropertyValue("dc:description", "new description");
        session.saveDocument(docToPublish);

        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);
        try {
            assertNotNull(dirSession);

            // Testing adding refs for live doc ref only if does not exist.
            OperationChain chain = new OperationChain("testAddFirstSimpleEntry");
            // entry 1
            chain.add(AddExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    docToPublish.getId());
            // shouldn't be added
            chain.add(AddExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    docToPublish.getId());
            // entry 2
            chain.add(AddExternalReference.ID).set("ExternalReference",
                    "somedifferentReference").set("DocumentUID",
                    docToPublish.getId());
            DocumentModel dm = (DocumentModel) service.run(ctx, chain);
            assertNotNull(dm);
            assertEquals("2", dm.getId());
            assertNull(dirSession.getEntry("1").getProperty(
                    ExternalReferenceConstant.EXTERNAL_REF_SCHEMA,
                    ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD));
            // Testing we can add an existing ref if we want.
            OperationChain chain2 = new OperationChain("testAddExisitingEntry");
            // entry 3 (1bis)
            chain2.add(AddExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    docToPublish.getId()).set("AddEvenIfAlreadyExist", true);
            dm = (DocumentModel) service.run(ctx, chain2);
            assertNotNull(dm);
            assertEquals("3", dm.getId());
            // Testing we can add a proxy and that the correct live doc was
            // found.
            OperationChain chain3 = new OperationChain("testAddProxy");
            // entry 4
            chain3.add(AddExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    publishedDoc.getId());
            dm = (DocumentModel) service.run(ctx, chain3);
            assertNotNull(dm);
            assertEquals("4", dm.getId());
            assertEquals(
                    dm.getPropertyValue(ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD),
                    docToPublish.getId());

            // Testing get info
            OperationChain chain7 = new OperationChain("testAddProxy");
            chain7.add(GetExternalReferenceInfo.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    publishedDoc.getId());
            DocumentModelList dml = (DocumentModelList) service.run(ctx, chain7);
            assertEquals(1, dml.size());

            OperationChain chain8 = new OperationChain("testAddProxy");
            chain8.add(GetExternalReferenceInfo.ID).set("DocumentUID",
                    docToPublish.getId());
            dml = (DocumentModelList) service.run(ctx, chain8);
            // should be 4 because there is also a version using this.
            assertEquals(4, dml.size());

            // Testing get ref version label
            assertEquals("0.1",
                    getReferenceVersionLabel(session, dirSession.getEntry("4")));
            assertEquals("0.1+",
                    getReferenceVersionLabel(session, dirSession.getEntry("1")));

            // Testing deletion from Proxy ID
            OperationChain chain4 = new OperationChain("testAddProxy");
            chain4.add(RemoveExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    publishedDoc.getId());
            service.run(ctx, chain4);
            assertNotNull(dirSession.getEntry("1"));
            assertNotNull(dirSession.getEntry("2"));
            assertNotNull(dirSession.getEntry("3"));
            assertNull(dirSession.getEntry("4"));

            // Testing deletion from reference
            OperationChain chain5 = new OperationChain("testAddProxy");
            chain5.add(RemoveExternalReference.ID).set("ExternalReference",
                    "somedifferentReference");
            service.run(ctx, chain5);
            assertNotNull(dirSession.getEntry("1"));
            assertNull(dirSession.getEntry("2"));
            assertNotNull(dirSession.getEntry("3"));

            // Testing deletion from tuple
            OperationChain chain6 = new OperationChain("testAddProxy");
            // entry 5
            chain6.add(AddExternalReference.ID).set("ExternalReference",
                    "someLastReference").set("DocumentUID",
                    docToPublish.getId()).set("externalSource", "FakeSource").set(
                    "referenceLabel", "FakeLabel");
            chain6.add(RemoveExternalReference.ID).set("ExternalReference",
                    "someExternalReference").set("DocumentUID",
                    docToPublish.getId());
            service.run(ctx, chain6);
            assertNull(dirSession.getEntry("1"));
            assertNull(dirSession.getEntry("3"));
            assertNotNull(dirSession.getEntry("5"));
            assertEquals(
                    "FakeSource",
                    dirSession.getEntry("5").getPropertyValue(
                            "usagedirectory:externalSource"));
            assertEquals(
                    "FakeLabel",
                    dirSession.getEntry("5").getPropertyValue(
                            "usagedirectory:externalReferenceLabel"));

        } finally {
            dirSession.close();
        }
    }

    @Test
    public void storeReportingInfoOnDocumentTest() throws OperationException {
        DocumentModel document = session.createDocumentModel("/Folder",
                "docToPublish", "File");
        document.setPropertyValue("dc:title", "File");
        document = session.createDocument(docToPublish);
        session.saveDocument(document);

        OperationContext ctx = new OperationContext(session);

        ctx.setInput(document);
        OperationChain initChain = new OperationChain("testStoreDirectoryInfo");
        initChain.add(FetchContextDocument.ID);
        initChain.add(CreateVersion.ID).set("increment", "Major").set(
                "saveDocument", true);
        // entry 1
        initChain.add(AddExternalReference.ID).set("ExternalReference",
                "someExternalReference").set("DocumentUID", document.getId());
        // entry 2
        initChain.add(AddExternalReference.ID).set("ExternalReference",
                "someOtherReference").set("DocumentUID", document.getId());
        service.run(ctx, initChain);
        document = session.getDocument(new IdRef(document.getId()));
        assertEquals("1.0", document.getVersionLabel());
        assertTrue(document.hasFacet(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_FACET));
        assertEquals(
                2,
                ((String[]) document.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                        + ":"
                        + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_REFS)).length);
        assertEquals(
                (long)2,
                (document.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                        + ":"
                        + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_COUNT)));

        assertEquals(
                2,
                ((String[]) document.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                        + ":"
                        + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_DOCTITLE)).length);
        // Testing update on remove works
        removeExternalReference(session, null, "someExternalReference");
        document = session.getDocument(new IdRef(document.getId()));
        assertEquals("1.0", document.getVersionLabel());
        assertTrue(document.hasFacet(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_FACET));
        assertEquals(
                1,
                ((String[]) document.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                        + ":"
                        + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_REFS)).length);
        // Testinf the schema is removed if not ref.
        removeExternalReference(session, document.getId(), null);
        document = session.getDocument(new IdRef(document.getId()));
        assertEquals("1.0", document.getVersionLabel());
        assertFalse(document.hasFacet(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_FACET));
    }

    @Test
    public void nonexistingDocumentTests() {
        DocumentModel externalRef = addExternalRef(session, "fakeID",
                "externalRef", null, null, true);
        assertNotNull(externalRef);
        DocumentModelList externalRefList = getExternalReferenceInfo("fakeID",
                null);
        assertNotNull(externalRefList);
        assertEquals(1, externalRefList.size());

    }
}
