package org.keycloak.representations;

import org.keycloak.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshToken extends AccessToken {

    private RefreshToken() {
        type(TokenUtil.TOKEN_TYPE_REFRESH);
    }

    /**
     * Deep copies issuer, subject, issuedFor, sessionState, realmAccess, and resourceAccess
     * from AccessToken.
     *
     * @param token
     */
    public RefreshToken(AccessToken token) {
        this();
        this.clientSession = token.getClientSession();
        this.issuer = token.issuer;
        this.subject = token.subject;
        this.issuedFor = token.issuedFor;
        this.sessionState = token.sessionState;
        this.nonce = token.nonce;
        if (token.realmAccess != null) {
            realmAccess = token.realmAccess.clone();
        }
        if (token.resourceAccess != null) {
            resourceAccess = new HashMap<String, Access>();
            for (Map.Entry<String, Access> entry : token.resourceAccess.entrySet()) {
                resourceAccess.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }
}
