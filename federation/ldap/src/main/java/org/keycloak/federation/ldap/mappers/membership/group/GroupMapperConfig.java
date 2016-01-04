package org.keycloak.federation.ldap.mappers.membership.group;

import java.util.Collection;
import java.util.Collections;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserFederationMapperModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupMapperConfig extends CommonLDAPGroupMapperConfig {

    // LDAP DN where are groups of this tree saved.
    public static final String GROUPS_DN = "groups.dn";

    // Name of LDAP attribute, which is used in group objects for name and RDN of group. Usually it will be "cn"
    public static final String GROUP_NAME_LDAP_ATTRIBUTE = "group.name.ldap.attribute";

    // Object classes of the group object.
    public static final String GROUP_OBJECT_CLASSES = "group.object.classes";

    // Flag whether group inheritance from LDAP should be propagated to Keycloak group inheritance.
    public static final String PRESERVE_GROUP_INHERITANCE = "preserve.group.inheritance";

    // Customized LDAP filter which is added to the whole LDAP query
    public static final String GROUPS_LDAP_FILTER = "groups.ldap.filter";

    // Name of attributes of the LDAP group object, which will be mapped as attributes of Group in Keycloak
    public static final String MAPPED_GROUP_ATTRIBUTES = "mapped.group.attributes";

    // During sync of groups from LDAP to Keycloak, we will keep just those Keycloak groups, which still exists in LDAP. Rest will be deleted
    public static final String DROP_NON_EXISTING_GROUPS_DURING_SYNC = "drop.non.existing.groups.during.sync";

    // See UserRolesRetrieveStrategy
    public static final String LOAD_GROUPS_BY_MEMBER_ATTRIBUTE = "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE";
    public static final String GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE = "GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE";
    public static final String LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY = "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY";

    public GroupMapperConfig(UserFederationMapperModel mapperModel) {
        super(mapperModel);
    }


    public String getGroupsDn() {
        String groupsDn = mapperModel.getConfig().get(GROUPS_DN);
        if (groupsDn == null) {
            throw new ModelException("Groups DN is null! Check your configuration");
        }
        return groupsDn;
    }

    @Override
    public String getLDAPGroupsDn() {
        return getGroupsDn();
    }

    public String getGroupNameLdapAttribute() {
        String rolesRdnAttr = mapperModel.getConfig().get(GROUP_NAME_LDAP_ATTRIBUTE);
        return rolesRdnAttr!=null ? rolesRdnAttr : LDAPConstants.CN;
    }

    @Override
    public String getLDAPGroupNameLdapAttribute() {
        return getGroupNameLdapAttribute();
    }

    public boolean isPreserveGroupsInheritance() {
        return AbstractLDAPFederationMapper.parseBooleanParameter(mapperModel, PRESERVE_GROUP_INHERITANCE);
    }

    public String getMembershipLdapAttribute() {
        String membershipAttrName = mapperModel.getConfig().get(MEMBERSHIP_LDAP_ATTRIBUTE);
        return membershipAttrName!=null ? membershipAttrName : LDAPConstants.MEMBER;
    }

    public Collection<String> getGroupObjectClasses(LDAPFederationProvider ldapProvider) {
        String objectClasses = mapperModel.getConfig().get(GROUP_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }

        return getConfigValues(objectClasses);
    }

    public Collection<String> getGroupAttributes() {
        String groupAttrs = mapperModel.getConfig().get(MAPPED_GROUP_ATTRIBUTES);
        return (groupAttrs == null) ? Collections.<String>emptySet() : getConfigValues(groupAttrs);
    }

    public String getCustomLdapFilter() {
        return mapperModel.getConfig().get(GROUPS_LDAP_FILTER);
    }

    public boolean isDropNonExistingGroupsDuringSync() {
        return AbstractLDAPFederationMapper.parseBooleanParameter(mapperModel, DROP_NON_EXISTING_GROUPS_DURING_SYNC);
    }

    public String getUserGroupsRetrieveStrategy() {
        String strategyString = mapperModel.getConfig().get(USER_ROLES_RETRIEVE_STRATEGY);
        return strategyString!=null ? strategyString : LOAD_GROUPS_BY_MEMBER_ATTRIBUTE;
    }
}
