<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:3.0"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
                xmlns:k="urn:jboss:domain:keycloak:1.1"
                xmlns:sec="urn:jboss:domain:security:1.2"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>
    <xsl:variable name="inf" select="'urn:jboss:domain:infinispan:'"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:extensions">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <extension module="org.keycloak.keycloak-server-subsystem"/>
            <extension module="org.keycloak.keycloak-adapter-subsystem"/>
            <extension module="org.keycloak.keycloak-saml-adapter-subsystem"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//ds:datasources">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='datasource']"/>
            <datasource jndi-name="java:jboss/datasources/KeycloakDS" pool-name="KeycloakDS" enabled="true" use-java-context="true">
                <connection-url>jdbc:h2:${jboss.server.data.dir}/keycloak;AUTO_SERVER=TRUE</connection-url>
                <driver>h2</driver>
                <security>
                    <user-name>sa</user-name>
                    <password>sa</password>
                </security>
            </datasource>
            <xsl:apply-templates select="node()[name(.)='drivers']"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//j:profile">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
                <web-context>auth</web-context>
            </subsystem>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.1"/>
            <subsystem xmlns="urn:jboss:domain:keycloak-saml:1.1"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//sec:security-domains">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='security-domain']"/>
            <security-domain name="keycloak">
                <authentication>
                    <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"/>
                </authentication>
            </security-domain>
            <security-domain name="sp" cache-type="default">
                <authentication>
                    <login-module code="org.picketlink.identity.federation.bindings.wildfly.SAML2LoginModule" flag="required"/>
                </authentication>
            </security-domain>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $inf)]">
        <xsl:copy>
            <cache-container name="keycloak" jndi-name="infinispan/Keycloak">
                <local-cache name="realms"/>
                <local-cache name="users"/>
                <local-cache name="sessions"/>
                <local-cache name="loginFailures"/>
            </cache-container>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>