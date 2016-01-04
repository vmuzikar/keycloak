package org.keycloak.federation.ldap.mappers;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.EqualCondition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;

/**
 * Mapper useful for the LDAP deployments when some attribute (usually CN) is mapped to full name of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapper extends AbstractLDAPFederationMapper {

    public static final String LDAP_FULL_NAME_ATTRIBUTE = "ldap.full.name.attribute";
    public static final String READ_ONLY = "read.only";

    public FullNameLDAPFederationMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        super(mapperModel, ldapProvider, realm);
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate) {
        String ldapFullNameAttrName = getLdapFullNameAttrName();
        String fullName = ldapUser.getAttributeAsString(ldapFullNameAttrName);
        if (fullName == null) {
            return;
        }

        fullName = fullName.trim();
        if (!fullName.isEmpty()) {
            int lastSpaceIndex = fullName.lastIndexOf(" ");
            if (lastSpaceIndex == -1) {
                user.setLastName(fullName);
            } else {
                user.setFirstName(fullName.substring(0, lastSpaceIndex));
                user.setLastName(fullName.substring(lastSpaceIndex + 1));
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser) {
        String ldapFullNameAttrName = getLdapFullNameAttrName();
        String fullName = getFullName(localUser.getFirstName(), localUser.getLastName());
        ldapUser.setSingleAttribute(ldapFullNameAttrName, fullName);

        if (isReadOnly()) {
            ldapUser.addReadOnlyAttributeName(ldapFullNameAttrName);
        }
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate) {
        if (ldapProvider.getEditMode() == UserFederationProvider.EditMode.WRITABLE && !isReadOnly()) {


            TxAwareLDAPUserModelDelegate txDelegate = new TxAwareLDAPUserModelDelegate(delegate, ldapProvider, ldapUser) {

                @Override
                public void setFirstName(String firstName) {
                    super.setFirstName(firstName);
                    setFullNameToLDAPObject();
                }

                @Override
                public void setLastName(String lastName) {
                    super.setLastName(lastName);
                    setFullNameToLDAPObject();
                }

                private void setFullNameToLDAPObject() {
                    String fullName = getFullName(getFirstName(), getLastName());
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Pushing full name attribute to LDAP. Full name: %s", fullName);
                    }

                    ensureTransactionStarted();

                    String ldapFullNameAttrName = getLdapFullNameAttrName();
                    ldapUser.setSingleAttribute(ldapFullNameAttrName, fullName);
                }

            };

            return txDelegate;
        } else {
            return delegate;
        }
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        String ldapFullNameAttrName = getLdapFullNameAttrName();
        query.addReturningLdapAttribute(ldapFullNameAttrName);

        // Change conditions and compute condition for fullName from the conditions for firstName and lastName. Right now just "equal" condition is supported
        EqualCondition firstNameCondition = null;
        EqualCondition lastNameCondition = null;
        Set<Condition> conditionsCopy = new HashSet<Condition>(query.getConditions());
        for (Condition condition : conditionsCopy) {
            String paramName = condition.getParameterName();
            if (paramName != null) {
                if (paramName.equals(UserModel.FIRST_NAME)) {
                    firstNameCondition = (EqualCondition) condition;
                    query.getConditions().remove(condition);
                } else if (paramName.equals(UserModel.LAST_NAME)) {
                    lastNameCondition = (EqualCondition) condition;
                    query.getConditions().remove(condition);
                } else if (paramName.equals(LDAPConstants.GIVENNAME)) {
                    // Some previous mapper already converted it to LDAP name
                    firstNameCondition = (EqualCondition) condition;
                } else if (paramName.equals(LDAPConstants.SN)) {
                    // Some previous mapper already converted it to LDAP name
                    lastNameCondition = (EqualCondition) condition;
                }
            }
        }


        String fullName = null;
        if (firstNameCondition != null && lastNameCondition != null) {
            fullName = firstNameCondition.getValue() + " " + lastNameCondition.getValue();
        } else if (firstNameCondition != null) {
            fullName = (String) firstNameCondition.getValue();
        } else if (lastNameCondition != null) {
            fullName = (String) lastNameCondition.getValue();
        } else {
            return;
        }
        EqualCondition fullNameCondition = new EqualCondition(ldapFullNameAttrName, fullName);
        query.addWhereCondition(fullNameCondition);
    }

    protected String getLdapFullNameAttrName() {
        String ldapFullNameAttrName = mapperModel.getConfig().get(LDAP_FULL_NAME_ATTRIBUTE);
        return ldapFullNameAttrName == null ? LDAPConstants.CN : ldapFullNameAttrName;
    }

    protected String getFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return LDAPConstants.EMPTY_ATTRIBUTE_VALUE;
        }
    }

    private boolean isReadOnly() {
        return parseBooleanParameter(mapperModel, READ_ONLY);
    }
}
