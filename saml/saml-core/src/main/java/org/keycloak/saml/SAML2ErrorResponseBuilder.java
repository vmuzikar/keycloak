package org.keycloak.saml;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.factories.JBossSAMLAuthnResponseFactory;
import org.keycloak.saml.processing.core.saml.v2.holders.IDPInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.holders.IssuerInfoHolder;
import org.keycloak.saml.processing.core.saml.v2.holders.SPInfoHolder;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2ErrorResponseBuilder {

    protected String status;
    protected String destination;
    protected String issuer;

    public SAML2ErrorResponseBuilder status(String status) {
        this.status = status;
        return this;
    }

    public SAML2ErrorResponseBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2ErrorResponseBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }


    public Document buildDocument() throws ProcessingException {

        try {
            StatusResponseType statusResponse = new StatusResponseType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());

            statusResponse.setStatus(JBossSAMLAuthnResponseFactory.createStatusTypeForResponder(status));
            NameIDType issuer = new NameIDType();
            issuer.setValue(this.issuer);

            statusResponse.setIssuer(issuer);
            statusResponse.setDestination(destination);

            SAML2Response saml2Response = new SAML2Response();
            return saml2Response.convert(statusResponse);
        } catch (ConfigurationException e) {
            throw new ProcessingException(e);
        } catch (ParsingException e) {
            throw new ProcessingException(e);
        }

    }


}
