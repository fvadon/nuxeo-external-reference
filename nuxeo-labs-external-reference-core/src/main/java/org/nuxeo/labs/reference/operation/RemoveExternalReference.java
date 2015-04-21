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

/**
 * @author fvadon
 */
@Operation(id=RemoveExternalReference.ID, category=Constants.CAT_SERVICES, label="RemoveExternalReference", description="Remove the entries corresponding the tuple formed by ExternalRef and DocumentUID, if only of them is provided, then remove all entries with that item. SHould probably be run as an Admin, not check is done on permissions")
public class RemoveExternalReference extends AbstractExternalReferenceActions{

    public static final String ID = "RemoveExternalReference";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;


    @Param(name = "ExternalReference", required = false)
    protected String ExternalReference;

    @Param(name = "DocumentUID", required = false)
    protected String DocumentUID;

    @OperationMethod
    public void run() {
        removeExternalReference(coreSession,DocumentUID,ExternalReference);

    }



}
