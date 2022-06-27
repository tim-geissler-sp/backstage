package com.sailpoint.sp.identity.event.domain;

import com.sailpoint.sp.identity.event.domain.event.AccountReference;
import com.sailpoint.sp.identity.event.domain.event.AppReference;
import com.sailpoint.sp.identity.event.domain.event.SourceReference;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class App {
	AppId appId;
	SourceId sourceId;
	AccountId accountId;

	public AppReference getAppReference() {
		return AppReference.builder()
			.id(appId.getId())
			.name(appId.getName())
			.type(ReferenceType.APP)
			.build();
	}

	public SourceReference getSourceReference() {
		return SourceReference.builder()
			.id(sourceId.getId())
			.name(sourceId.getName())
			.type(ReferenceType.SOURCE)
			.build();
	}

	public AccountReference getAccountReference() {
		return AccountReference.builder()
			.id(accountId.getId())
			.nativeIdentity(accountId.getNativeIdentity())
			.name(accountId.getName())
			.uuid(accountId.getUuid())
			.type(ReferenceType.ACCOUNT)
			.build();
	}
}
