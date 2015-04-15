package org.nuxeo.labs.reference.hippo.web;

import java.io.IOException;
import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.labs.reference.hippo.operation.AbstractExternalReferenceHippoActions;


/**
 * This Seam bean manages the external references specific to hippo.
 *
 * @author <a href="mailto:fvadon@nuxeo.com">Fred Vadon</a>
 */
@Name("externalReferencesHippo")
@Scope(ScopeType.CONVERSATION)
public class ExternalReferencesHippoBean extends AbstractExternalReferenceHippoActions implements Serializable {

    public DocumentModelList updateHippoRefsOfNuxeoDocumentBean(DocumentModel DocumentId) throws IOException{
        return updateHippoRefsOfNuxeoDocument(DocumentId);
    }

    private static final long serialVersionUID = 1L;

}
