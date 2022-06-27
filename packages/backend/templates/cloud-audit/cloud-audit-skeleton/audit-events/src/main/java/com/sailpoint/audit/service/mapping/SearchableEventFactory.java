/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */
package com.sailpoint.audit.service.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.search.util.MapUtils;
import com.sailpoint.audit.AuditActions;
import com.sailpoint.audit.AuditEventConstants;
import com.sailpoint.audit.service.model.SearchableEvent;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.sailpoint.audit.AuditEventConstants.*;

public class SearchableEventFactory {

	private static final Map<String, SearchableEvent> EVENTS = ImmutableMap.<String, SearchableEvent>builder()
		.put(AuditEventActions.CONNECTOR_FILE_DELETE, event(objects(CONNECTOR, FILE), DELETE))
		.put(AuditEventActions.CONNECTOR_FILE_UPLOAD, event(objects(CONNECTOR, FILE), UPLOAD))
		.put(AuditEventActions.SOURCE_ACCOUNT_AGGREGATION, event(objects(SOURCE, ACCOUNT), AGGREGATE, STARTED)) // deprecated
		.put(AuditEventActions.SOURCE_AGGREGATION_TERMINATED, event(objects(SOURCE, ACCOUNT), AGGREGATE, TERMINATED)) // deprecated
		.put(AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_STARTED, event(objects(SOURCE, ACCOUNT), AGGREGATE, STARTED))
		.put(AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_PASSED, event(objects(SOURCE, ACCOUNT), AGGREGATE, PASSED))
		.put(AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_FAILED, event(objects(SOURCE, ACCOUNT), AGGREGATE, FAILED))
		.put(AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_TERMINATED, event(objects(SOURCE, ACCOUNT), AGGREGATE, TERMINATED))
		.put(AuditEventActions.SOURCE_ACCOUNTS_EXPORT, event(objects(SOURCE, ACCOUNT), EXPORT))
		.put(AuditEventActions.SOURCE_ACCOUNT_FEED_DOWNLOAD, event(objects(SOURCE, ACCOUNT, FEED), DOWNLOAD))
		.put(AuditEventActions.SOURCE_ACTIVITY_EXPORT, event(objects(SOURCE, ACTIVITY), EXPORT))
		.put(AuditEventActions.SOURCE_CREATE, event(objects(SOURCE), CREATE))
		.put(AuditEventActions.SOURCE_DELETE, event(objects(SOURCE), DELETE))
		.put(AuditEventActions.SOURCE_RESET, event(objects(SOURCE), RESET))
		.put(AuditEventActions.SOURCE_UPDATE, event(objects(SOURCE), UPDATE))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_CREATED, event(objects(SERVICE, DESK, INTEGRATION), CREATE))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_CREATE_FAILED, event(objects(SERVICE, DESK, INTEGRATION), CREATE, FAILED))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATED, event(objects(SERVICE, DESK, INTEGRATION), UPDATE))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATE_FAILED, event(objects(SERVICE, DESK, INTEGRATION), UPDATE, FAILED))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_DELETED, event(objects(SERVICE, DESK, INTEGRATION), DELETE))
		.put(AuditEventActions.SERVICE_DESK_INTEGRATION_DELETE_FAILED, event(objects(SERVICE, DESK, INTEGRATION), DELETE, FAILED))
		.put(AuditEventActions.SOURCE_FEATURES_STRING_UPDATE, event(objects(SOURCE, FEATURES_STRING), UPDATE))
		.put(AuditEventActions.SOURCE_ENTITLEMENT_AGGREGATION, event(objects(SOURCE, ENTITLMENT), AGGREGATE))
		.put(AuditEventActions.SOURCE_ENTITLEMENT_IMPORT, event(objects(SOURCE, ENTITLMENT), IMPORT))
		.put(AuditEventActions.SOURCE_ENTITLEMENT_EXPORT, event(objects(SOURCE, ENTITLMENT), EXPORT))
		.put(AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_ACTIVITY_EXPORT, event(objects(SOURCE, EXTERNAL, PASSWORD, CHANGE, ACTIVITY), EXPORT))
		.put(AuditEventActions.SOURCE_SCHEMA_ATTRIBUTE_ADDED, event(objects(SOURCE, SCHEMA, ATTRIBUTE), ADD))
		.put(AuditEventActions.SOURCE_SCHEMA_ATTRIBUTES_DELETED, event(objects(SOURCE, SCHEMA, ATTRIBUTE), DELETE))
		.put(AuditActions.SOURCE_UPDATE_DELETE_THRESHOLD.toString(), event(objects(SOURCE, DELETE, THRESHOLD), UPDATE))
		.put(AuditActions.SOURCE_SCHEMA_ATTRIBUTES_UPDATED.toString(), event(objects(SOURCE, SCHEMA), UPDATE))
		.put(AuditActions.SOURCE_AGGREGATION_SCHEDULE_UPDATED.toString(), event(objects(SOURCE, AGGREGATION, SCHEDULE), UPDATE))
		.put(AuditActions.CONNECTOR_CREATE.toString(), event(objects(CUSTOM, CONNECTOR), CREATE))
		.put(AuditActions.CONNECTOR_DELETE.toString(), event(objects(CUSTOM, CONNECTOR), DELETE))
		.put(AuditActions.CONNECTOR_EXPORT.toString(), event(objects(CUSTOM, CONNECTOR), EXPORT))
		.put(AuditActions.CONNECTOR_UPDATE.toString(), event(objects(CUSTOM, CONNECTOR), UPDATE))
		.put(AuditActions.SOURCE_RENAME_FAILURE_DETECTED.toString(), event(objects(SOURCE), RENAME, FAILED))

		//From CC
		.put(AuditEventActions.ACCESS_PROFILE_CREATE, event(objects(ACCESS, PROFILE), CREATE))
		.put(AuditEventActions.ACCESS_PROFILE_DELETE, event(objects(ACCESS, PROFILE), DELETE))
		.put(AuditEventActions.ACCESS_PROFILE_UPDATE, event(objects(ACCESS, PROFILE), UPDATE))

		//From Mantis
		.put(AuditActions.ACCESS_PROFILE_CREATE_PASSED.toString(), event(objects(ACCESS, PROFILE), CREATE))
		.put(AuditActions.ACCESS_PROFILE_DELETE_PASSED.toString(), event(objects(ACCESS, PROFILE), DELETE))
		.put(AuditActions.ACCESS_PROFILE_UPDATE_PASSED.toString(), event(objects(ACCESS, PROFILE), UPDATE))

		.put(AuditActions.CERT_CAMPAIGN_COMPLETE.toString(), event(objects(CERTIFICATION, CAMPAIGN), COMPLETE))
		.put(AuditActions.CertificationCampaignDelete.toString(), event(objects(CERTIFICATION, CAMPAIGN), DELETE))
		.put(AuditActions.CertificationCampaignCreate.toString(), event(objects(CERTIFICATION, CAMPAIGN), CREATE))
		.put(AuditActions.CertificationCampaignComplete.toString(), event(objects(CERTIFICATION, CAMPAIGN), COMPLETE))
		.put(AuditActions.CertificationCampaignActivate.toString(), event(objects(CERTIFICATION, CAMPAIGN), ACTIVATE))
		.put(AuditActions.CampaignFilterCreate.toString(), event(objects(CERTIFICATION, CAMPAIGN, FILTER), CREATE))
		.put(AuditActions.CampaignFilterDelete.toString(), event(objects(CERTIFICATION, CAMPAIGN, FILTER), DELETE))
		.put(AuditActions.CampaignFilterUpdate.toString(), event(objects(CERTIFICATION, CAMPAIGN, FILTER), UPDATE))
		.put(AuditActions.STUCK_PENDING_CAMPAIGN_DETECTED.toString(), event(objects(CERTIFICATION, CAMPAIGN), STUCK, DETECTED))
		.put(AuditActions.CERT_CAMPAIGN_REVIEW_NOTIFICATION.toString(), event(objects(CERTIFICATION, CAMPAIGN), NOTIFY))
		.put(AuditActions.CERTIFICATIONSFINISHED.toString(), event(objects(CERTIFICATION, CAMPAIGN), FINISH))
		.put(AuditEvent.ActionSignoff, event(objects(CERTIFICATION), SIGNOFF))
		.put(AuditEvent.ActionReassign, event(objects(CERTIFICATION), REASSIGN))
		.put(AuditActions.CERT_ITEM_TIMESTAMP_STATS.toString(), event(objects(CERTIFICATION, ITEM, CREATION, STATS), LOG))
		.put(AuditEvent.ActionCertificationsPhased, event(objects(CERTIFICATION, PHASE), CHANGE))
		.put(AuditEvent.ActionRemediate, event(objects(CERTIFICATION, ITEM), REMEDIATE))
		.put(AuditActions.RULE_CREATE_PASSED.toString(), event(objects(RULE), CREATE))
		.put(AuditActions.RULE_UPDATE_PASSED.toString(), event(objects(RULE), UPDATE))
		.put(AuditActions.RULE_DELETE_PASSED.toString(), event(objects(RULE), DELETE))

		.put(AuditEventActions.ACCESS_REQUEST_REQUESTED, event(objects(ACCESS), REQUEST, STARTED))
		.put(AuditEventActions.ACCESS_REQUEST_APPROVED, event(objects(ACCESS), REQUEST, APPROVED))
		.put(AuditEventActions.ACCESS_REQUEST_REJECTED, event(objects(ACCESS), REQUEST, REJECTED))
		.put(AuditEventActions.ACCESS_REQUEST_FORWARDED, event(objects(ACCESS), REQUEST, FORWARDED))
		.put(AuditEventActions.ACCESS_REQUEST_ESCALATE, event(objects(ACCESS), REQUEST, ESCALATED))
		.put(AuditEventActions.ACCESS_REQUEST_ESCALATED, event(objects(ACCESS), REQUEST, ESCALATED))
		.put(AuditEventActions.ACCESS_REQUEST_CANCELLED, event(objects(ACCESS), REQUEST, CANCELLED))
		.put(AuditEventActions.ACCESS_REQUEST_PROCESSED, event(objects(ACCESS), REQUEST, PROCESSED))

		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_REQUESTED, event(objects(ACCESS), REVOKE, STARTED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_APPROVED, event(objects(ACCESS), REVOKE, APPROVED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_REJECTED, event(objects(ACCESS), REVOKE, REJECTED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_FORWARDED, event(objects(ACCESS), REVOKE, FORWARDED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_ESCALATED, event(objects(ACCESS), REVOKE, ESCALATED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_CANCELLED, event(objects(ACCESS), REVOKE, CANCELLED))
		.put(AuditEventActions.ACCESS_REVOKE_REQUEST_PROCESSED, event(objects(ACCESS), REVOKE, PROCESSED))

		.put(AuditEventActions.EnableAccount, event(objects(ACCOUNT), ENABLE))
		.put(AuditEventActions.EnableAccountFailure, event(objects(ACCOUNT), ENABLE, FAILED))
		.put(AuditEventActions.ModifyAccount, event(objects(ACCOUNT), MODIFY))
		.put(AuditEventActions.ModifyAccountFailure, event(objects(ACCOUNT), MODIFY, FAILED))
		.put(AuditEventActions.RequestApp, event(objects(APP), REQUEST))
		.put(AuditEventActions.RemoveEntitlement, event(objects(ENTITLMENT), REMOVE))
		.put(AuditEventActions.RemoveEntitlementFailure, event(objects(ENTITLMENT), REMOVE, FAILED))
		.put(AuditEventActions.UnlockAccount, event(objects(ACCOUNT), UNLOCK))
		.put(AuditEventActions.UnlockAccountFailure, event(objects(ACCOUNT), UNLOCK, FAILED))
		.put(AuditEventActions.ACTION_STATE_CHANGE, event(objects(IDENTITY, STATE), CHANGE))
		.put(AuditEventActions.ACTION_APP_REQUEST_APPROVED, event(objects(APP), REQUEST, APPROVED))
		.put(AuditEventActions.ACTION_APP_REQUEST_REJECTED, event(objects(APP), REQUEST, REJECTED))
		.put(AuditActions.IDENTITY_PROFILE_UPDATE.toString(), event(objects(IDENTITY, PROFILE), UPDATE))

		.put(AuditActions.USER_ACTIVATE.toString(), event(objects(USER), ACTIVATE))
		.put(AuditActions.USER_ACTIVITY_EXPORT.toString(), event(objects(USER, ACTIVITY), EXPORT))
		.put(AuditActions.USER_ADMIN_GRANT.toString(), event(objects(USER, ROLE, ADMIN), GRANT))
		.put(AuditActions.USER_ADMIN_REVOKE.toString(), event(objects(USER, ROLE, ADMIN), REVOKE))
		.put(AuditActions.USER_DELETE.toString(), event(objects(USER), DELETE))
		.put(AuditActions.USER_EMAIL_UPDATE.toString(), event(objects(USER, EMAIL), UPDATE))
		.put(AuditActions.USER_HELPDESK_GRANT.toString(), event(objects(USER, ROLE, HELPDESK), GRANT))
		.put(AuditActions.USER_HELPDESK_REVOKE.toString(), event(objects(USER, ROLE, HELPDESK), REVOKE))
		.put(AuditActions.USER_INVITE.toString(), event(objects(USER), INVITE))
		.put(AuditActions.USER_KBA_ANSWER_DELETE.toString(), event(objects(USER, KBA), DELETE))
		.put(AuditActions.USER_KBA_ANSWER_UPDATE.toString(), event(objects(USER, KBA), UPDATE))
		.put(AuditActions.USER_KBA_ANSWER_UPDATE_NOTIFICATION.toString(), event(objects(USER, KBA), NOTIFY))
		.put(AuditActions.USER_KBA_ANSWERS.toString(), event(objects(USER, KBA), ANSWERS))
		.put(AuditActions.USER_LOCK.toString(), event(objects(USER), LOCK))
		.put(AuditActions.USER_PASSWORD_RESET_REJECTED.toString(), event(objects(USER, PASSWORD), RESET, REJECTED))
		.put(AuditActions.USER_PASSWORD_UPDATE.toString(), event(objects(USER, PASSWORD), UPDATE, STARTED))
		.put(AuditActions.USER_PASSWORD_UPDATE_PASSED.toString(), event(objects(USER, PASSWORD), UPDATE))
		.put(AuditActions.USER_PASSWORD_UPDATE_FAILED.toString(), event(objects(USER, PASSWORD), UPDATE, FAILED))
		.put(AuditActions.USER_PHONE_UPDATE.toString(), event(objects(USER, PHONE), UPDATE))
		.put(AuditActions.USER_REGISTRATION.toString(), event(objects(USER), REGISTER))
		.put(AuditActions.USER_REGISTRATION_FAILURE.toString(), event(objects(USER), REGISTER, FAILED))
		.put(AuditActions.USER_REGISTRATION_LINK.toString(), event(objects(USER), REGISTER, STARTED))
		.put(AuditActions.USER_RESET.toString(), event(objects(USER), RESET))
		.put(AuditActions.USER_STEP_UP_AUTH.toString(), event(objects(USER, AUTHENTICATION, STEP_UP), SETUP))
		.put(AuditActions.USER_STEP_UP_AUTH_FAILURE.toString(), event(objects(USER, AUTHENTICATION, STEP_UP), SETUP, FAILED))
		.put(AuditActions.USER_UNLOCK.toString(), event(objects(USER), UNLOCK))
		.put(AuditActions.USER_UNLOCK_REJECTED.toString(), event(objects(USER), UNLOCK))

		//PASSWORD_ACTIVITY
		.put(AuditEvent.PasswordChange, event(objects(PASSWORD), CHANGE, STARTED))
		.put(AuditEvent.PasswordChangeFailure, event(objects(PASSWORD), CHANGE, FAILED))
		.put(AuditEventActions.ACTION_PASSWORD_CHANGE_SUCCESS, event(objects(PASSWORD, ACTION), CHANGE))
		.put(AuditEventActions.PasswordsRequestStart, event(objects(PASSWORD), REQUEST, STARTED))
		.put(AuditEventActions.ExpirePasswordStart, event(objects(PASSWORD, EXPIRED), CHANGE, STARTED))
		.put(AuditEvent.ExpiredPasswordChange, event(objects(PASSWORD, EXPIRED), CHANGE))
		.put(AuditEvent.ForgotPasswordChange, event(objects(PASSWORD, FORGOT), CHANGE))
		.put(AuditEventActions.ForgotPasswordStart, event(objects(PASSWORD, FORGOT), CHANGE, STARTED))
		.put(AuditEventActions.ACCOUNT_PASSWORD_RESET_FAILED, event(objects(ACCOUNT, PASSWORD), RESET, FAILED))
		.put(AuditEventActions.ACCOUNT_PASSWORD_RESET_PASSED, event(objects(ACCOUNT, PASSWORD), RESET))
		.put(AuditEventActions.ACCOUNT_PASSWORD_SYNC_PASSED, event(objects(ACCOUNT, PASSWORD), SYNC))
		.put(AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE, event(objects(SOURCE, PASSWORD), CHANGE, STARTED))
		.put(AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_FAILED, event(objects(SOURCE, PASSWORD), CHANGE, FAILED))
		.put(AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_PASSED, event(objects(SOURCE, PASSWORD), CHANGE))
		.put(AuditActions.SOURCE_PASSWORD_INTERCEPT_IGNORED.toString(), event(objects(SOURCE, PASSWORD), INTERCEPT, IGNORED))
		.put(AuditActions.SOURCE_PASSWORD_INTERCEPT_PROCESSED.toString(), event(objects(SOURCE, PASSWORD), INTERCEPT, PROCESSED))

		.put(AuditActions.ACCOUNT_VAULT_UPDATED.toString(), event(objects(ACCOUNT, VAULT), UPDATE))
		.put(AuditActions.SOURCE_PASSWORD_POLICY_ASSIGNED.toString(), event(objects(PASSWORD, POLICY), ASSIGN))
		.put(AuditActions.REDIRECT_PATTERNS_UPDATED.toString(), event(objects(SAML, REDIRECT, PATTERNS), UPDATE))
		.put(AuditActions.PASSWORD_POLICY_CREATED.toString(), event(objects(PASSWORD, POLICY), CREATE))
		.put(AuditActions.PASSWORD_POLICY_DELETED.toString(), event(objects(PASSWORD, POLICY), DELETE))
		.put(AuditActions.PASSWORD_REPLAY_ENABLED.toString(), event(objects(PASSWORD, REPLAY), ENABLE))
		.put(AuditActions.INTEGRATION_ENABLED.toString(), event(objects(STRONG, AUTHENTICATION, INTEGRATION), ENABLE))
		.put(AuditActions.PASSWORD_REPLAY_PASSIVE.toString(), event(objects(PASSWORD, REPLAY, PASSIVE), LAUNCH))
		.put(AuditActions.PASSWORD_POLICY_UPDATED.toString(), event(objects(PASSWORD, POLICY), UPDATE))
		.put(AuditActions.INTEGRATION_DISABLED.toString(), event(objects(STRONG, AUTHENTICATION, INTEGRATION), DISABLE))
		.put(AuditActions.INTEGRATION_UPDATED.toString(), event(objects(STRONG, AUTHENTICATION, INTEGRATION), UPDATE))
		.put(AuditActions.ACCOUNT_FULL_RESET.toString(), event(objects(ACCOUNT, VAULT), RESET))
		.put(AuditActions.MFA_VERIFICATION_FAILED.toString(), event(objects(MFA), VERIFICATION, FAILED))
		.put(AuditActions.PASSWORD_REPLAY_DISABLED.toString(), event(objects(BROWSER, EXTENSION), DISABLE))
		.put(AuditActions.ONBOARDING_TOKEN_GENERATION_PASSED.toString(), event(objects(USER, PASSWORD, TOKEN), CREATE))
		.put(AuditActions.ONBOARDING_TOKEN_GENERATION_FAILED.toString(), event(objects(USER, PASSWORD, TOKEN), CREATE, FAILED))
		.put(AuditActions.ONBOARDING_TOKEN_GENERATION_LIMIT_BYPASS.toString(), event(objects(PASSWORD, CONFIG), ENABLE))
		.put(AuditActions.DIGIT_TOKEN_GENERATION_PASSED.toString(), event(objects(USER, TOKEN), CREATE))
		.put(AuditActions.DIGIT_TOKEN_GENERATION_FAILED.toString(), event(objects(USER, TOKEN), CREATE, FAILED))
		.put(AuditActions.ONBOARDING_TOKEN_VERIFICATION_PASSED.toString(), event(objects(USER, PASSWORD, TOKEN), VERIFICATION))
		.put(AuditActions.ONBOARDING_TOKEN_VERIFICATION_FAILED.toString(), event(objects(USER, PASSWORD, TOKEN), VERIFICATION, FAILED))
		.put(AuditActions.DIGIT_TOKEN_VERIFICATION_PASSED.toString(), event(objects(USER, TOKEN), VERIFICATION))
		.put(AuditActions.DIGIT_TOKEN_VERIFICATION_FAILED.toString(), event(objects(USER, TOKEN), VERIFICATION, FAILED))
		.put(AuditActions.PASSWORD_ORG_CONFIG_CREATE.toString(), event(objects(PASSWORD, CONFIG), CREATE))
		.put(AuditActions.PASSWORD_ORG_CONFIG_UPDATE.toString(), event(objects(PASSWORD, CONFIG), UPDATE))
		.put(AuditActions.CUSTOM_PASSWORD_INSTRUCTIONS_CREATE.toString(), event(objects(PASSWORD, CUSTOM, INSTRUCTIONS), CREATE))
		.put(AuditActions.CUSTOM_PASSWORD_INSTRUCTIONS_DELETE.toString(), event(objects(PASSWORD, CUSTOM, INSTRUCTIONS), DELETE))
		//END PASSWORD_ACTIVITY

		.put(AuditEventActions.AddEntitlementFailure, event(objects(ENTITLMENT), ADD, FAILED))
		.put(AuditEventActions.AddEntitlement, event(objects(ENTITLMENT), ADD))
		.put(AuditEventActions.CreateAccount, event(objects(ACCOUNT), CREATE))
		.put(AuditEventActions.CreateAccountFailure, event(objects(ACCOUNT), CREATE, FAILED))
		.put(AuditEventActions.DeleteAccount, event(objects(ACCOUNT), DELETE))
		.put(AuditEventActions.DeleteAccountFailure, event(objects(ACCOUNT), DELETE, FAILED))
		.put(AuditEventActions.DisableAccount, event(objects(ACCOUNT), DISABLE))
		.put(AuditEventActions.DisableAccountFailure, event(objects(ACCOUNT), DISABLE, FAILED))
		.put(AuditEventActions.ACTION_DISABLE_ATTRIBUTE_SYNC, event(objects(ATTRIBUTE, SYNC), DISABLE))
		.put(AuditEventActions.ACTION_ENABLE_ATTRIBUTE_SYNC, event(objects(ATTRIBUTE, SYNC), ENABLE))
		.put(AuditEventActions.WORK_ITEM_FORWARD, event(objects(WORKITEM), FORWARD))

		.put(AuditActions.ROLE_CREATE_PASSED.toString(), event(objects(ROLE), CREATE))
		.put(AuditActions.ROLE_UPDATE_PASSED.toString(), event(objects(ROLE), UPDATE))
		.put(AuditActions.ROLE_DELETE_PASSED.toString(), event(objects(ROLE), DELETE))

		.put(AuditActions.IDENTITY_CREATE_PASSED.toString(), event(objects(IDENTITY), CREATE))
		.put(AuditActions.IDENTITY_UPDATE_PASSED.toString(), event(objects(IDENTITY), UPDATE))
		.put(AuditActions.IDENTITY_DELETE_PASSED.toString(), event(objects(IDENTITY), DELETE))

		.put(AuditActions.LIFECYCLE_STATE_CREATE_PASSED.toString(), event(objects(LIFECYCLE, STATE), CREATE))
		.put(AuditActions.LIFECYCLE_STATE_UPDATE_PASSED.toString(), event(objects(LIFECYCLE, STATE), UPDATE))
		.put(AuditActions.LIFECYCLE_STATE_DELETE_PASSED.toString(), event(objects(LIFECYCLE, STATE), DELETE))

		.put(AuditActions.TASK_SCHEDULE_CREATE_PASSED.toString(), event(objects(TASK, SCHEDULE), CREATE))
		.put(AuditActions.TASK_SCHEDULE_UPDATE_PASSED.toString(), event(objects(TASK, SCHEDULE), UPDATE))
		.put(AuditActions.TASK_SCHEDULE_DELETE_PASSED.toString(), event(objects(TASK, SCHEDULE), DELETE))

		.put(AuditEvent.ActionIdentityTriggerEvent, event(objects(IDENTITY, LIFECYCLE), CHANGE))
		.put(AuditEvent.ManualChange, event(objects(ACCOUNT, MANUAL, CHANGE), COMPLETE))
		.put(AuditEventActions.IDENTITY_ATTRIBUTE_UPDATE, event(objects(IDENTITY, ATTRIBUTE, VALUE), UPDATE))
		.put(AuditEvent.Comment, event(objects(WORKITEM, COMPLETE, COMMENTS), ADD))
		.put(AuditEvent.ActionApproveLineItem, event(objects(APPROVAL, ITEM), APPROVE))
		.put(AuditActions.ACCOUNT_PROFILE_UPDATED.toString(), event(objects(PROVISIONING, POLICY), UPDATE))
		.put(AuditActions.USER_REMOVE_ACCOUNT.toString(), event(objects(IDENTITY, ACCOUNT), REMOVE))
		.put(AuditActions.ACCOUNT_PROFILE_CREATED.toString(), event(objects(PROVISIONING, POLICY), CREATE))
		.put(AuditActions.ACCOUNT_PROFILE_DELETED.toString(), event(objects(PROVISIONING, POLICY), DELETE))

		//ACCESS_ITEM
		.put(AuditActions.SetEntitlement.toString(), event(objects(ENTITLMENT), SET))
		.put(AuditEvent.RoleAdd, event(objects(ROLE), ADD))
		.put(AuditEvent.RoleRemove, event(objects(ROLE), REMOVE))
		.put(AuditEventActions.ACTION_MAPPING_ATTRIBUTE_CREATE, event(objects(IDENTITY, PROFILE, ATTRIBUTE), CREATE))
		.put(AuditEventActions.ACTION_MAPPING_ATTRIBUTE_UPDATE, event(objects(IDENTITY, PROFILE, ATTRIBUTE), UPDATE))
		.put(AuditEventActions.ACTION_MAPPING_ATTRIBUTE_DELETE, event(objects(IDENTITY, PROFILE, ATTRIBUTE), DELETE))
		.put(AuditActions.APP_UPDATE.toString(), event(objects(APP), UPDATE))
		.put(AuditActions.APP_CREATE.toString(), event(objects(APP), CREATE))
		.put(AuditActions.APP_DELETE.toString(), event(objects(APP), DELETE))
		.put(AuditActions.APP_IMPORT.toString(), event(objects(APP), IMPORT))
		.put(AuditActions.APP_SET_ACCESS_PROFILES.toString(), event(objects(APP, ACCESS, PROFILE), SET))
		.put(AuditActions.APP_ADD.toString(), event(objects(APP), ADD))
		.put(AuditActions.APP_UPDATE_XML.toString(), event(objects(APP, XML), UPDATE))
		.put(AuditActions.APP_REMOVE.toString(), event(objects(APP), REMOVE))
		.put(AuditActions.APP_PURGED.toString(), event(objects(APP), PURGE))
		.put(AuditActions.IDENTITY_PROFILE_CONFIGURE_AUTHN.toString(), event(objects(IDENTITY, PROFILE, AUTHENTICATION), CONFIGURE))
		.put(AuditEvent.ActionForward, event(objects(ACCESS, REQUEST, APPROVAL), FORWARD))
		.put(AuditActions.AccessRequestConfigUpdated.toString(), event(objects(ACCESS, REQUEST, CONFIG), UPDATE))
		.put(AuditActions.IDENTITY_PROFILE_DELETE.toString(), event(objects(IDENTITY, PROFILE), DELETE))
		.put(AuditActions.IDENTITY_PROFILE_ATTRIBUTES_UPDATED.toString(), event(objects(IDENTITY, PROFILE, ATTRIBUTES), UPDATE))
		.put(AuditActions.IDENTITY_PROFILE_CREATE.toString(), event(objects(IDENTITY, PROFILE), CREATE))

		//AUTH
		.put(AuditEventConstants.SESSION_CREATED_PASSED, event(objects(SESSION), CREATE))
		.put(AuditEventConstants.SESSION_MAX_TIMEOUT_PASSED, event(objects(SESSION, MAXIMUM), TIMEOUT))
		.put(AuditEventConstants.USER_LOGOUT_PASSED, event(objects(USER), LOGOUT))
		.put(AuditEventConstants.SESSION_DESTROY_PASSED, event(objects(SESSION), DESTROY))
		.put(AuditEventConstants.SESSION_IDLE_TIMEOUT_PASSED, event(objects(SESSION, IDLE), TIMEOUT))
		.put(AuditEventConstants.AUTHENTICATION_REQUEST_PASSED, event(objects(AUTHENTICATION), REQUEST))
		.put(AuditEventConstants.AUTHENTICATION_REQUEST_PASSED_2, event(objects(AUTHENTICATION), REQUEST))
		.put(AuditEventConstants.AUTHENTICATION_REQUEST_PASSED_3, event(objects(AUTHENTICATION), REQUEST))
		.put(AuditEventConstants.AUTHENTICATION_REQUEST_FAILED, event(objects(AUTHENTICATION), REQUEST, FAILED))
		.put(AuditEventConstants.AUTHENTICATION_REQUEST_FAILED_2, event(objects(AUTHENTICATION), REQUEST, FAILED))
		.put(AuditEventConstants.USER_LOGOUT_PASSED_2, event(objects(USER), LOGOUT))
		.put(AuditEventConstants.USER_LOGOUT_PASSED_3, event(objects(USER), LOGOUT))
		.put(AuditEventConstants.USER_LOCKED, event(objects(AUTHENTICATION), LOCK))
		.put(AuditEventConstants.USER_BLOCKED, event(objects(AUTHENTICATION), REQUEST, BLOCKED))
		.put(AuditEventConstants.USER_PASSWORD_EXPIRED, event(objects(AUTHENTICATION), REQUEST, FAILED))
		.put(AuditEventConstants.AUTHENTICATION_TIMED_OUT, event(objects(AUTHENTICATION), TIMEOUT))
		.put(AuditEventConstants.SAML_ASSERTION_RECEIVED, event(objects(SAML, ASSERTION), RECEIVE, PASSED))
		.put(AuditEventConstants.SAML_REQUEST_SENT, event(objects(SAML, REQUEST), SEND))
		.put(AuditEventConstants.SAML_LOGIN_FAILED, event(objects(SAML, ASSERTION), RECEIVE, FAILED))
		.put(AuditActions.IDENTITY_PROVIDER_ENABLED.toString(), event(objects(IDENTITY, PROVIDER), ENABLE))
		.put(AuditActions.IDENTITY_PROVIDER_EXTERNAL_ENABLED.toString(), event(objects(IDENTITY, PROVIDER, EXTERNAL), ENABLE))
		.put(AuditActions.IDENTITY_PROVIDER_EXTERNAL_DISABLED.toString(), event(objects(IDENTITY, PROVIDER, EXTERNAL), DISABLE))
		.put(AuditActions.CLIENT_TOKEN_ISSUE.toString(), event(objects(CLIENT, TOKEN), CREATE))
		.put(AuditActions.SAML_FORCE_AUTHN.toString(), event(objects(FORCE, SAML), AUTHENTICATION))
		.put(AuditActions.IDENTITY_PROVIDER_CERTIFICATE_RENEWED.toString(), event(objects(IDENTITY, PROVIDER, CERTIFICATE), RENEW))

		//USER_MANAGEMENT
		.put(AuditEvent.ActionEmailSent, event(objects(EMAIL), SEND))
		.put(AuditEvent.ActionEmailFailure, event(objects(EMAIL), SEND, FAILED))
		.put(AuditActions.USER_DASHBOARD_GRANT.toString(), event(objects(USER, ROLE, DASHBOARD), GRANT))
		.put(AuditActions.USER_DASHBOARD_REVOKE.toString(), event(objects(USER, ROLE, DASHBOARD), REVOKE))
		.put(AuditActions.USER_REMOVE.toString(), event(objects(IDENTITY), DELETE))
		.put(AuditActions.USER_CERT_ADMIN_GRANT.toString(), event(objects(USER, ROLE, CERTIFICATION, ADMIN), GRANT))
		.put(AuditActions.USER_CERT_ADMIN_REVOKE.toString(), event(objects(USER, ROLE, CERTIFICATION, ADMIN), REVOKE))
		.put(AuditActions.USER_REPORT_ADMIN_GRANT.toString(), event(objects(USER, ROLE, REPORT, ADMIN), GRANT))
		.put(AuditActions.USER_REPORT_ADMIN_REVOKE.toString(), event(objects(USER, ROLE, REPORT, ADMIN), REVOKE))
		.put(AuditActions.USER_ROLE_ADMIN_GRANT.toString(), event(objects(USER, ROLE, ROLE, ADMIN), GRANT))
		.put(AuditActions.USER_ROLE_ADMIN_REVOKE.toString(), event(objects(USER, ROLE, ROLE, ADMIN), REVOKE))
		.put(AuditActions.USER_SOURCE_ADMIN_GRANT.toString(), event(objects(USER, ROLE, SOURCE, ADMIN), GRANT))
		.put(AuditActions.USER_SOURCE_ADMIN_REVOKE.toString(), event(objects(USER, ROLE, SOURCE, ADMIN), REVOKE))

		//SSO
		.put(AuditActions.APP_LAUNCH_SAML.toString(), event(objects(APP, SAML), LAUNCH))
		.put(AuditActions.APP_LAUNCH_WSFED.toString(), event(objects(APP, WSFED), LAUNCH))
		.put(AuditActions.APP_USAGE_AGREEMENT.toString(), event(objects(APP, USAGE, AGREEMENT), ACCEPT))
		.put(AuditActions.APP_LAUNCH_PASSWORD.toString(), event(objects(APP, PASSWORD), LAUNCH))
		.put(AuditActions.APP_ACCESS_DENIED.toString(), event(objects(APP, ACCESS), DENY))
		.put(AuditActions.APP_LAUNCH_NONE.toString(), event(objects(APP, BOOKMARK), LAUNCH))
		.put(AuditActions.APP_LAUNCH_DIR_PSWD.toString(), event(objects(APP, DIRECTORY, PASSWORD), LAUNCH))
		.put(AuditActions.SERVICE_PASSWORD_REPLAY.toString(), event(objects(SERVICE, BROWSER, PASSWORD), REPLAY))
		.put(AuditActions.SERVICE_PASSWORD_REPLAY_CREDENTIALS_RETRIEVED.toString(), event(objects(SERVICE, BROWSER, PASSWORD, REPLAY, CREDENTIALS), RETRIEVE))

		//IDENTITY_MANAGEMENT
		.put(AuditEventActions.ACTION_IDENTITY_DIRECT_CREATE, event(objects(API, IDENTITY), CREATE))
		.put(AuditActions.IdentityDirectCreateFailure.toString(), event(objects(API, IDENTITY), CREATE, FAILED))

		//SYSTEM_CONFIG
		.put(AuditEvent.ActionTaskResultsPruned, event(objects(TASK, RESULT), DELETE))
		.put(AuditActions.ORG_KBA_ADD.toString(), event(objects(SYSTEM, KBA), ADD))
		.put(AuditActions.ORG_KBA_DELETE.toString(), event(objects(SYSTEM, KBA), DELETE))
		.put(AuditActions.API_CLIENT_CREATE.toString(), event(objects(API, CLIENT), CREATE))
		.put(AuditActions.API_CLIENT_DELETE.toString(), event(objects(API, CLIENT), DELETE))
		.put(AuditActions.BRANDING_CREATE.toString(), event(objects(BRANDING), CREATE))
		.put(AuditActions.BRANDING_UPDATE.toString(), event(objects(BRANDING), UPDATE))
		.put(AuditActions.BRANDING_DELETE.toString(), event(objects(BRANDING), DELETE))
		.put(AuditActions.EMAIL_REDIRECTION_ENABLED.toString(), event(objects(EMAIL, REDIRECTION), ENABLE))
		.put(AuditActions.EMAIL_TEMPLATE_UPDATE.toString(), event(objects(EMAIL, TEMPLATE), UPDATE))

		// SCHEMA
		.put(AuditEventActions.SCHEMA_CREATED, event(objects(SOURCE, SCHEMA), ADD))
		.put(AuditEventActions.SCHEMA_UPDATED, event(objects(SOURCE, SCHEMA), UPDATE))
		.put(AuditEventActions.SCHEMA_DELETED, event(objects(SOURCE, SCHEMA), DELETE))
		.build();

	private static final Map<String, SearchableEvent> _defaultEvents = new ConcurrentHashMap<>();

	private static Log _log = LogFactory.getLog(SearchableEventFactory.class);

	static {

		_log.info("searchable_event_count = " + EVENTS.size() + ", mappings:\n" + EVENTS.entrySet().stream()
			.map(event -> event.getKey() + " -> " + event.getValue())
			.collect(Collectors.joining("\n")));
	}

	public static SearchableEvent get(String action, String type) {
		return MapUtils.get(EVENTS, action, () -> defaultEvent(action, type));
	}

	private static SearchableEvent defaultEvent(String action, String type) {

		return _defaultEvents.computeIfAbsent(action, key -> event(objects(type), action, ACTION));
	}

	private static List<String> objects(String... names) {

		return ImmutableList.copyOf(names);
	}

	private static SearchableEvent event(List<String> objects, String action) {

		return event(objects, action, PASSED);
	}

	private static SearchableEvent event(List<String> objects, String action, String status) {

		return new SearchableEvent(objects, action, status);
	}
}
