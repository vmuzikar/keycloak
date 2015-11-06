package org.keycloak.testsuite.console.page.idp;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProvider extends IdentityProviders {
    public static final String ID = "id";

    @FindBy(xpath = "//div[@data-ng-controller='IdentityProviderTabCtrl']/ul")
    protected IdentityProviderTabs identityProviderTabs;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + ID + "}";
    }

    public final void setId(String id) {
        setUriParameter(ID, id);
    }

    public String getId() {
        return getUriParameter(ID).toString();
    }

    public IdentityProviderTabs tabs() {
        return identityProviderTabs;
    }

    public class IdentityProviderTabs {

        @FindBy(linkText = "Settings")
        private WebElement settingsLink;
        @FindBy(linkText = "Mappers")
        private WebElement mappersLink;

        public void settings() {
            settingsLink.click();
        }

        public void mappers() {
            mappersLink.click();
        }

    }
}
