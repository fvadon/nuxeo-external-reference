package org.nuxeo.labs.reference.operation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.operations.services.directory.AbstractDirectoryOperation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.labs.reference.constants.ExternalReferenceConstant;
import org.nuxeo.runtime.api.Framework;

public class AbstractExternalReferenceOperation extends
        AbstractDirectoryOperation {

    /**
     *
     * @param dirSession
     * @param documentUID2
     * @param externalReference2
     * @param addEvenIfAlreadyExist2
     * @return DocumentModel of the created entry, null if no entry is created
     */

    protected DocumentModel addExternalRef(CoreSession coreSession,
            String documentUID, String externalReference,
            boolean addEvenIfAlreadyExist) {

        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);

        DocumentModel dm = null;
        boolean canAddEntry = true;
        if (!addEvenIfAlreadyExist) {
            // Here test existence and set canAddEntry to false if this is the
            // case
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            // Test for live doc
            filter.put(ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD,
                    documentUID);
            filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                    externalReference);
            DocumentModelList list = dirSession.query(filter);
            // Test for proxy
            filter.clear();
            filter.put(ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD,
                    documentUID);
            filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                    externalReference);
            list.addAll(dirSession.query(filter));
            if (list.size() > 0) {
                canAddEntry = false;
            }

        }

        if (canAddEntry) {
            // Fetch the document from UID
            DocumentModel docToAdd = coreSession.getDocument(new IdRef(
                    documentUID));
            if (docToAdd != null) {
                if (!docToAdd.isProxy()) {
                    dm = addExternalRef(dirSession, documentUID, null,
                            externalReference);
                }
                else{
                // It's a proxy get the live doc
                DocumentModel documentLive = coreSession.getSourceDocument(new IdRef(
                        documentUID));
                if(documentLive.isVersion()){
                    documentLive=coreSession.getSourceDocument(documentLive.getRef());
                }
                dm = addExternalRef(dirSession, documentLive.getId(), documentUID,
                        externalReference);
                }
            }
        }
        return dm;

    }

    /**
     * Will add the entries to dirSession. No control at this point. LiveDocUID
     * and proxyUID are not required though.
     *
     *
     */
    protected DocumentModel addExternalRef(Session dirSession,
            String liveDocUID, String proxyUID, String externalRef) {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD, externalRef);

        if (liveDocUID != null) {
            map.put(ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD,
                    liveDocUID);
        }
        if (proxyUID != null) {
            map.put(ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD,
                    proxyUID);
        }

        DocumentModel dm = dirSession.createEntry(map);

        return dm;
    }

}
