package org.keycloak.testsuite.auth.page.login.idp;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractIdentityProvider {

    private String provider;

    @Drone
    protected WebDriver driver;

    public AbstractIdentityProvider(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public abstract void login();

    public abstract String getEmail();

    public abstract String getFirstName();

    public abstract String getLastName();
}