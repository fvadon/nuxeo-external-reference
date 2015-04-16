package org.nuxeo.labs.reference.hippo.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.reference.hippo.operation.AbstractExternalReferenceHippoActions;
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
public class ExternalRefHippoTest extends AbstractExternalReferenceHippoActions{

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

        docToPublish = session.createDocumentModel("/Folder", "docToPublish", "File");
        docToPublish.setPropertyValue("dc:title", "File");
        docToPublish = session.createDocument(docToPublish);
        session.save();
        docToPublish = session.getDocument(docToPublish.getRef());

    }


    @Test
    public void hippoRefsTest() throws IOException {
        String nuxeoUID= "ccd5a6f4-2440-4346-a021-36d2b613845c";
        List<String> hippoRefs = getRefsForDocumentFromHippo(nuxeoUID);
        assertNotNull(hippoRefs);

        List<String> nuxeoUIDs = getAllDocumentRefsFromHippo();
        assertNotNull(nuxeoUIDs);

        //Test correctiong the hippo link that is incorrect
        String wrongLink = "http://www.chi.test.us.onehippo.com/nuxeo/news/2014/10/gogreen-nominated-for-unef-sustainable-business-award.html";
        String correctLink = "http://www.chi.test.us.onehippo.com/news/2014/10/gogreen-nominated-for-unef-sustainable-business-award.html";
        assertEquals(correctLink,correctHippoLink(wrongLink));
        assertEquals(correctLink,correctHippoLink(correctLink));

        //Test Label Extraction from Ref
        String ref = "http://www.chi.test.us.onehippo.com/nuxeo/news/2014/10/gogreen-nominated-for-unef-sustainable-business-award.html";
        String label= "gogreen nominated for unef sustainable business award";
        assertEquals(label,extractHippoLabelFromLink(ref));

    }


}
