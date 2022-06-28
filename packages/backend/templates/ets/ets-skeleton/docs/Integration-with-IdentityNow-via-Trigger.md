This section provides information about how to integrate with SailPoint's IdentityNow via a *trigger*.

# Trigger Types

There are two types of triggers to choose from: `REQUEST_RESPONSE` and `FIRE_AND_FORGET`

 - `REQUEST_RESPONSE` <br />
 This type of trigger is used to give the custom application an ability to answer back to a trigger event
 sent by the trigger service. This integration is bi-directional -- a response from the custom application is required
 for a trigger invocation to be considered complete and successful.
 
 - `FIRE_AND_FORGET` <br />
 This type of trigger is used to notify the custom application of a particular occurrence of an event.
 This integration is *not* bi-directional -- trigger invocation is successful the moment the trigger service notifies
 the external application, and it does not require a response from the custom application.
 
<br />
 
# 1. Getting a List of Available Triggers

List of available triggers can be retrieved via a HTTP GET request.


#### Request
```
GET https://<org>.api.identitynow.com/beta/triggers
```

#### Response
- `type`: Trigger Type, i.e. `REQUEST_RESPONSE` or `FIRE_AND_FORGET`
- `id`: Trigger ID
- `inputSchema`: [Json](https://json-schema.org/) schema of trigger input (payload sent by SailPoint)
- `outputSchema`: [Json](https://json-schema.org/) schema of trigger output (response payload expected to send back to SailPoint)
- `exampleInput`: Example input payload based on input schema
- `exampleOutput`: Example output payload based on output schema

200 OK
```json
[
  {
    "type": "REQUEST_RESPONSE",
    "id": "test:request-response",
    "inputSchema": "{\"type\":\"record\",\"name\":\"AccessRequestedInput\",\"fields\":[{\"name\":\"identityId\",\"type\":\"string\"}]}",
    "outputSchema": "{\"type\":\"record\",\"name\":\"AccessRequestedOutput\",\"fields\":[{\"name\":\"approved\",\"type\":\"boolean\"}]}",
    "exampleInput": {
      "identityId": "201327fda1c44704ac01181e963d463c"
    },
    "exampleOutput": {
      "approved": true
    }
  },
  {
    "...": "..."
  }
]
```

<br />

# 2. Choosing Invocation Response Mode

Response mode simply means how the custom application wishes to interact with the trigger service.

There are three response modes to choose from: `SYNC`, `ASYNC`, and `DYNAMIC`

 - `SYNC` <br />
 This type of response creates a *synchronous* flow between the trigger service and the custom application.
 Once a trigger has been invoked, the custom application is expected to respond within 10 seconds before timeout.
 
 - `ASYNC` <br />
 This type of response creates an *asynchronous* flow between the trigger service and the custom application.
 When a trigger is invoked, the custom application does not need to respond immediately.
 The trigger service will provide an URL and a secret that the custom application can use to complete the invocation
 if the custom application wishes to handle the invocation at a later time.
 
 - `DYNAMIC` <br />
 This type of response gives the custom application the ability to choose on the fly whether it handles the
 invocation request synchronously or asynchronously.
 In some cases, `SYNC` mode may be chosen because it is able to respond quickly. In other cases, it may choose
 `ASYNC` because it needs to run some long, blocking task before responding to the invocation.

> Note: Above response modes only apply when HTTP is specified as subscription type.


<br />


# 3. Subscribing to a Trigger

Creating a subscription to a trigger is the first step of the integration process.
This allows the custom application to start receiving trigger invocations from the trigger service.

There are three types of subscriptions:
 - `HTTP` subscription is the primary subscription type that involves the trigger service making HTTP request to
 custom integration when a trigger is invoked.
 - `INLINE` subscription is a subscription type specifically used for Engineering tests or Sales POC demo.
 Inline subscription will mock out custom integration and uses the specified output from the provided `inlineConfig`
 to simulate trigger output coming from a custom integration. This allows for quick demonstration of how triggers
 should work without building a real custom integration. `INLINE` subscription type is NOT available to customers.
 - `SCRIPT` subscription is a subscription type that executes a user-provided code to handle a trigger invocation.
 This may be used in lieu of more sophisticated, custom integration when simple and lightweight integration is sufficient.
 - `EVENTBRIDGE` subscription sends trigger invocations to AWS Event Bridge of our customers' AWS accounts. Each of these subscription means an event source in a customer's AWS account and the customer will be able to consume the event however they want.

#### Request
```
POST https://<org>.api.identitynow.com/beta/trigger-subscriptions
```

- `name` (optional): Name of the subscription
- `description` (optional): Description of the subscription
- `triggerId` **(required)**: Trigger ID
- `type` **(required)**: Subscription type (HTTP or INLINE)
- `httpConfig` **(required if type is HTTP)**
	- `url` **(required)**: URL of the custom application
	- `httpDispatchMode` **(required if Trigger Type is REQUEST_RESPONSE)**: Response mode, i.e. `SYNC` or `ASYNC`
	- `httpAuthenticationType` (optional): Authentication type, i.e. `NO_AUTH` (default), `BASIC_AUTH`, `BEARER_TOKEN`
	- `basicAuthConfig` (optional): Config if `BASIC_AUTH` is used
		- `userName` **(required)** if `BASIC_AUTH` is used
		- `password` **(required)** if `BASIC_AUTH` is used
	- `bearerTokenAuthConfig` (optional): Config if `BEARER_TOKEN` is used
		- `bearerToken` **(required)** if `BEARER_TOKEN` is used
- `inlineConfig` **(required if type is INLINE)**
	- `error` **(optional)**: Error string indicating failure response from custom integration
	- `output` **(optional)**: The output from custom integration
- `scriptConfig` **(required if type is SCRIPT)**
    - `responseMode` **(required)**: Response mode, i.e. `SYNC`, `ASYNC`, `DYNAMIC`
    - `language` **(required)**: Programming language the source code is written in, e.g. `JAVASCRIPT`
    - `source` **(required)**: Source code
- `eventBridgeConfig` **(required if type is EVENTBRIDGE)**
    - `awsAccount` **(required)**: 12 digit AWS account number in string format
    - `awsRegion` **(required)**: The region where event source will be created. [Here is a list of available regions](https://docs.aws.amazon.com/general/latest/gr/rande.html#regional-endpoints)
- `responseDeadline` (optional): Deadline to complete the invocation by. This is configured to PT1H (ISO 8601 duration format) by default if not provided. This option does not apply to a `REQUEST_RESPONSE` trigger subscription with `SYNC` response mode, as response is expected promptly within 10 seconds. This option also does not apply to a `FIRE_AND_FORGET` trigger subscription, as no response is required.
- `filter` (optional): Goessner JsonPath filter expression to set condition for when the trigger should be invoked.
- `enabled` (optional): True if subscription should be enabled on create, false otherwise; default to true

Example `HTTP` subscription to *test:request-response* trigger:
```json
{
  "name": "Request-response subscription",
  "description": "Request response from custom-app-url",
  "triggerId": "test:request-response",
  "type": "HTTP",
  "httpConfig": {
    "url": "https://{custom-app-url}",
    "httpDispatchMode": "ASYNC"
  },
  "responseDeadline": "PT1H",
  "filter": "$[?($.identityId == \"201327fda1c44704ac01181e963d463c\")]",
  "enabled": true
}
```

Example `INLINE` subscription to *test:request-response* trigger:
```json
{
  "triggerId": "test:request-response",
  "type": "INLINE",
  "inlineConfig": {
    "output": {
      "approved": true
    }
  },
  "enabled": true
}
```

Example `SCRIPT` subscription to *test:request-response* trigger:
```json
{
  "triggerId": "test:request-response",
  "type": "SCRIPT",
  "scriptConfig": {
    "responseMode": "SYNC",
    "language": "JAVASCRIPT",
    "source": "async (event, context) => { const triggerOutput = { approved: true }; return new EventCallResult(Status.SUCCESS, triggerOutput, null); }"
  },
  "enabled": true
}
```

Example `EVENTBRIDGE` subscription to *test:request-response* trigger:
```json
{
  "triggerId": "test:request-response",
  "type": "EVENTBRIDGE",
  "eventBridgeConfig": {
    "awsAccount": "123456789012",
    "awsRegion": "us-east-1"
  },
  "enabled": true
}
```

#### Response
- `type`: Subscription type
- `httpConfig`
	- `url`: URL of the custom application
	- `httpAuthenticationType`: Authentication type, i.e. `NO_AUTH` (default), `BASIC_AUTH`, `BEARER_TOKEN`
	- `basicAuthConfig`: Config if `BASIC_AUTH` is used
	- `bearerTokenAuthConfig`: Config if `BEARER_TOKEN` is used
	- `httpDispatchMode`: Invocation type, i.e. `SYNC` or `ASYNC`
- `id`: Subscription ID
- `name`: Name of the subscription
- `description`: Description of the subscription
- `triggerId`: Trigger ID
-  `triggerName`: Trigger Name
- `responseDeadline`: Deadline to complete the invocation by (ISO 8601 duration format)
- `enabled`: True if subscription is enabled, false otherwise

201 Created
```json
{
  "type": "HTTP",
  "httpConfig": {
    "url": "https://{custom-app-url}",
    "httpAuthenticationType": "NO_AUTH",
    "basicAuthConfig": null,
    "bearerTokenAuthConfig": null,
    "httpDispatchMode": "ASYNC"
  },
  "id": "1774e567-b486-4245-a4d4-3f256e9bfd9d",
  "name": "Request-response subscription",
  "description": "Request response from custom-app-url",
  "triggerId": "test:request-response",
  "triggerName": "Request-Response Test",
  "responseDeadline": "PT1H",
  "enabled": true
}
```


<br />


## Subscription Filter

Subscription filter enables the custom application to conditionally invoke the trigger only when some pre-specified condition is met.
Goessner JsonPath filter expression is configured as part of trigger subscription,
to only receive trigger input when the expression evaluates to true.

Suppose the trigger service is preparing the following trigger input for trigger invocation:

```json
{
  "identityId": "201327fda1c44704ac01181e963d463c"
}
```

If the custom application should only receive trigger input when the `identityId` is "1234",
the filter would be written as follows:

```
$[?($.identityId == \"1234\")]
```

---


Subscription filter can be tested for correctness beforehand, to ensure that it is valid for use with a trigger input.

#### Request
```
POST https://<org>.api.identitynow.com/beta/trigger-subscriptions/validate-filter
```

- `input` **(required)**: Mock trigger input to evaluate filter against 
- `filter` **(required)**: JsonPath expression

Example filter validation on *test:request-response* trigger input:
```json
{
  "input": {
    "identityId": "1234"
  },
  "filter": "$[?($.identityId == \"1234\")]"
}
```

#### Response

- `isValid`: True if filter expression is valid for use against provided input, false otherwise

200 OK
```json
{
  "isValid": true
}
```


<br />


## Script Subscription

There is currently only support of JavaScript.

JavaScript code with Node.js is supported for defining a handler to handle trigger invocations.
The handler function must use the keyword `async` to return a response or error.

Handler Function Signature:
```javascript
/**
 * Script Subscription JavaScript Handler Response Type
 *
 * status: Handler Status - Status={Readonly<{SUCCESS: string, ACCEPTED: string, FAILED: string}>}, e.g. Status.SUCCESS
 * output: Trigger Output - <null>|<Object>, e.g. { key1: 'value', key2: true }
 * error: Handler Error - <null>|<string>, e.g. 'Error message'
 */
class EventCallResult {
	constructor(status, output, error) {
		this.status = status;
		this.output = output;
		this.error = error;
	}
}

/**
 * Script Subscription Handler
 *
 * @param event Object - Event/Trigger Input
 * @param context Map - Context
 * @returns {Promise<EventCallResult>}
 */
exports.handler = async (event, context) => {
	// Handle trigger input

	return new EventCallResult();
}
```

<br />

Similar to HTTP subscription, Script subscription supports three response modes: `SYNC`, `ASYNC`, `DYNAMIC`.

`SYNC` response mode is used to complete or fail the trigger invocation within the handler.

Example handler of script subscription with `SYNC` response mode for *test:request-response* trigger
```javascript
exports.handler = async (event, context) => {
	// Get identityId value from event/trigger input
	const identityId = event.hasOwnProperty('identityId') ? event.identityId : null;
	
	// If identityId property doesn't exit, fail the invocation immediately
	if (identityId == null) {
		return new EventCallResult(Status.FAILED, null, 'Missing identityId! Maybe subscribed to wrong trigger?');
	}

	// Build trigger output for completing the invocation
	const triggerOutput = { approved: identityId === '1234567890' };

	// Complete the trigger synchronously with specified triggerOutput
	return new EventCallResult(Status.SUCCESS, triggerOutput, null);
}
```

<br />

Due to the constraints of the environment, it may be necessary to call out to an external resource for further
processing. In such case, `ASYNC` response mode can be used to relay the event to the external resource and complete
the invocation at later time.

Example handler of script subscription with `ASYNC` response mode for *test:request-response* trigger
```javascript
exports.handler = async (event, context) => {
	// Import Node.js https module
	const https = require('https');

	// Function to get identityId from event/trigger input
	const getIdentityId = (event) => event.hasOwnProperty('identityId') ? event.identityId : null;

	// Define a Promise on response of a HTTP POST request
	const relayEventToExternalIntegration = (event, requestUrl) => {
		return new Promise((resolve, reject) => {
			const options = {
				hostname: requestUrl.hostname,
				port: requestUrl.port,
				path: requestUrl.pathname,
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				}
			};

			const req = https.request(options, (res) => {
				res.on('end', () => {
					resolve(new EventCallResult(Status.ACCEPTED, null, null));
				});

				res.on('error', err => {
					reject(new EventCallResult(Status.FAILED, null, err.message));
				})
			});

			req.write(JSON.stringify(event));
			req.end();
		});
	}

	let identityId = getIdentityId(event);

	if (identityId == null) {
		return new EventCallResult(Status.FAILED, null, 'Missing identityId! Maybe subscribed to wrong trigger?');
	}

	// Define URL of external integration
	const integrationUrl = new URL('https://example.com/path');

	// On successful request, complete the invocation at later time
	// On error, fail the invocation immediately
	try {
		return await relayEventToExternalIntegration(event, integrationUrl);
	} catch (err) {
		return new EventCallResult(Status.FAILED, null, err.message);
	}
}
```

<br />

`DYNAMIC` response mode allows the handler to determine in flight whether to complete the invocation synchronously or
asynchronously. For synchronous completion, return `Status.SUCCESS` as the handler status. For asynchronous completion,
return `Status.ACCEPTED` as the handler status.

<br />

## Subscription Limits

 - `REQUEST_RESPONSE`<br />
 There can be only 1 subscription per `REQUEST_RESPONSE` trigger.
 This means that just one custom integration can interact with each `REQUEST_RESPONSE` trigger at a time.
 
 - `FIRE_AND_FORGET`<br />
 There can be at most 50 subscriptions per `FIRE_AND_FORGET` trigger.
 This means that at most 50 custom integrations can listen to the same trigger input of a `FIRE_AND_FORGET`
 trigger at a time. However, only 10 script subcriptions can be created out of the allotted 50 subscriptions.
 
### Script Subscription Handler Limits
 - Dependencies: [Node.js 12.x core modules](https://nodejs.org/dist/latest-v12.x/docs/api/)
 - Code size: up to 1 MB
 - Memory allocation: up to 128 MB
 - Execution timeout: 10 seconds

<br />


# 4. Initiating a Trigger Test Invocation

A dry run of trigger invocation can be initiated via a request to `/trigger-invocations/test` endpoint.

Example request to start a dry run of *test:fire-and-forget* trigger invocation:
```
POST https://<org>.api.cloud.sailpoint.com/beta/trigger-invocations/test
```
- `triggerId` **(required)**: Trigger ID
- `input` (optional): Mock trigger input, subject to follow input schema of the trigger; if not provided, the example
input specified in trigger definition is used instead 
- `contentJson` **(required)**: JSON map of metadata about the invocation (empty map is sufficient)
- `subscriptionIds` (optional): List of subscriptions to send the test invocation to; if not provided, test invocation
is sent to all subscriptions to a trigger

```json
{
  "triggerId": "test:fire-and-forget",
  "input": {
    "approved": true,
    "identityId": "201327fda1c44704ac01181e963d463c"
  },
  "contentJson": {},
  "subscriptionIds": [
    "d44c2f42-a8bb-41fd-a7ca-b5996a46c27f",
    "4fd682bd-3595-4cff-9f65-6ad746c606cb"
  ]
}
```

#### Response
- `id`: Invocation ID
- `triggerId`: Trigger ID
- `contentJson`: JSON map of metadata about the invocation
- `secret`: Unique invocation secret that must accompany the output payload to complete the invocation

200 OK
```json
[
  {
    "id": "a6de292d-1def-4e9f-a15a-23348a4ba0f3",
    "contentJson": {},
    "triggerId": "test:fire-and-forget",
    "secret": "99a4de81-e311-4f45-95b0-6be269b474c7"
  },
  {
    "id": "eb0fb93d-ced8-4228-81bb-369858e4618c",
    "contentJson": {},
    "triggerId": "test:fire-and-forget",
    "secret": "c05f54fe-ea21-40fd-9f5f-431e630e4bb7"
  }
]
```


<br />


# 5. Receiving Trigger Invocation Request

The custom application can expect to receive a POST request from the trigger service at the URL specified in trigger subscription.

Example `SYNC` *test:request-response* trigger invocation from the trigger service:

- `identityId`: *test:request-response* trigger's input payload (fields as specified in the trigger's input schema)

```json
{
  "identityId": "201327fda1c44704ac01181e963d463c"
}
```


Example `ASYNC` *test:request-response* trigger invocation from the trigger service:

- `_metadata`
	- `callbackURL`: URL to post output payload to complete the invocation
	- `secret`: Unique invocation secret that must accompany the output payload to complete the invocation
- `identityId`: *test:request-response* trigger's input payload (fields as specified in the trigger's input schema)

```json
{
  "_metadata": {
    "callbackURL": "https://{org}.api.identitynow.com/beta/trigger-invocations/{id}/complete",
    "secret": "16ddc0c4-8bcc-489d-8f47-4dfb2ebf4dff"
  },
  "identityId": "201327fda1c44704ac01181e963d463c"
}
```

> **Note:** `identityId` field is included as a field specific for the *test:request-response* trigger. Other triggers may include other fields specific for those triggers.


<br />


# 6. Responding to a Trigger Invocation (`REQUEST_RESPONSE` only)

When a `REQUEST_RESPONSE` trigger invocation request is received by the custom application, it must respond to the request with the output payload as specified in the trigger's output schema.

 - `SYNC` <br />
 The custom application responds to the trigger invocation with the output payload.
 
 - `ASYNC` <br />
 The custom application only needs to acknowledge (200 OK) that it has received the trigger invocation request
 and complete the invocation at a later time.
 
 - `DYNAMIC` <br />
 The custom application determines arbitrarily whether to respond to the trigger invocation as `SYNC` or `ASYNC`.
 In the case of `ASYNC`, the custom application only needs to acknowledge (202 Accepted) that it has received
 the trigger invocation request and complete the invocation at a later time.
 
 > **Note:** Content type of the response body MUST be `application/json` (including empty responses for `ASYNC`).

---

Suppose a custom application has received a trigger invocation request for the *test:request-response* trigger.
Following is an example of how the custom application may respond to the trigger invocation:

 - `SYNC` 
	 -	200 OK
	```json
	{
	  "approved": true
	}
	```

- `ASYNC`
	- 200 OK
	```json
	{}
	```
	- At a later time but within the invocation deadline, respond to the given completion URL
	with a secret and output payload:
	```
	POST https://{org}.api.identitynow.com/beta/trigger-invocations/{id}/complete
	```
	```json
	{
	  "secret": "16ddc0c4-8bcc-489d-8f47-4dfb2ebf4dff",
	  "output": {
	    "approved": true
	  }
	}
	``` 

- `DYNAMIC (SYNC)`
    - Same as `SYNC`

- `DYNAMIC (ASYNC)`
	- 202 Accepted
	```json
	{}
	```
	- At a later time but within the invocation deadline, respond to the given completion URL
	with a secret and output payload:
	```
	POST https://{org}.api.identitynow.com/beta/trigger-invocations/{id}/complete
	```
	```json
	{
	  "secret": "16ddc0c4-8bcc-489d-8f47-4dfb2ebf4dff",
	  "output": {
	    "approved": true
	  }
	}
	```

<br />


# 7. Did the trigger invocation get completed? Did it succeed or fail? What's its status?

The status of the invocation, from the start of the invocation to its completion,
can be tracked with the following request:

```
GET https://<org>.api.cloud.sailpoint.com/beta/trigger-invocations/status
```
- `type`: Invocation type - TEST or REAL_TIME
- `id`: Invocation ID
- `created`: UTC timestamp of when the invocation has started
- `completed`: UTC timestamp of when the invocation has completed (null represents the invocation has not yet completed)
- `triggerId`: Trigger ID
- `startInvocationInput`
    - `input`: Trigger input (payload sent to external application)
    - `triggerId`: Trigger ID
    - `contentJson`: Content produced to Kafka when invocation is completed
- `completeInvocationInput`
    - `output`: Trigger output (payload received from external application)
    - `localizedError`: Any error during completion
- `subscriptionId`: Tenant's subscription ID
- `subscriptionName`: Tenant's subscription name

```json
{
    "type": "TEST",
    "id": "1210e59e-f52b-4368-89f6-bd15d34c3a87",
    "created": "2020-04-08T12:32:59.712Z",
    "completed": null,
    "triggerId": "test:request-response",
    "startInvocationInput": {
        "input": {
            "identityId": "201327fda1c44704ac01181e963d463c"
        },
        "triggerId": "test:request-response",
        "contentJson": {}
    },
    "completeInvocationInput": {
        "output": null,
        "localizedError": null
    },
    "subscriptionId": "d43401b6-e8c9-4ab3-a475-c067e7411f29",
    "subscriptionName": "test subscription name"

}
```

> **Note:** The status endpoint works for both `REQUEST_RESPONSE` and `FIRE_AND_FORGET` triggers. However, the status of
> `FIRE_AND_FORGET` trigger invocation will contain null values in its `completeInvocationInput` as `FIRE_AND_FORGET`
> trigger is not expected be completed.

---

## Modifying Existing Subscription

An existing subscription can be modified via a PUT request with the exception of `id` and `triggerId` fields.

Example request to modify response deadline of *test:request-response* subscription:

```
PUT https://{org}.api.cloud.sailpoint.com/beta/trigger-subscriptions/{subscriptionId}
```

```json
{
    "triggerId": "test:request-response",
    "type": "HTTP",
    "httpConfig": {
        "url": "https://webhook.site/db18da4e-d9ec-4aae-a423-9fa96a9e9c84",
        "httpDispatchMode": "DYNAMIC"
    },
    "responseDeadline": "PT2H",
    "enabled": true
}
```

200 OK
```json
{
    "type": "HTTP",
    "enabled": true,
    "id": "ca9d24cb-4d61-4563-88b7-daca9caafecf",
    "triggerId": "test:request-response",
    "responseDeadline": "PT2H",
    "httpConfig": {
        "url": "https://{custom-app-url}",
        "httpAuthenticationType": "NO_AUTH",
        "basicAuthConfig": null,
        "bearerTokenAuthConfig": null,
        "httpDispatchMode": "DYNAMIC"
    }
}
```

## Unsubscribing from a Trigger

A subscription can be deleted via a DELETE request.

```
DELETE https://{org}.api.cloud.sailpoint.com/beta/trigger-subscriptions/{subscriptionId}
```

On successful delete, 204 No Content is returned.
