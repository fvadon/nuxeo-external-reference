package org.nuxeo.labs.reference.operation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.labs.reference.constants.ExternalReferenceConstant;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractExternalReferenceActions {

    /**
     * Add external reference. Will not add it again if it exists and
     * addEvenIfAlreadyExist is false. Note that the identifiers of the entry
     * would be the documentUID+externalReference, the label and source are just
     * additional optional info
     *
     * If the UID is a proxy, it will also store the live documentUID
     *
     * @param dirSession
     * @param documentUID
     * @param externalReference
     * @param addEvenIfAlreadyExist
     * @return DocumentModel of the created entry, null if no entry is created
     */

    protected DocumentModel addExternalRef(CoreSession coreSession,
            String documentUID, String externalReference,
            String referenceLabel, String externalSource,
            boolean addEvenIfAlreadyExist) {

        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);
        try {
            DocumentModel dm = null;
            boolean canAddEntry = true;

            // Test existence and set canAddEntry to false if this is the
            // case
            if (!addEvenIfAlreadyExist) {

                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                // Test for live doc
                filter.put(
                        ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD,
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
                                externalReference, referenceLabel,
                                externalSource);
                    } else {
                        // It's a proxy get the live doc
                        DocumentModel documentLive = coreSession.getSourceDocument(new IdRef(
                                documentUID));
                        if (documentLive.isVersion()) {
                            documentLive = coreSession.getSourceDocument(documentLive.getRef());
                        }
                        dm = addExternalRef(dirSession, documentLive.getId(),
                                documentUID, externalReference, referenceLabel,
                                externalSource);
                    }
                }
            }
            return dm;
        } finally {
            dirSession.close();
        }

    }

    /**
     * Will add the entries to dirSession. No control at this point.
     *
     *
     */
    protected DocumentModel addExternalRef(Session dirSession,
            String liveDocUID, String proxyUID, String externalRef,
            String referenceLabel, String externalSource) {

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

        if (referenceLabel != null) {
            map.put(ExternalReferenceConstant.EXTERNAL_REFERENCE_LABEL_FIELD,
                    referenceLabel);
        }
        if (externalSource != null) {
            map.put(ExternalReferenceConstant.EXTERNAL_SOURCE_FIELD,
                    externalSource);
        }

        DocumentModel dm = dirSession.createEntry(map);

        return dm;
    }

    /**
     * Removes any tuples of DocumentUID and External Reference. If one is null,
     * then removes all occurrences. Making the assumption that they are not
     * both null... It will check DocumentUID both as live document and proxy
     *
     * @param documentUID
     * @param externalReference
     */
    protected void removeExternalReference(String documentUID,
            String externalReference) {
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();

            // Look for lives doc references
            if (documentUID != null) {

                filter.put(
                        ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD,
                        documentUID);
            }
            if (externalReference != null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                        externalReference);
            }

            DocumentModelList list = dirSession.query(filter);

            for (DocumentModel entry : list) {
                dirSession.deleteEntry(entry);
            }
            // Look for Proxy doc references
            filter.clear();
            if (externalReference != null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                        externalReference);
            }
            if (documentUID != null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD,
                        documentUID);
            }
            list = dirSession.query(filter);

            for (DocumentModel entry : list) {
                dirSession.deleteEntry(entry);
            }
        } finally {
            dirSession.close();
        }

    }

    /**
     *
     *
     * @param documentUID
     * @param externalReference
     * @return a DocumentModelList of any combination of the search on
     *         DocumentUID and ExternalReference. (Given the fact that
     *         DocumentUID can a proxy or a live document
     */
    protected DocumentModelList getExternalReferenceInfo(String documentUID,
            String externalReference) {
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);
        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();

            if (externalReference != null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                        externalReference);
            }
            if (documentUID != null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD,
                        documentUID);
            }

            DocumentModelList list = dirSession.query(filter);

            if (list.size() == 0) {
                // If there is no Proxy, try with Live docs as it can't be in
                // both
                filter.clear();
                if (externalReference != null) {
                    filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                            externalReference);
                }
                if (documentUID != null) {
                    filter.put(
                            ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD,
                            documentUID);
                }
                list = dirSession.query(filter);
            }

            return list;
        } finally {
            dirSession.close();
        }

    }

    /**
     *
     * @param coreSession
     * @param externalReference
     * @return String containing the label for the referenced document
     */
    protected String getReferenceVersionLabel(CoreSession coreSession,
            DocumentModel externalReference) {
        DocumentModel referencedDocument;
        // A reference to a proxy document will have a not null proxy field
        if (externalReference.getProperty(
                ExternalReferenceConstant.EXTERNAL_REF_SCHEMA,
                ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD) != null) {
            referencedDocument = coreSession.getDocument(new IdRef(
                    (String) externalReference.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                            + ":"
                            + ExternalReferenceConstant.EXTERNAL_PROXY_UID_FIELD)));
        } else {
            referencedDocument = coreSession.getDocument(new IdRef(
                    (String) externalReference.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                            + ":"
                            + ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD)));
        }

        return referencedDocument.getVersionLabel();

    }
}
