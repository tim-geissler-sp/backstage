/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */
package com.sailpoint.audit;

import com.sailpoint.mantis.core.service.model.AuditEventActions;
import sailpoint.object.AuditEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuditEventConstants {

	public static final String SESSION_CREATED_PASSED = "SESSION-1";
	public static final String SESSION_MAX_TIMEOUT_PASSED = "SESSION-3";
	public static final String SESSION_IDLE_TIMEOUT_PASSED = "SESSION-2";
	public static final String USER_LOGOUT_PASSED = "SESSION-4";
	public static final String SESSION_DESTROY_PASSED = "SESSION-6";
	public static final String AUTHENTICATION_REQUEST_PASSED = "AUTHENTICATION-103";
	public static final String AUTHENTICATION_REQUEST_PASSED_2 = "AUTHENTICATION-105";
	public static final String AUTHENTICATION_REQUEST_FAILED = "AUTHENTICATION-240";
	public static final String AUTHENTICATION_REQUEST_FAILED_2 = "AUTHENTICATION-241";
	public static final String USER_LOGOUT_PASSED_2 = "AUTHENTICATION-303";
	public static final String USER_LOGOUT_PASSED_3 = "AUTHENTICATION-300";
	public static final String AUTHENTICATION_REQUEST_PASSED_3 = "AUTHENTICATION-100";
	public static final String SAML_ASSERTION_RECEIVED = "SAML2-142";
	public static final String USER_LOCKED = "AUTHENTICATION-245";
	public static final String USER_PASSWORD_EXPIRED = "AUTHENTICATION-246";
	public static final String USER_BLOCKED = "AUTHENTICATION-281";
	public static final String AUTHENTICATION_TIMED_OUT = "AUTHENTICATION-247";
	public static final String SAML_REQUEST_SENT = "SAML2-31";
	public static final String SAML_LOGIN_FAILED = "SAML2-166";

	public static final String USER = "USER";
	public static final String ACTIVITY = "ACTIVITY";
	public static final String REPORT = "REPORT";
	public static final String ROLE = "ROLE";
	public static final String ADMIN = "ADMIN";
	public static final String EMAIL = "EMAIL";
	public static final String HELPDESK = "HELPDESK";
	public static final String KBA = "KBA";
	public static final String PASSWORD = "PASSWORD";
	public static final String PHONE = "PHONE";
	public static final String AUTHENTICATION = "AUTHENTICATION";
	public static final String STEP_UP = "STEP_UP";
	public static final String EXPIRED = "EXPIRED";
	public static final String FORGOT = "FORGOT";
	public static final String ACCOUNT = "ACCOUNT";
	public static final String SOURCE = "SOURCE";
	public static final String ENTITLMENT = "ENTITLEMENT";
	public static final String SYNC = "SYNC";
	public static final String ATTRIBUTE = "ATTRIBUTE";
	public static final String APP = "APP";
	public static final String IDENTITY = "IDENTITY";
	public static final String PROFILE = "PROFILE";
	public static final String STATE = "STATE";
	public static final String CERTIFICATION = "CERTIFICATION";
	public static final String ACCESS = "ACCESS";
	public static final String CAMPAIGN = "CAMPAIGN";
	public static final String STUCK = "STUCK";
	public static final String FILTER = "FILTER";
	public static final String ACTION = "ACTION";
	public static final String CONNECTOR = "CONNECTOR";
	public static final String FILE = "FILE";
	public static final String FEED = "FEED";
	public static final String FEATURES_STRING = "FEATURES_STRING";
	public static final String SCHEMA = "SCHEMA";
	public static final String EXTERNAL = "EXTERNAL";
	public static final String LIFECYCLE = "LIFECYCLE";
	public static final String TASK = "TASK";
	public static final String SCHEDULE = "SCHEDULE";
	public static final String ITEM = "ITEM";
	public static final String CREATION = "CREATION";
	public static final String STATS = "STATS";
	public static final String PHASE = "PHASE";
	public static final String VALUE = "VALUE";
	public static final String WORKITEM = "WORKITEM";
	public static final String COMMENTS = "COMMENTS";
	public static final String APPROVAL = "APPROVAL";
	public static final String MANUAL = "MANUAL";
	public static final String PROVISIONING = "PROVISIONING";
	public static final String POLICY = "POLICY";
	public static final String XML = "XML";
	public static final String CONFIG = "CONFIG";
	public static final String ATTRIBUTES = "ATTRIBUTES";
	public static final String AGGREGATION = "AGGREGATION";
	public static final String THRESHOLD = "THRESHOLD";
	public static final String CUSTOM = "CUSTOM";
	public static final String SESSION = "SESSION";
	public static final String MAXIMUM = "MAXIMUM";
	public static final String TIMEOUT = "TIMEOUT";
	public static final String IDLE = "IDLE";
	public static final String PROVIDER = "PROVIDER";
	public static final String DASHBOARD = "DASHBOARD";
	public static final String BLOCKED = "BLOCKED";
	public static final String SAML = "SAML";
	public static final String ASSERTION = "ASSERTION";
	public static final String RECEIVE = "RECEIVE";
	public static final String WSFED = "WSFED";
	public static final String USAGE = "USAGE";
	public static final String BOOKMARK = "BOOKMARK";
	public static final String DIRECTORY = "DIRECTORY";
	public static final String AGREEMENT = "AGREEMENT";
	public static final String API = "API";
	public static final String VAULT = "VAULT";
	public static final String REDIRECT = "REDIRECT";
	public static final String REPLAY = "REPLAY";
	public static final String STRONG = "STRONG";
	public static final String MFA = "MFA";
	public static final String BROWSER = "BROWSER";
	public static final String EXTENSION = "EXTENSION";
	public static final String PATTERNS = "PATTERNS";
	public static final String INTEGRATION = "INTEGRATION";
	public static final String PASSIVE = "PASSIVE";
	public static final String SYSTEM = "SYSTEM";
	public static final String CLIENT = "CLIENT";
	public static final String BRANDING = "BRANDING";
	public static final String REDIRECTION = "REDIRECTION";
	public static final String TEMPLATE = "TEMPLATE";
	public static final String RESULT = "RESULT";
	public static final String TOKEN = "TOKEN";
	public static final String FORCE = "FORCE";
	public static final String CERTIFICATE = "CERTIFICATE";
	public static final String RULE = "RULE";
	public static final String SERVICE = "SERVICE";
	public static final String DESK = "DESK";
	public static final String CREDENTIALS = "CREDENTIALS";
	public static final String INSTRUCTIONS = "INSTRUCTIONS";

	public static final String ACTIVATE = "ACTIVATE";
	public static final String EXPORT = "EXPORT";
	public static final String IMPORT = "IMPORT";
	public static final String GRANT = "GRANT";
	public static final String REVOKE = "REVOKE";
	public static final String DELETE = "DELETE";
	public static final String UPDATE = "UPDATE";
	public static final String INVITE = "INVITE";
	public static final String NOTIFY = "NOTIFY";
	public static final String ANSWERS = "ANSWERS";
	public static final String LOCK = "LOCK";
	public static final String RESET = "RESET";
	public static final String REGISTER = "REGISTER";
	public static final String SETUP = "SETUP";
	public static final String UNLOCK = "UNLOCK";
	public static final String CHANGE = "CHANGE";
	public static final String CANCELLED = "CANCELLED";
	public static final String REQUEST = "REQUEST";
	public static final String INTERCEPT = "INTERCEPT";
	public static final String ADD = "ADD";
	public static final String CREATE = "CREATE";
	public static final String DISABLE = "DISABLE";
	public static final String ENABLE = "ENABLE";
	public static final String MODIFY = "MODIFY";
	public static final String REMOVE = "REMOVE";
	public static final String COMPLETE = "COMPLETE";
	public static final String FINISH = "FINISH";
	public static final String UPLOAD = "UPLOAD";
	public static final String AGGREGATE = "AGGREGATE";
	public static final String DOWNLOAD = "DOWNLOAD";
	public static final String SIGNOFF = "SIGNOFF";
	public static final String REASSIGN = "REASSIGN";
	public static final String LOG = "LOG";
	public static final String REMEDIATE = "REMEDIATE";
	public static final String APPROVE = "APPROVE";
	public static final String SET = "SET";
	public static final String PURGE = "PURGE";
	public static final String CONFIGURE = "CONFIGURE";
	public static final String FORWARD = "FORWARD";
	public static final String CANCEL = "CANCEL";
	public static final String RENAME = "RENAME";
	public static final String LOGOUT = "LOGOUT";
	public static final String DESTROY = "DESTROY";
	public static final String SEND = "SEND";
	public static final String LAUNCH = "LAUNCH";
	public static final String DENY = "DENY";
	public static final String ACCEPT = "ACCEPT";
	public static final String ASSIGN = "ASSIGN";
	public static final String VERIFICATION = "VERIFICATION";
	public static final String RENEW = "RENEW";
	public static final String RETRIEVE = "RETRIEVE";

	public static final String PASSED = "PASSED";
	public static final String FAILED = "FAILED";
	public static final String REJECTED = "REJECTED";
	public static final String STARTED = "STARTED";
	public static final String IGNORED = "IGNORED";
	public static final String PROCESSED = "PROCESSED";
	public static final String TERMINATED = "TERMINATED";
	public static final String APPROVED = "APPROVED";
	public static final String FORWARDED = "FORWARDED";
	public static final String ESCALATED = "ESCALATED";
	public static final String DETECTED = "DETECTED";

	public static final List<String> USER_MANAGEMENT_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditActions.USER_ACTIVATE.toString(),
			AuditActions.USER_ACTIVATE.toString(),
			AuditActions.USER_ACTIVITY_EXPORT.toString(),
			AuditActions.USER_ADMIN_GRANT.toString(),
			AuditActions.USER_ADMIN_REVOKE.toString(),
			AuditActions.USER_DELETE.toString(),
			AuditActions.USER_EMAIL_UPDATE.toString(),
			AuditActions.USER_HELPDESK_GRANT.toString(),
			AuditActions.USER_HELPDESK_REVOKE.toString(),
			AuditActions.USER_INVITE.toString(),
			AuditActions.USER_INVITE_FAILURE.toString(),
			AuditActions.USER_KBA_ANSWER_DELETE.toString(),
			AuditActions.USER_KBA_ANSWER_UPDATE.toString(),
			AuditActions.USER_KBA_ANSWER_UPDATE_NOTIFICATION.toString(),
			AuditActions.USER_KBA_ANSWERS.toString(),
			AuditActions.USER_LOCK.toString(),
			AuditActions.USER_PHONE_UPDATE.toString(),
			AuditActions.USER_REGISTRATION.toString(),
			AuditActions.USER_REGISTRATION_FAILURE.toString(),
			AuditActions.USER_REGISTRATION_LINK.toString(),
			AuditActions.USER_RESET.toString(),
			AuditActions.USER_STEP_UP_AUTH.toString(),
			AuditActions.USER_STEP_UP_AUTH_FAILURE.toString(),
			AuditActions.USER_UNLOCK.toString(),
			AuditActions.USER_UNLOCK_REJECTED.toString(),
			AuditEvent.ActionEmailSent,
			AuditEvent.ActionEmailFailure,
			AuditActions.USER_DASHBOARD_GRANT.toString(),
			AuditActions.USER_DASHBOARD_REVOKE.toString(),
			AuditActions.USER_REMOVE.toString(),
			AuditActions.USER_CERT_ADMIN_GRANT.toString(),
			AuditActions.USER_CERT_ADMIN_REVOKE.toString(),
			AuditActions.USER_REPORT_ADMIN_GRANT.toString(),
			AuditActions.USER_REPORT_ADMIN_REVOKE.toString(),
			AuditActions.USER_ROLE_ADMIN_GRANT.toString(),
			AuditActions.USER_ROLE_ADMIN_REVOKE.toString(),
			AuditActions.USER_SOURCE_ADMIN_GRANT.toString(),
			AuditActions.USER_SOURCE_ADMIN_REVOKE.toString()
	));

	// Gson issue forced us to restructure the nested object `Map<String, List<String>>`
	public static List<String> PASSWORD_ACTIVITY_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEvent.PasswordChange,
			AuditEvent.PasswordChangeFailure,
			//Custom event we've added to track password-change success
			AuditEventActions.ACTION_PASSWORD_CHANGE_SUCCESS,
			//Not sure what causes this to be logged, can't see it defined anywhere.
			AuditEventActions.PasswordsRequestStart,
			// Not sure if the following are used in IDN
			AuditEventActions.ExpirePasswordStart,
			AuditEvent.ExpiredPasswordChange,
			AuditEvent.ForgotPasswordChange,
			AuditEventActions.ForgotPasswordStart,
			//Relevant events generated by CC
			AuditEventActions.ACCOUNT_PASSWORD_RESET_FAILED,
			AuditEventActions.ACCOUNT_PASSWORD_RESET_PASSED,
			AuditEventActions.ACCOUNT_PASSWORD_SYNC_PASSED,
			AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE,
			AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_FAILED,
			AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_PASSED,
			AuditEventActions.USER_PASSWORD_RESET_REJECTED,
			AuditEventActions.USER_PASSWORD_UPDATE,
			AuditEventActions.USER_PASSWORD_UPDATE_FAILED,
			AuditEventActions.USER_PASSWORD_UPDATE_PASSED,
			AuditActions.SOURCE_PASSWORD_INTERCEPT_IGNORED.toString(),
			AuditActions.SOURCE_PASSWORD_INTERCEPT_PROCESSED.toString(),
			AuditActions.ACCOUNT_VAULT_UPDATED.toString(),
			AuditActions.SOURCE_PASSWORD_POLICY_ASSIGNED.toString(),
			AuditActions.REDIRECT_PATTERNS_UPDATED.toString(),
			AuditActions.PASSWORD_POLICY_CREATED.toString(),
			AuditActions.PASSWORD_POLICY_DELETED.toString(),
			AuditActions.PASSWORD_REPLAY_ENABLED.toString(),
			AuditActions.INTEGRATION_ENABLED.toString(),
			AuditActions.PASSWORD_REPLAY_PASSIVE.toString(),
			AuditActions.PASSWORD_POLICY_UPDATED.toString(),
			AuditActions.INTEGRATION_DISABLED.toString(),
			AuditActions.INTEGRATION_UPDATED.toString(),
			AuditActions.ACCOUNT_FULL_RESET.toString(),
			AuditActions.MFA_VERIFICATION_FAILED.toString(),
			AuditActions.PASSWORD_REPLAY_DISABLED.toString(),
			AuditActions.ONBOARDING_TOKEN_GENERATION_PASSED.toString(),
			AuditActions.ONBOARDING_TOKEN_GENERATION_FAILED.toString(),
			AuditActions.ONBOARDING_TOKEN_GENERATION_LIMIT_BYPASS.toString(),
			AuditActions.DIGIT_TOKEN_GENERATION_PASSED.toString(),
			AuditActions.DIGIT_TOKEN_GENERATION_FAILED.toString(),
			AuditActions.ONBOARDING_TOKEN_VERIFICATION_PASSED.toString(),
			AuditActions.ONBOARDING_TOKEN_VERIFICATION_FAILED.toString(),
			AuditActions.DIGIT_TOKEN_VERIFICATION_PASSED.toString(),
			AuditActions.DIGIT_TOKEN_VERIFICATION_FAILED.toString(),
			AuditActions.PASSWORD_ORG_CONFIG_UPDATE.toString(),
			AuditActions.PASSWORD_ORG_CONFIG_CREATE.toString(),
			AuditActions.CUSTOM_PASSWORD_INSTRUCTIONS_CREATE.toString(),
			AuditActions.CUSTOM_PASSWORD_INSTRUCTIONS_DELETE.toString()
	));

	// we want this report to be a super-set of password actions
	public static List<String> PROVISIONING_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEventActions.CreateAccount,
			AuditEventActions.CreateAccountFailure,
			AuditEventActions.DeleteAccount,
			AuditEventActions.DeleteAccountFailure,
			AuditEventActions.DisableAccount,
			AuditEventActions.DisableAccountFailure,
			AuditEventActions.ACTION_DISABLE_ATTRIBUTE_SYNC,
			AuditEventActions.EnableAccount,
			AuditEventActions.EnableAccountFailure,
			AuditEventActions.ACTION_ENABLE_ATTRIBUTE_SYNC,
			AuditEventActions.ModifyAccount,
			AuditEventActions.ModifyAccountFailure,
			//AuditEventActions.RequestApp, Question to Pam, just access_item?
			AuditEventActions.UnlockAccount,
			AuditEventActions.UnlockAccountFailure,
			AuditEventActions.ACTION_STATE_CHANGE,
			//Access Request
			AuditEventActions.ACTION_APP_REQUEST_APPROVED,
			AuditEventActions.ACTION_APP_REQUEST_REJECTED,

			AuditActions.IDENTITY_CREATE_PASSED.toString(),
			AuditActions.IDENTITY_UPDATE_PASSED.toString(),
			AuditActions.IDENTITY_DELETE_PASSED.toString(),
			AuditEvent.ActionIdentityTriggerEvent,
			AuditEvent.ManualChange,
			AuditEventActions.IDENTITY_ATTRIBUTE_UPDATE,
			AuditEvent.Comment,
			AuditEvent.ActionApproveLineItem,
			AuditActions.ACCOUNT_PROFILE_UPDATED.toString(),
			AuditActions.USER_REMOVE_ACCOUNT.toString(),
			AuditActions.ACCOUNT_PROFILE_CREATED.toString(),
			AuditActions.ACCOUNT_PROFILE_DELETED.toString(),
			// Forwarding a manual action
			AuditEventActions.WORK_ITEM_FORWARD
	));

	public static List<String> ACCESS_REQUEST_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEventActions.ACCESS_REQUEST_REQUESTED,
			AuditEventActions.ACCESS_REQUEST_APPROVED,
			AuditEventActions.ACCESS_REQUEST_REJECTED,
			AuditEventActions.ACCESS_REQUEST_FORWARDED,
			AuditEventActions.ACCESS_REQUEST_ESCALATE,
			AuditEventActions.ACCESS_REQUEST_ESCALATED,
			AuditEventActions.ACCESS_REQUEST_CANCELLED,
			AuditEventActions.ACCESS_REQUEST_PROCESSED,

			AuditEventActions.ACCESS_REVOKE_REQUEST_REQUESTED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_APPROVED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_REJECTED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_FORWARDED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_ESCALATED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_CANCELLED,
			AuditEventActions.ACCESS_REVOKE_REQUEST_PROCESSED,

			AuditEvent.CancelWorkflow,
			AuditEventActions.RequestApp));

	public static List<String> SOURCE_MANAGEMENT_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEventActions.CONNECTOR_FILE_DELETE,
			AuditEventActions.CONNECTOR_FILE_UPLOAD,
			AuditEventActions.ACTION_DISABLE_ATTRIBUTE_SYNC,
			AuditEventActions.ACTION_ENABLE_ATTRIBUTE_SYNC,
			AuditEventActions.SOURCE_ACCOUNT_AGGREGATION,
			AuditEventActions.SOURCE_AGGREGATION_TERMINATED,
			AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_STARTED,
			AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_PASSED,
			AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_FAILED,
			AuditEventActions.SOURCE_ACCOUNT_AGGREGATION_TERMINATED,
			AuditEventActions.SOURCE_ACCOUNTS_EXPORT,
			AuditEventActions.SOURCE_ACCOUNT_FEED_DOWNLOAD,
			AuditEventActions.SOURCE_ACTIVITY_EXPORT,
			AuditEventActions.SOURCE_CREATE,
			AuditEventActions.SOURCE_DELETE,
			AuditEventActions.SOURCE_FEATURES_STRING_UPDATE,
			AuditEventActions.SOURCE_RESET,
			AuditEventActions.SOURCE_UPDATE,
			AuditEventActions.SOURCE_ENTITLEMENT_AGGREGATION,
			AuditEventActions.SOURCE_ENTITLEMENT_IMPORT,
			AuditEventActions.SOURCE_ENTITLEMENT_EXPORT,
			AuditEventActions.SOURCE_EXTERNAL_PASSWORD_CHANGE_ACTIVITY_EXPORT,
			AuditEventActions.SOURCE_SCHEMA_ATTRIBUTE_ADDED,
			AuditEventActions.SOURCE_SCHEMA_ATTRIBUTES_DELETED,
			AuditActions.SOURCE_SCHEMA_ATTRIBUTES_UPDATED.toString(),
			AuditActions.SOURCE_UPDATE_DELETE_THRESHOLD.toString(),
			AuditActions.SOURCE_AGGREGATION_SCHEDULE_UPDATED.toString(),
			AuditActions.CONNECTOR_CREATE.toString(),
			AuditActions.CONNECTOR_DELETE.toString(),
			AuditActions.CONNECTOR_EXPORT.toString(),
			AuditActions.CONNECTOR_FILE_UPLOAD.toString(),
			AuditActions.CONNECTOR_UPDATE.toString(),
			AuditActions.SOURCE_RENAME_FAILURE_DETECTED.toString(),
			AuditEventActions.SCHEMA_CREATED,
			AuditEventActions.SCHEMA_UPDATED,
			AuditEventActions.SCHEMA_DELETED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_CREATED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_CREATE_FAILED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATE_FAILED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_DELETED,
			AuditEventActions.SERVICE_DESK_INTEGRATION_DELETE_FAILED
			));


	public static final List<String> CERTIFICATIONS_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditActions.CERT_CAMPAIGN_COMPLETE.toString(),
			AuditActions.CertificationCampaignDelete.toString(),
			AuditActions.CertificationCampaignCreate.toString(),
			AuditActions.CertificationCampaignComplete.toString(),
			AuditActions.CertificationCampaignActivate.toString(),
			AuditActions.CampaignFilterCreate.toString(),
			AuditActions.CampaignFilterDelete.toString(),
			AuditActions.CampaignFilterUpdate.toString(),
			AuditActions.STUCK_PENDING_CAMPAIGN_DETECTED.toString(),
			AuditActions.CERT_CAMPAIGN_REVIEW_NOTIFICATION.toString(),
			AuditActions.CERTIFICATIONSFINISHED.toString(),
			AuditEvent.ActionSignoff,
			AuditEvent.ActionReassign,
			AuditActions.CERT_ITEM_TIMESTAMP_STATS.toString(),
			AuditEvent.ActionCertificationsPhased,
			AuditEvent.ActionRemediate,
			AuditActions.RULE_CREATE_PASSED.toString(),
			AuditActions.RULE_UPDATE_PASSED.toString(),
			AuditActions.RULE_DELETE_PASSED.toString()));

	public static List<String> ACCESS_ITEM_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			//Entitlement Add, Remove, Success/Failure
			AuditEventActions.AddEntitlement,
			AuditEventActions.AddEntitlementFailure,
			AuditEventActions.RemoveEntitlement,
			AuditEventActions.RemoveEntitlementFailure,

			//Check if modify entitlement is present - Not sure how to modify an entitlement except using uploads

			//Generated from CC
			AuditEventActions.ACCESS_PROFILE_CREATE,
			AuditEventActions.ACCESS_PROFILE_DELETE,
			AuditEventActions.ACCESS_PROFILE_UPDATE,

			//Generated from mantis
			AuditActions.ACCESS_PROFILE_CREATE_PASSED.toString(),
			AuditActions.ACCESS_PROFILE_UPDATE_PASSED.toString(),
			AuditActions.ACCESS_PROFILE_DELETE_PASSED.toString(),

			AuditActions.ROLE_CREATE_PASSED.toString(),
			AuditActions.ROLE_UPDATE_PASSED.toString(),
			AuditActions.ROLE_DELETE_PASSED.toString(),

			//LCS(Life cycle state) should be both here and provisioning
			AuditActions.LIFECYCLE_STATE_CREATE_PASSED.toString(),
			AuditActions.LIFECYCLE_STATE_UPDATE_PASSED.toString(),
			AuditActions.LIFECYCLE_STATE_DELETE_PASSED.toString(),
			//IDENTITY_PROFILE_UPDATE - when a new LCS is added, target: Identity Profile name; Naming is not consistent
			AuditActions.IDENTITY_PROFILE_UPDATE.toString(),
			//LCS Update - action:update, target:Lifecycle State:<LCS name>
			AuditActions.LIFECYCLE_STATE_CREATE_PASSED.toString(),
			AuditActions.LIFECYCLE_STATE_UPDATE_PASSED.toString(),
			AuditActions.LIFECYCLE_STATE_DELETE_PASSED.toString(),

			AuditActions.SetEntitlement.toString(),
			AuditEvent.RoleAdd,
			AuditEvent.RoleRemove,
			AuditEventActions.ACTION_MAPPING_ATTRIBUTE_CREATE,
			AuditEventActions.ACTION_MAPPING_ATTRIBUTE_DELETE,
			AuditActions.APP_UPDATE.toString(),
			AuditActions.APP_CREATE.toString(),
			AuditActions.APP_DELETE.toString(),
			AuditActions.APP_IMPORT.toString(),
			AuditActions.APP_SET_ACCESS_PROFILES.toString(),
			AuditActions.APP_ADD.toString(),
			AuditActions.APP_UPDATE_XML.toString(),
			AuditActions.APP_REMOVE.toString(),
			AuditActions.APP_PURGED.toString(),
			AuditActions.IDENTITY_PROFILE_CONFIGURE_AUTHN.toString(),
			AuditEvent.ActionForward,
			AuditActions.AccessRequestConfigUpdated.toString(),
			AuditActions.IDENTITY_PROFILE_DELETE.toString(),
			AuditActions.IDENTITY_PROFILE_ATTRIBUTES_UPDATED.toString(),
			AuditActions.IDENTITY_PROFILE_CREATE.toString()
	));

	public static List<String> AUTH_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEventConstants.SESSION_CREATED_PASSED,
			AuditEventConstants.SESSION_DESTROY_PASSED,
			AuditEventConstants.USER_LOGOUT_PASSED,
			AuditEventConstants.SESSION_IDLE_TIMEOUT_PASSED,
			AuditEventConstants.SESSION_MAX_TIMEOUT_PASSED,
			AuditEventConstants.AUTHENTICATION_REQUEST_PASSED,
			AuditEventConstants.AUTHENTICATION_REQUEST_PASSED_2,
			AuditEventConstants.AUTHENTICATION_REQUEST_PASSED_3,
			AuditEventConstants.AUTHENTICATION_REQUEST_FAILED,
			AuditEventConstants.AUTHENTICATION_REQUEST_FAILED_2,
			AuditEventConstants.USER_LOGOUT_PASSED_2,
			AuditEventConstants.USER_LOGOUT_PASSED_3,
			AuditEventConstants.SAML_ASSERTION_RECEIVED,
			AuditEventConstants.SAML_REQUEST_SENT,
			AuditEventConstants.SAML_LOGIN_FAILED,
			AuditEventConstants.USER_LOCKED,
			AuditEventConstants.USER_PASSWORD_EXPIRED,
			AuditEventConstants.USER_BLOCKED,
			AuditEventConstants.AUTHENTICATION_TIMED_OUT,
			AuditActions.IDENTITY_PROVIDER_ENABLED.toString(),
			AuditActions.IDENTITY_PROVIDER_EXTERNAL_ENABLED.toString(),
			AuditActions.IDENTITY_PROVIDER_EXTERNAL_DISABLED.toString(),
			AuditActions.CLIENT_TOKEN_ISSUE.toString(),
			AuditActions.SAML_FORCE_AUTHN.toString(),
			AuditActions.SAML_FORCE_AUTHN.toString()
	));

	public static List<String> SSO_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditActions.APP_LAUNCH_SAML.toString(),
			AuditActions.APP_LAUNCH_WSFED.toString(),
			AuditActions.APP_USAGE_AGREEMENT.toString(),
			AuditActions.APP_LAUNCH_PASSWORD.toString(),
			AuditActions.APP_ACCESS_DENIED.toString(),
			AuditActions.APP_LAUNCH_NONE.toString(),
			AuditActions.APP_LAUNCH_DIR_PSWD.toString(),
			AuditActions.SERVICE_PASSWORD_REPLAY.toString(),
			AuditActions.SERVICE_PASSWORD_REPLAY_CREDENTIALS_RETRIEVED.toString()
	));

	public static List<String> SYSTEM_CONFIG_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditActions.ORG_KBA_ADD.toString(),
			AuditActions.ORG_KBA_DELETE.toString(),
			AuditActions.API_CLIENT_CREATE.toString(),
			AuditActions.API_CLIENT_DELETE.toString(),
			AuditActions.BRANDING_CREATE.toString(),
			AuditActions.BRANDING_UPDATE.toString(),
			AuditActions.BRANDING_DELETE.toString(),
			AuditActions.EMAIL_REDIRECTION_ENABLED.toString(),
			AuditActions.EMAIL_TEMPLATE_UPDATE.toString(),
			AuditEvent.ActionTaskResultsPruned,
			AuditActions.TASK_SCHEDULE_CREATE_PASSED.toString(),
			AuditActions.TASK_SCHEDULE_UPDATE_PASSED.toString(),
			AuditActions.TASK_SCHEDULE_DELETE_PASSED.toString()
	));

	public static List<String> IDENTITY_MANAGMENT_ACTION_LIST = Collections.unmodifiableList(Arrays.asList(
			AuditEventActions.ACTION_IDENTITY_DIRECT_CREATE,
			AuditActions.IdentityDirectCreateFailure.toString()
	));

	public static Map<String, String> CRUD_TYPE_ACTIONS_MAP = Collections.unmodifiableMap(buildCrudTypeActionsMap());

	private static Map<String, String> buildCrudTypeActionsMap() {
		Map<String, String> actionsMap = new LinkedHashMap<>();

		actionsMap.put("createIdentity", AuditActions.IDENTITY_CREATE_PASSED.toString());
		actionsMap.put("updateIdentity", AuditActions.IDENTITY_UPDATE_PASSED.toString());
		actionsMap.put("deleteIdentity", AuditActions.IDENTITY_DELETE_PASSED.toString());

		actionsMap.put("createCloud Role", AuditActions.ROLE_CREATE_PASSED.toString());
		actionsMap.put("updateCloud Role", AuditActions.ROLE_UPDATE_PASSED.toString());
		actionsMap.put("deleteCloud Role", AuditActions.ROLE_DELETE_PASSED.toString());

		actionsMap.put("createLifecycle State", AuditActions.LIFECYCLE_STATE_CREATE_PASSED.toString());
		actionsMap.put("updateLifecycle State", AuditActions.LIFECYCLE_STATE_UPDATE_PASSED.toString());
		actionsMap.put("deleteLifecycle State", AuditActions.LIFECYCLE_STATE_DELETE_PASSED.toString());

		actionsMap.put("createTaskSchedule", AuditActions.TASK_SCHEDULE_CREATE_PASSED.toString());
		actionsMap.put("updateTaskSchedule", AuditActions.TASK_SCHEDULE_UPDATE_PASSED.toString());
		actionsMap.put("deleteTaskSchedule", AuditActions.TASK_SCHEDULE_DELETE_PASSED.toString());

		actionsMap.put("createAccess Profile", AuditActions.ACCESS_PROFILE_CREATE_PASSED.toString());
		actionsMap.put("updateAccess Profile", AuditActions.ACCESS_PROFILE_UPDATE_PASSED.toString());
		actionsMap.put("deleteAccess Profile", AuditActions.ACCESS_PROFILE_DELETE_PASSED.toString());

		actionsMap.put("createRule", AuditActions.RULE_CREATE_PASSED.toString());
		actionsMap.put("updateRule", AuditActions.RULE_UPDATE_PASSED.toString());
		actionsMap.put("deleteRule", AuditActions.RULE_DELETE_PASSED.toString());

		return actionsMap;
	}
}
