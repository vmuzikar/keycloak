package org.keycloak.testsuite.auth.page.login.idp;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.LoginForm;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OIDC extends AbstractIdentityProvider {
    @Page
    private LoginForm loginForm;

    private UserRepresentation user;

    public OIDC() {
        super("oidc");
    }

    @Override
    public void login() {

        if (user == null) {throw new IllegalStateException("User is not set");}
        loginForm.login(user);
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }
}
