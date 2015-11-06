package org.keycloak.testsuite.console.page.idp;

import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTableExt;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProviders extends AdminConsoleRealm {
    @FindBy(tagName = "table")
    private IdentityProvidersTable table;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/identity-provider-settings";
    }

    public IdentityProvidersTable table() {
        return table;
    }

    public class IdentityProvidersTable extends DataTableExt<IdentityProviderRepresentation> {
        @FindBy(tagName = "select")
        private Select addProviderSelect;

        @Override
        protected IdentityProviderRepresentation dataRepresentationFactory() {
            return new IdentityProviderRepresentation();
        }

        @Override
        protected String getRepresentationId(IdentityProviderRepresentation representation) {
            return representation.getAlias();
        }

        @Override
        protected void setRepresentationId(String value, IdentityProviderRepresentation representation) {
            representation.setAlias(value);
        }

        public void addProvider(String id) {
            addProviderSelect.selectByVisibleText(id);
        }
    }

    public IdentityProvidersResource identityProvidersResource() {
        return realmResource().identityProviders();
    }
}
