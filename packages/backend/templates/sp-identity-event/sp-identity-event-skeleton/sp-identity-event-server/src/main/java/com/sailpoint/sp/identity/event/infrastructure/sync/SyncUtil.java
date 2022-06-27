/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.sync;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.sailpoint.iris.client.Event;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.AccountId;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.AppId;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.SourceId;
import com.sailpoint.sp.identity.event.domain.event.DisplayableIdentityReference;
import com.sailpoint.utilities.JsonUtil;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Util to process Identity and Accounts in IDENTIY_CHANGED event emitted from mantis-synchronization
 *
 * Mantis Sync IDENTITY_CHANGED --> sp-identity-event domain objects
 */
@CommonsLog
public class SyncUtil {

	private static final Set<String> INTERNAL_ATTRIBUTES =
		ImmutableSet.of(
			"cloudAuthoritativeSource".toLowerCase(),
			"cloudStatus".toLowerCase(),
			"internalCloudStatus".toLowerCase(),
			"iplanet-am-user-alias-list",
			"lastModified".toLowerCase(),
			"lastLoginTimestamp".toLowerCase()
		);

	private static final Set<String> ACCOUNT_INTERNAL_ATTRIBUTES =
		ImmutableSet.of(
			"entitlementAttributes".toLowerCase()
		);

	public static final String IDENTITY_CHANGED_EXTERNAL_ID_KEY = "externalId";

	public static final String IDENTITY_CHANGED_EXTERNAL_ALIAS_KEY = "alias";

	public static final String STACKTRACE = "stackTrace";

	public static final String PROCESSING_DETAILS = "processingDetails";


	public static Map<String, Object> getIdentityMapFromIdentiyChangedEventEvent(Event event) {
		Map<String, Object> identityMap = event.getContent(Map.class);

		List<Map> users = (List<Map>)identityMap.get("users");
		if (users == null || users.size() == 0) {
			// This is possible when IDENTITY_CHANGED event is for uncorrelated identities
			return null;
		}

		// Although the event contains a list of 'users', in practice it is only ever created with a single identity.
		if (users.size() > 1) {
			log.warn("Multiple users in IDENTITY_CHANGED event, ignoring all but the first! " + users.size());
		}

		return users.get(0);
	}

	/**
	 * Process Identity from event based on implementations from SynchronizationServiceImpl.
	 */
	public static Map<String, Object> getIdentityAttributesFromIdentityMap(Map<String, Object> identity) {
		Map<String, Object> rawAttributes = (Map<String, Object>) identity.get("attributes");

		if (rawAttributes == null || rawAttributes.size() == 0) {
			log.info("No attributes in IDENTITY_CHANGED event.");
			return null;
		}

		Map<String, Object> result = new HashMap<>();
		//clean up internal attributes.
		Set<String> keyNames = new HashSet<>(rawAttributes.keySet());
		for (String key : keyNames) {
			if (INTERNAL_ATTRIBUTES.contains(key.toLowerCase())) {
				continue;
			}
			result.put(key, rawAttributes.get(key));
		}
		//set identity attributes.
		result.put("displayName", rawAttributes.getOrDefault("displayName", identity.get("displayName")));
		result.put("firstname", rawAttributes.getOrDefault("firstname", identity.get("firstname")));
		result.put("lastname", rawAttributes.getOrDefault("lastname", identity.get("lastname")));
		result.put("email", rawAttributes.getOrDefault("email", identity.get("email")));
		result.put("created", rawAttributes.getOrDefault("created", getTimeAsString(identity, "created")));
		result.put("phone",  rawAttributes.getOrDefault("phone", identity.get("phone")));
		result.put("inactive",  rawAttributes.getOrDefault("inactive", "true"));
		result.put("employeeNumber",  rawAttributes.getOrDefault("employeeNumber", identity.get("employeeNumber")));
		result.put("isManager", identity.get("isManager"));
		result.put("manager", processManager(identity));
		result.put(PROCESSING_DETAILS, fetchProcessingDetails(identity));

		//need to convert to map type provided during json serialization/deserialization.
		return (Map<String, Object>) JsonUtil.parse(Map.class, JsonUtil.toJson(result));
	}

	/**
	 * Process Accounts from event based on implementations from SynchronizationServiceImpl.
	 */
	public static List<Account> getAccountsFromIdentityMap(Map<String, Object> identity) {
		List<Account> result = new ArrayList<>();
		List<Map<String, Object>> accounts = (List<Map<String, Object>>)identity.get("accounts");
		if(accounts == null) {
			log.warn("No accounts found in IDENTITY_CHANGED event.");
			return result;
		}

		for(Map<String, Object> account : accounts) {
			AccountId accountId = AccountId.builder()
				.id((String)account.get("id"))
				.uuid((String)account.get("uuid"))
				.name((String)account.get("name"))
				.nativeIdentity((String)account.get("nativeIdentity"))
				.build();
			SourceId sourceId = SourceId.builder()
				.id((String)account.get("serviceId"))
				.name((String)account.get("serviceName"))
				.build();
			Map<String, Object> entitlementAttributes = (Map<String, Object>)account.get("entitlementAttributes");

			Map<String, Object> accountModel = new HashMap<>();

			//clean up internal attributes.
			Set<String> keyNames = new HashSet<>(account.keySet());
			for (String key : keyNames) {
				if (ACCOUNT_INTERNAL_ATTRIBUTES.contains(key.toLowerCase())) {
					continue;
				}
				accountModel.put(key, account.get(key));
			}
			//convert long fields to time.
			accountModel.put("created", getTimeAsString(account, "created"));
			if(account.get("passwordLastSet") != null) {
				accountModel.put("passwordLastSet", getTimeAsString(account, "passwordLastSet"));
			}

			result.add(Account.builder()
				.accountId(accountId)
				.sourceId(sourceId)
				.attributes(accountModel)
				.entitlementAttributes(entitlementAttributes)
				.build());
		}
		return JsonUtil.parseList(Account.class, JsonUtil.toJson(result));
	}

	/**
	 * Process access from event based on implementations from SynchronizationServiceImpl.
	 */
	public static List<Map<String, Object>> getAccessFromIdentityMap(Map<String, Object> identity) {
		List<Map<String, Object>> result = new ArrayList<>();
		List<Map<String, Object>> accessList = (List<Map<String, Object>>)identity.get("access");
		if(accessList == null) {
			return result;
		}

		result.addAll(accessList);
		return result;
	}

	/**
	 * Process apps from event based on implementations from SynchronizationServiceImpl.
	 */
	public static List<App> getAppsFromIdentityMap(Map<String, Object> identity) {
		List<App> result = new ArrayList<>();
		List<Map<String, Object>> apps = (List<Map<String, Object>>)identity.get("apps");
		if(apps == null) {
			return result;
		}

		for (Map<String, Object> app : apps) {
			AppId appId = AppId.builder()
				.id((String)app.get("serviceAppId"))
				.name((String)app.get("serviceAppName"))
				.build();
			SourceId sourceId = SourceId.builder()
				.id((String)app.get("serviceId"))
				.name((String)app.get("serviceName"))
				.build();

			Map<String, Object> appAccountMap = (Map<String, Object>)app.get("account");
			// Account map on the app only has id and nativeIdentity. Instead of modifying mantis-sync and forcing another data migration,
			// just search for the account in the accounts list. It should be there!
			String appAccountId = (String)appAccountMap.get("id");
			String appAccountNativeIdentity = (String)appAccountMap.get("nativeIdentity");
			AccountId accountId = findAccountId(identity, appAccountId, appAccountNativeIdentity);

			result.add(App.builder()
				.appId(appId)
				.sourceId(sourceId)
				.accountId(accountId)
				.build());
		}

		return result;
	}

	/**
	 * Process the boolean "disabled" value from identity based on implementations from SynchronizationServiceImpl.
	 */
	public static Optional<Boolean> getDisabledFromIdentityMap(Map<String, Object> identityMap) {
		return getBooleanValueFromIdentityMap(identityMap, "disabled");
	}

	private static Optional<Boolean> getBooleanValueFromIdentityMap(Map<String, Object> identityMap, String key) {
		if (!identityMap.containsKey(key) || identityMap.get(key) == null) {
			return Optional.empty();
		}

		// handle both string and boolean, although it should be boolean
		return Optional.of(Boolean.parseBoolean(identityMap.get(key).toString()));
	}

	private static AccountId findAccountId(Map<String, Object> identity, String accountId, String accountNativeIdentity) {
		List<Map<String, Object>> accounts = (List<Map<String, Object>>)identity.get("accounts");
		Optional<Map<String, Object>> accountMap = (accounts == null) ? Optional.empty() :
			accounts.stream().filter(acct -> accountId.equals(acct.get("id")) && accountNativeIdentity.equals(acct.get("nativeIdentity"))).findFirst();
		AccountId resultAccountId;
		if (!accountMap.isPresent()) {
			log.warn("Account not found in identity map, filling out partial AccountId. Account:" + accountId);
			resultAccountId = AccountId.builder()
				.id(accountId)
				.nativeIdentity(accountNativeIdentity)
				.name(accountNativeIdentity)
				.build();
		} else {
			resultAccountId =  AccountId.builder()
				.id((String)accountMap.get().get("id"))
				.uuid((String)accountMap.get().get("uuid"))
				.name((String)accountMap.get().get("name"))
				.nativeIdentity((String)accountMap.get().get("nativeIdentity"))
				.build();
		}

		return resultAccountId;
	}

	/**
	 * Process Manager from event based on implementations from SynchronizationServiceImpl.
	 */
	private static Object processManager(Map<String, Object> identity) {
		if (identity.containsKey("managerId")) {
			return DisplayableIdentityReference.builder()
				.id((String)identity.get("managerId"))
				.name((String)identity.getOrDefault("managerName", ""))
				.displayName((String)identity.getOrDefault("managerDisplayName", null))
				.type(ReferenceType.IDENTITY)
				.build().toAttributesMap();
		}
		return null;
	}

	/**
	 * Extracts the info of precessing details except the stacktrace.
	 *
	 * @param identity
	 * @return a map of <String, Object> which contains info of precessing details.
	 */
	@VisibleForTesting
	static Map<String, Object> fetchProcessingDetails(Map<String, Object> identity) {

		if (identity.get(PROCESSING_DETAILS) == null) {
			return null;
		}

		Map<String, Object> processingDetails;
		try {
			processingDetails = (Map<String, Object>) identity.get(PROCESSING_DETAILS);
		} catch (Exception e) {
			log.error("failed to fetch processingDetails, it should always be a map", e);
			return null;
		}

		String stackTraceKey = null;
		for (String key : processingDetails.keySet()) {
			if (key.equalsIgnoreCase(STACKTRACE)) {
				stackTraceKey = key;
				break;
			}
		}
		if (stackTraceKey != null) {
			processingDetails.remove(stackTraceKey);
		}

		return processingDetails;
	}

	/**
	 * Get time from identity field. Use Date type to be consistent with mantis.
 	 */
	private static String getTimeAsString(Map<String, Object> identity, String field) {
		if(!identity.containsKey(field))
			return toOffsetDateTimeString(new Date());
		Object time = identity.get(field);
		if(time instanceof Double) {
			return toOffsetDateTimeString(new Date(((Double) time).longValue()));
		}
		else if(time instanceof Long) {
			return toOffsetDateTimeString(new Date((Long)time));
		} else {
			return toOffsetDateTimeString(new Date());
		}
	}

	/**
	 * Date to ISO-8601 String
	 * @param date java date.
	 * @return string as time in ISO-8601.
	 */
	private static String toOffsetDateTimeString(Date date) {
		Instant instant = date.toInstant();
		return instant.atOffset(ZoneOffset.UTC).toString();
	}

}
