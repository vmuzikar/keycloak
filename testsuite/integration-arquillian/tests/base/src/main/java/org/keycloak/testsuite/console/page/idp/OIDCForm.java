package org.keycloak.testsuite.console.page.idp;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OIDCForm extends IdpForm {
    @FindBy(id = "authorizationUrl")
    private WebElement authorizationUrlInput;

    @FindBy(id = "tokenUrl")
    private WebElement tokenUrlInput;

    public String getAuthorizationUrl() {
        return getInputValue(authorizationUrlInput);
    }

    public void setAuthorizationUrl(String value) {
        setInputValue(authorizationUrlInput, value);
    }

    public String getTokenUrl() {
        return getInputValue(tokenUrlInput);
    }

    public void setTokenUrl(String value) {
        setInputValue(tokenUrlInput, value);
    }
}
