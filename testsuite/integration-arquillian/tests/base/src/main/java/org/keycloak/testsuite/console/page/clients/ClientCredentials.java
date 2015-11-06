package org.keycloak.testsuite.console.page.clients;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 * TODO: Add support for Signed Jwt
 */
public class ClientCredentials extends Client {

    @FindBy(id = "secret")
    private WebElement secret;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/credentials";
    }

    public String getSecret() {
        return Form.getInputValue(secret);
    }

}
