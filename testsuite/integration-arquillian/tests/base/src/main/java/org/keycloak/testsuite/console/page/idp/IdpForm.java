package org.keycloak.testsuite.console.page.idp;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdpForm extends Form {
    public static final String FIRST_BROKER_LOGIN = "first broker login";

    @FindBy(id = "redirectUri")
    private WebElement redirectUrl;

    @FindBy(id = "clientId")
    private WebElement clientIdInput;

    @FindBy(id = "clientSecret")
    private WebElement clientSecretInput;

    @FindBy(id = "firstBrokerLoginFlowAlias")
    private Select firstLoginFlowSelect;

    public String getRedirectUrl() {
        return getInputValue(redirectUrl);
    }

    public String getClientId() {
        return getInputValue(clientIdInput);
    }

    public void setClientId(String value) {
        setInputValue(clientIdInput, value);
    }

    public String getClientSecret() {
        return getInputValue(clientSecretInput);
    }

    public void setClientSecret(String value) {
        setInputValue(clientSecretInput, value);
    }

    public String getFirstLoginFlow() {
        return firstLoginFlowSelect.getFirstSelectedOption().getText();
    }

    public void setFirstLoginFlow(String value) {
        firstLoginFlowSelect.selectByVisibleText(value);
    }
}
