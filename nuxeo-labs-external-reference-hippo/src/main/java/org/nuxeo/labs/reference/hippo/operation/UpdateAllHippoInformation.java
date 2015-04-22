/**
 *
 */

package org.nuxeo.labs.reference.hippo.operation;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author fvadon
 */
@Operation(id=UpdateAllHippoInformation.ID, category=Constants.CAT_SERVICES, label="UpdateAllHippoInformation", description="")
public class UpdateAllHippoInformation extends AbstractExternalReferenceHippoActions{

    public static final String ID = "UpdateAllHippoInformation";

    @Context
    protected CoreSession coreSession;

    @OperationMethod
    public DocumentModelList run() {

       try {
        return(updateAllHippoRefsInNuxeo(coreSession));
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
    }
    }

}
