/*
 *  Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.infrastructure.aws;

public interface EventBridge {

	/**
	 * Create an event bridge event source in a customer's aws account
	 * @param account the 12 digit AWS account in string format
	 * @param region the AWS region where the event source will live in
	 * @param name the name of the event source
	 * @return the arn of the event source
	 */
	String createPartnerEventSource(String account, String region, String name);

	/**
	 * Delete an event bridge event source in a customer's aws account
	 * @param account the 12 digit AWS account in string format
	 * @param region the AWS region where the event source will live in
	 * @param name the name of the event source
	 */
	void deletePartnerEventSource(String account, String region, String name);


}
