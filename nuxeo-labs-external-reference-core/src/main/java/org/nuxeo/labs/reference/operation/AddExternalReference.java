/**
 *
 */

package org.nuxeo.labs.reference.operation;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author fvadon
 */
@Operation(id = AddExternalReference.ID, category = Constants.CAT_SERVICES, label = "Add External Reference", description = "Will store the string Reference for the document UUID passed as a parameter. If the document is a proxy, it will automatically add also the source document. Returns the created entry as a document model (null if nothing is created). This op should probably be run as an Admin, as it needs to manage directories and read documents")
public class AddExternalReference extends AbstractExternalReferenceActions {

    public static final String ID = "AddExternalReference";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;


    @Param(name = "ExternalReference", required = true)
    protected String ExternalReference;

    @Param(name = "DocumentUID", required = true)
    protected String DocumentUID;

    @Param(name = "referenceLabel", required = false)
    protected String referenceLabel;

    @Param(name = "externalSource", required = false)
    protected String externalSource;

    @Param(name = "AddEvenIfAlreadyExist", required = false, values = "false")
    protected boolean AddEvenIfAlreadyExist = false;

    @OperationMethod
    public DocumentModel run() {
        //validateCanManageDirectories(ctx); creates NPE, don't have time for now


        DocumentModel dm = addExternalRef(coreSession, DocumentUID,
                ExternalReference,referenceLabel,externalSource, AddEvenIfAlreadyExist);

        return dm;

    }



}
