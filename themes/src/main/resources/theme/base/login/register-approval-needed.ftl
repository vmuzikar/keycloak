<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("registrationApprovalTitle")}
    <#elseif section = "header">
        ${msg("registrationApprovalTitle")}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            ${msg("registrationApprovalMsg")}<br />
            <a id="loginRestartLink" href="${url.loginRestartFlowUrl}">${msg("backToLogin")}</a>
        </p>
    </#if>
</@layout.registrationLayout>