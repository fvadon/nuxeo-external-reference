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
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author fvadon
 */
@Operation(id=GetExternalReferenceInfo.ID, category=Constants.CAT_SERVICES, label="GetExternalReferenceInfo", description="Get the list of entries for any combination of External Reference and DocumentUID")
public class GetExternalReferenceInfo extends AbstractExternalReferenceActions {

    public static final String ID = "GetExternalReferenceInfo";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;


    @Param(name = "ExternalReference", required = false)
    protected String ExternalReference;

    @Param(name = "DocumentUID", required = false)
    protected String DocumentUID;


    @OperationMethod
    public DocumentModelList run() {
      return getExternalReferenceInfo(DocumentUID,ExternalReference);
    }

}
