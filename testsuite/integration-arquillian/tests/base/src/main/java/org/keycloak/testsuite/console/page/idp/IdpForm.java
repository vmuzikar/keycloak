package org.keycloak.testsuite.console.page.idp;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdpForm extends Form {
    public static final String UPDATE_PROFILE_OFF = "Off";
    public static final String UPDATE_PROFILE_ON = "On";
    public static final String UPDATE_PROFILE_MISSING_INFO = "On missing info";

    @FindBy(id = "redirectUri")
    private WebElement redirectUrl;

    @FindBy(id = "clientId")
    private WebElement clientIdInput;

    @FindBy(id = "clientSecret")
    private WebElement clientSecretInput;

    @FindBy(id = "updateProfileFirstLoginMode")
    private Select updateProfileFirstLoginModeSelect;

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

    public String getUpdateProfileFirstLoginMode() {
        return updateProfileFirstLoginModeSelect.getFirstSelectedOption().getText();
    }

    public void setUpdateProfileFirstLoginMode(String value) {
        updateProfileFirstLoginModeSelect.selectByVisibleText(value);
    }
}
