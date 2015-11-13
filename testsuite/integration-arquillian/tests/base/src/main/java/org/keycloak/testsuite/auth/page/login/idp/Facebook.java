package org.keycloak.testsuite.auth.page.login.idp;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Facebook extends AbstractIdentityProvider {
    public static final String CLIENT_ID = "355623747967843";
    public static final String SECRET = "8730c706b2d97b8b37307bd3acd44913";

    public static final String EMAIL = "keycloak_rdbpcee_qe@tfbnw.net";
    public static final String PASSWORD = "keycloak1";
    public static final String FIRST_NAME = "Keycloak";
    public static final String LAST_NAME = "QE";

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "pass")
    private WebElement passwordInput;

    @FindBy(name = "login")
    private WebElement loginButton;

    public Facebook() {
        super("facebook");
    }

    @Override
    public void login() {
        emailInput.sendKeys(EMAIL);
        passwordInput.sendKeys(PASSWORD);
        loginButton.click();
    }

    @Override
    public String getEmail() {
        return EMAIL;
    }

    @Override
    public String getFirstName() {
        return FIRST_NAME;
    }

    @Override
    public String getLastName() {
        return LAST_NAME;
    }
}
