package org.nuxeo.labs.reference.web;

import java.io.IOException;
import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.labs.reference.operation.AbstractExternalReferenceOperation;


/**
 * This Seam bean manages the external references.
 *
 * @author <a href="mailto:fvadon@nuxeo.com">Fred Vadon</a>
 */
@Name("externalReferences")
@Scope(ScopeType.CONVERSATION)
public class ExternalReferencesBean extends AbstractExternalReferenceOperation implements Serializable {

    public DocumentModelList getExternalReferences(String DocumentId){
        return getExternalReferenceInfo(DocumentId, null);
    }

    public DocumentModelList updateHippoRefsOfNuxeoDocumentBean(String DocumentId) throws IOException{
        return updateHippoRefsOfNuxeoDocument(DocumentId);
    }




    private static final long serialVersionUID = 1L;

}
