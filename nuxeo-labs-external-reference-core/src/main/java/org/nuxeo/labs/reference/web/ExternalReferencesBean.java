package org.nuxeo.labs.reference.web;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.labs.reference.operation.AbstractExternalReferenceActions;


/**
 * This Seam bean manages the external references.
 *
 * @author <a href="mailto:fvadon@nuxeo.com">Fred Vadon</a>
 */
@Name("externalReferences")
@Scope(ScopeType.CONVERSATION)
public class ExternalReferencesBean extends AbstractExternalReferenceActions implements Serializable {

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public DocumentModelList getExternalReferences(String DocumentId){
        return getExternalReferenceInfo(DocumentId, null);
    }

    public String getExternalReferenceVersion(DocumentModel externalRef){
        return getReferenceVersionLabel(documentManager,externalRef);
    }

    private static final long serialVersionUID = 1L;

}
