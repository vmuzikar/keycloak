package org.keycloak.protocol.saml.installation;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlClientInstallation implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        SamlClient samlClient = new SamlClient(client);
        StringBuffer buffer = new StringBuffer();
        buffer.append("<keycloak-saml-adapter>\n");
        buffer.append("    <SP entityID=\"").append(client.getClientId()).append("\"\n");
        buffer.append("        sslPolicy=\"").append(realm.getSslRequired().name()).append("\"\n");
        buffer.append("        logoutPage=\"SPECIFY YOUR LOGOUT PAGE!\">\n");
        if (samlClient.requiresClientSignature() || samlClient.requiresEncryption()) {
            buffer.append("        <Keys>\n");
            if (samlClient.requiresClientSignature()) {
                buffer.append("            <Key signing=\"true\">\n");
                buffer.append("                <PrivateKeyPem>\n");
                if (samlClient.getClientSigningPrivateKey() == null) {
                    buffer.append("                    PRIVATE KEY NOT SET UP OR KNOWN\n");
                } else {
                    buffer.append("                    ").append(samlClient.getClientSigningPrivateKey()).append("\n");
                }
                buffer.append("                </PrivateKeyPem>\n");
                buffer.append("                <CertificatePem>\n");
                if (samlClient.getClientSigningCertificate() == null) {
                    buffer.append("                    YOU MUST CONFIGURE YOUR CLIENT's SIGNING CERTIFICATE\n");
                } else {
                    buffer.append("                    ").append(samlClient.getClientSigningCertificate()).append("\n");
                }
                buffer.append("                </CertificatePem>\n");
                buffer.append("            </Key>\n");
            }
            if (samlClient.requiresEncryption()) {
                buffer.append("            <Key encryption=\"true\">\n");
                buffer.append("                <PrivateKeyPem>\n");
                if (samlClient.getClientEncryptingPrivateKey() == null) {
                    buffer.append("                    PRIVATE KEY NOT SET UP OR KNOWN\n");
                } else {
                    buffer.append("                    ").append(samlClient.getClientEncryptingPrivateKey()).append("\n");
                }
                buffer.append("                </PrivateKeyPem>\n");
                buffer.append("            </Key>\n");

            }
            buffer.append("        </Keys>\n");
        }
        buffer.append("        <IDP entityID=\"idp\"");
        if (samlClient.requiresClientSignature()) {
            buffer.append("\n             signatureAlgorithm=\"").append(samlClient.getSignatureAlgorithm()).append("\"");
            if (samlClient.getCanonicalizationMethod() != null) {
                buffer.append("\n             signatureCanonicalizationMethod=\"").append(samlClient.getCanonicalizationMethod()).append("\"");
            }
        }
        buffer.append(">\n");
        buffer.append("            <SingleSignOnService signRequest=\"").append(Boolean.toString(samlClient.requiresClientSignature())).append("\"\n");
        buffer.append("                                 validateResponseSignature=\"").append(Boolean.toString(samlClient.requiresRealmSignature())).append("\"\n");
        buffer.append("                                 requestBinding=\"POST\"\n");
        UriBuilder bindingUrlBuilder = UriBuilder.fromUri(baseUri);
        String bindingUrl = RealmsResource.protocolUrl(bindingUrlBuilder)
                .build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString();
        buffer.append("                                 bindingUrl=\"").append(bindingUrl).append("\"/>\n");

        buffer.append("            <SingleLogoutService signRequest=\"").append(Boolean.toString(samlClient.requiresClientSignature())).append("\"\n");
        buffer.append("                                 signResponse=\"").append(Boolean.toString(samlClient.requiresClientSignature())).append("\"\n");
        buffer.append("                                 validateRequestSignature=\"").append(Boolean.toString(samlClient.requiresRealmSignature())).append("\"\n");
        buffer.append("                                 validateResponseSignature=\"").append(Boolean.toString(samlClient.requiresRealmSignature())).append("\"\n");
        buffer.append("                                 requestBinding=\"POST\"\n");
        buffer.append("                                 responseBinding=\"POST\"\n");
        buffer.append("                                 postBindingUrl=\"").append(bindingUrl).append("\"\n");
        buffer.append("                                 redirectBindingUrl=\"").append(bindingUrl).append("\"");
        buffer.append("/>\n");
        if (samlClient.requiresRealmSignature()) {
            buffer.append("            <Keys>\n");
            buffer.append("                <Key signing=\"true\">\n");
            buffer.append("                    <CertificatePem>\n");
            buffer.append("                       ").append(realm.getCertificatePem()).append("\n");
            buffer.append("                    </CertificatePem>\n");
            buffer.append("                </Key>\n");
            buffer.append("            </Keys>\n");
        }
        buffer.append("        </IDP>\n");
        buffer.append("    </SP>\n");
        buffer.append("</keycloak-saml-adapter>\n");
        return Response.ok(buffer.toString(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Keycloak SAML Adapter keycloak-saml.xml";
    }

    @Override
    public String getHelpText() {
        return "Keycloak SAML adapter configuration file.  Put this in WEB-INF directory if your WAR.";
    }

    @Override
    public String getFilename() {
        return "keycloak-saml.xml";
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "keycloak-saml";
    }
}
