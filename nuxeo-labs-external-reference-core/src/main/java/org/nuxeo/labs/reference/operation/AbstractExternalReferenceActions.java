package org.nuxeo.labs.reference.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.labs.reference.constants.ExternalReferenceConstant;
import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractExternalReferenceActions {

    private static final Log log = LogFactory.getLog(AbstractExternalReferenceActions.class);
    private DocumentModel liveDocument;
    private DocumentModel directoryEntry;
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
            directoryEntry = null;
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
                DocumentModel docToAdd = null;
                // Fetch the document from UID
                try {
                    docToAdd = coreSession.getDocument(new IdRef(documentUID));
                } catch (Exception e) {
                    log.warn(
                            "Trying to store ref for a non existing document, storing it anyway",
                            e);
                    directoryEntry = addExternalRef(dirSession, documentUID, null,
                            externalReference, referenceLabel, externalSource);

                }
                if (docToAdd != null) {
                    if (!docToAdd.isProxy()) {
                        directoryEntry = addExternalRef(dirSession, documentUID, null,
                                externalReference, referenceLabel,
                                externalSource);
                        // Update stats
                        updateReferenceInfoForReporting(docToAdd);
                    } else {
                        // It's a proxy get the live doc
                        new UnrestrictedSessionRunner(coreSession) {
                            @Override
                            public void run() throws ClientException {
                                liveDocument = coreSession.getSourceDocument(new IdRef(
                                        documentUID));
                                if (liveDocument.isVersion()) {
                                    liveDocument = coreSession.getSourceDocument(liveDocument.getRef());
                                }
                                directoryEntry = addExternalRef(dirSession,
                                        liveDocument.getId(), documentUID,
                                        externalReference, referenceLabel,
                                        externalSource);

                            }

                        }.runUnrestricted();

                        // Update stats
                        updateReferenceInfoForReporting(liveDocument);

                    }
                }
            }
            return directoryEntry;
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
     * @param coreSession
     * @param documentUID
     * @param externalReference
     */
    protected void removeExternalReference(CoreSession coreSession,
            String documentUID, String externalReference) {
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        Session dirSession = dirService.open(ExternalReferenceConstant.EXTERNAL_REF_DIRECTORY);
        Map<String, Serializable> filter = new HashMap<String, Serializable>();

        List<String> previousDocumentUIDs = new ArrayList<String>();

        try {

            // Storing the list of UID that will be removed to updated the stats
            // when documentUID is null
            if (documentUID == null) {
                filter.put(ExternalReferenceConstant.EXTERNAL_REF_FIELD,
                        externalReference);
                DocumentModelList previousValues = dirSession.query(filter);

                for (DocumentModel entry : previousValues) {
                    previousDocumentUIDs.add((String) entry.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                            + ":"
                            + ExternalReferenceConstant.EXTERNAL_LIVEDOC_UID_FIELD));
                }
            } else {
                previousDocumentUIDs.add(documentUID);
            }

            filter.clear();
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

            // Updating the stats
            for (String previousDocumentUID : previousDocumentUIDs) {
                DocumentModel document = coreSession.getDocument(new IdRef(
                        previousDocumentUID));
                if (document != null) {
                    if (!document.isProxy()) {
                        // Update stats
                        updateReferenceInfoForReporting(document);
                    } else {
                        // It's a proxy get the live doc
                        document = coreSession.getSourceDocument(new IdRef(
                                previousDocumentUID));
                        if (document.isVersion()) {
                            document = coreSession.getSourceDocument(document.getRef());
                        }
                        // Update stats
                        updateReferenceInfoForReporting(document);
                    }
                }

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

    protected DocumentModel updateReferenceInfoForReporting(
            DocumentModel document) {

        // Store the refs
        CoreSession session = document.getCoreSession();

        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() throws ClientException {

                DocumentModel temp = document;
                // values can exist multiple time, we have to reset the full
                // schema
                temp.removeFacet(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_FACET);
                // Disabling events
                disableEvents(temp);
                temp = session.saveDocument(temp);
                // Get the external ref values
                DocumentModelList externalRefs = getExternalReferenceInfo(
                        temp.getId(), null);
                // De-normalize the values into string lists
                if (externalRefs.size() > 0) {
                    List<String> externalRefsStringList = new ArrayList<String>();
                    List<String> externalRefsLabelsStringList = new ArrayList<String>();
                    List<String> externalSourcesStringList = new ArrayList<String>();
                    List<String> externalCurrentDocumentTitleStringList = new ArrayList<String>();

                    for (DocumentModel externalRef : externalRefs) {
                        externalRefsStringList.add((String) externalRef.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                                + ":"
                                + ExternalReferenceConstant.EXTERNAL_REF_FIELD));
                        externalRefsLabelsStringList.add((String) externalRef.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                                + ":"
                                + ExternalReferenceConstant.EXTERNAL_REFERENCE_LABEL_FIELD));
                        externalSourcesStringList.add((String) externalRef.getPropertyValue(ExternalReferenceConstant.EXTERNAL_REF_SCHEMA
                                + ":"
                                + ExternalReferenceConstant.EXTERNAL_SOURCE_FIELD));
                        externalCurrentDocumentTitleStringList.add((String) temp.getPropertyValue("dc:title"));
                    }
                    temp.addFacet(ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_FACET);
                    // Disabling events
                    disableEvents(temp);
                    temp = session.saveDocument(temp);
                    temp.setPropertyValue(
                            ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                                    + ":"
                                    + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_REFS,
                            (Serializable) externalRefsStringList);
                    temp.setPropertyValue(
                            ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                                    + ":"
                                    + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_LABELS,
                            (Serializable) externalRefsLabelsStringList);

                    temp.setPropertyValue(
                            ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                                    + ":"
                                    + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SOURCES,
                            (Serializable) externalSourcesStringList);
                    temp.setPropertyValue(
                            ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                                    + ":"
                                    + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_COUNT,
                            externalRefsStringList.size());
                    temp.setPropertyValue(
                            ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_SCHEMA
                                    + ":"
                                    + ExternalReferenceConstant.EXTERNAL_REFERENCE_REPORTING_DOCTITLE,
                            (Serializable) externalCurrentDocumentTitleStringList);
                }
                // Disabling events
                disableEvents(temp);
                temp = session.saveDocument(temp);

            }

        }.runUnrestricted();
        DocumentModel finalDocument = session.getDocument(new IdRef(
                document.getId()));

        return finalDocument;

    }

    protected static void disableEvents(final DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE,
                true);
        doc.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);
    }

}
