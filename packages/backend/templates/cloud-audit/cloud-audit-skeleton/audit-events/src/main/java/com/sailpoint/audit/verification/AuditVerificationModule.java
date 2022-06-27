package com.sailpoint.audit.verification;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.sailpoint.audit.service.SQSClientProvider;

public class AuditVerificationModule extends AbstractModule {

	public static final String AUDIT_VERIFICATION_SQS_CLIENT = "AuditVerificationSQSClient";

	@Override
	public void configure() {
		bind(AuditVerificationService.class);
		bind(AmazonSQS.class).annotatedWith(Names.named(AUDIT_VERIFICATION_SQS_CLIENT)).toProvider(SQSClientProvider.class).in(Scopes.SINGLETON);
	}
}
