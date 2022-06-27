This section provides information about how to register a new trigger in ETS to support integration of external
customer applications with our internal IdentityNow services.

<br />

## 1. Choosing Trigger Type

There are two types of triggers to choose from: `REQUEST_RESPONSE` and `FIRE_AND_FORGET`

 - `REQUEST_RESPONSE` (HTTP) <br />
 This type of trigger is used to give the external application an ability to answer back to a trigger input payload sent by ETS. This integration is bi-directional -- a trigger output response from the external application is required for a trigger invocation to be considered complete and successful. This trigger is invoked by an internal service via REST HTTP call to ETS.
 
 - `FIRE_AND_FORGET` (HTTP/Event-based) <br />
 This type of trigger is used to notify the external application of a particular occurrence of an event.
 This integration is *not* bi-directional -- trigger invocation is successful the moment ETS sends a trigger input to
 the external application. This trigger is invoked by an internal service via REST HTTP call to ETS.
 <br /><br />
 `FIRE_AND_FORGET` trigger can also become event-based, in addition to HTTP, when the `eventSources` field is
     specified in the trigger definition. Event-based trigger is invoked by ETS on behalf of the internal service
     when an event is published to the specified Kafka topic(s) in the `eventSources` field.

<br />

## 2. Creating a Trigger

A trigger is created by adding a new json file under path(https://github.com/sailpoint/ets/blob/master/ets-server/src/main/resources/triggers) with all the needed trigger specs.

#### Naming convention of new trigger json file

Any new trigger json file name should be a combination of product name + trigger name in lower case and snake case(have underscore(_) as deleimiter between every word of the file name).  
For example, if we are creating the new trigger with name `FIRE_AND_FORGET` for product `TEST` then new json file name should be `test_fire_and_forget.json` 


<br />

### Feature Flag

Each trigger must be accompanied with a feature flag.

See [Trigger Feature Flags](https://github.com/sailpoint/ets/wiki/Trigger-Feature-Flags) documentation.

<br />

### Defining `REQUEST_RESPONSE` Trigger

- `id` **(required)**: Trigger ID
- `name` **(required)**: Trigger name
- `type` **(required)**: Trigger type - `REQUEST_RESPONSE`
- `_description_` (optional): Description of trigger
- `inputSchema` **(required)**: [Json](https://json-schema.org/) schema of trigger input (payload sent to external application)
- `exampleInput` **(required)**: Example input payload based on input schema
- `outputSchema` **(required)**: [Json](https://json-schema.org/) schema of trigger output (response payload expected back from external application)
- `exampleOutput` **(required)**: Example output payload based on output schema

<br />

Example *test:request-response*  `REQUEST_RESPONSE` trigger:
```json
{
  "id": "test:request-response",
  "name": "Request-Response Test",
  "type": "REQUEST_RESPONSE",
  "_description_": "Example trigger for testing",
  "inputSchema": {
    "type": "record",
    "name": "AccessRequestedInput",
    "fields": [
      {
        "name": "identityId",
        "type": "string"
      }
    ]
  },
  "exampleInput": {
    "identityId": "201327fda1c44704ac01181e963d463c"
  },
  "outputSchema": {
    "type": "record",
    "name": "AccessRequestedOutput",
    "fields": [
      {
        "name": "approved",
        "type": "boolean"
      }
    ]
  },
  "exampleOutput": {
    "approved": true
  }
}
```

<br />


### Defining `FIRE_AND_FORGET` Trigger

`FIRE_AND_FORGET` trigger can be "event-based", meaning the trigger can be invoked by publishing some event
to Kafka. It is always recommended to initiate trigger invocation via an event rather than an HTTP request, to prevent
synchronous dependency of the underlying service on ETS.

- `id` **(required)**: Trigger ID
- `name` **(required)**: Trigger name
- `type` **(required)**: Trigger type - `FIRE_AND_FORGET`
- `eventSources` (optional): List of Kafka topics (currently only supports topics defined in `com.sailpoint.atlas.event.idn.IdnTopic`)
containing events that sources an event-based trigger
- `_description_` (optional): Description of trigger
- `inputSchema` **(required)**: [Json](https://json-schema.org/) schema of trigger input (payload sent to external application)
- `exampleInput` **(required)**: Example input payload based on input schema

<br />

Example *test:fire-and-forget*  `FIRE_AND_FORGET` event-based trigger:
```json
{
  "id": "test:fire-and-forget",
  "name": "Fire and Forget Test",
  "_description_": "Example trigger for testing",
  "type": "FIRE_AND_FORGET",
  "eventSources": [
    {
      "topic": "IDENTITY",
      "eventType": "IDENTITY_CHANGED"
    }
  ],
  "inputSchema": {
    "type": "record",
    "name": "AccessRequestedInput",
    "fields": [
      {
        "name": "identityId",
        "type": "string"
      },
      {
        "name": "approved",
        "type": "boolean"
      }
    ]
  },
  "exampleInput": {
    "identityId": "201327fda1c44704ac01181e963d463c",
    "approved": true
  }
}
```

<br />

> **Note:** 
> - `FIRE_AND_FORGET` trigger should NOT define an `outputSchema` as it does not require custom integration to respond.
> - `FIRE_AND_FORGET` trigger is HTTP by default when `eventSources` field is not specified. When the `eventSources`
> field is specified, the trigger can be invoked via both HTTP and Kafka event.

<br />

## 3. Invoking a Trigger

> *Prerequisite: Trigger can be invoked once a tenant subscribes to the trigger.
> Without an existing subscription, an attempt to invoke a trigger will be blocked by ETS.
> 204 No Content response will be returned, instead of 200 OK.*

<br />

### HTTP-based trigger
Internal services can invoke a trigger via REST HTTP call to ETS.

#### Request
```
POST https://<org>.api.cloud.sailpoint.com/ets/trigger-invocations/start
```
- `triggerId` **(required)**: Trigger ID
- `input` **(required)**: Trigger input (payload sent to external application)
- `contentJson` **(required)**: Content produced to Kafka when invocation is completed (**NOT** sent to external application)
```
{ 
   "triggerId":"test:request-response",
   "input":{ 
      "identityId":"201327fda1c44704ac01181e963d463c"
   },
   "contentJson":{ 
      "workflowId":"some ID"
   }
}
```

#### Response
- `id`: Invocation ID
- `secret`: Unique invocation secret
- `triggerId`: Trigger ID
- `contentJson`: Content produced to Kafka when invocation is completed (**NOT** sent to external application)
```
200 OK
[
  {
    "id": "87c1d286-e21e-46ed-adc1-1ee43784054b",
    "secret": "16ddc0c4-8bcc-489d-8f47-4dfb2ebf4dff",
    "triggerId": "test:request-response",
    "contentJson": {
      "workflowId": "some ID"
    }
  }
]
```

<br />

#### Internal services are defined as:
1. Whitelisted by Oathkeeper as internal
```
// oathkeeper-server/src/main/resources/application.yml
- id: service_mantis
  authorizedGrantTypes:  
    - client_credentials  
  internal: true
```

2. Contain API role in their JWT access token:
```
{
  "tenant_id": "...",
  "internal": true,
  "pod": "...",
  "strong_auth_supported": false,
  "org": "...",
  "scope": [
    "read",
    "write"
  ],
  "exp": 1583388423,
  "authorities": [
    "API"
  ],
  "jti": "...",
  "client_id": "service_mantis"
}
```

<br />

### Event-based trigger

This section only applies to `FIRE_AND_FORGET` triggers.

Event-based trigger invocations are handled by ETS. When `eventSources` field is defined in the trigger schema,
ETS will poll appropriate event, source topics and invoke the trigger as events become available in the topics to consume.

The content of the event will be used to validate against the trigger's `inputSchema`. Only fields specified in the
`inputSchema` will be matched and used as input payload to custom application. Other fields in the event that do not
match will be ignored.

<br />

## 4. Handling Response and Trigger Output

This section only applies to `REQUEST_RESPONSE` triggers.

When a trigger invocation is started successfully, the internal service should persist the current state of the workflow
and wait for the custom application to complete the trigger. Either the invocation ID or custom key in `contentJson`
context can be used as unique identifier of the workflow when persisting its state. 

When an invocation is completed by the custom application, ETS publishes one of two types of completion events to the
"trigger_ack" topic: `InvocationCompletedEvent` or `InvocationFailedEvent`.

```java
// On successful completion
class InvocationCompletedEvent {
	String _tenantId;
	String _triggerId;
	String _invocationId;
	String _requestId;
	Map<String, Object> _output;
	Map<String, Object> _context;
}
```

```java
// On failed completion
class InvocationFailedEvent {
	String _tenantId;
	String _triggerId;
	String _requestId;
	String _invocationId;
	String _reason;
	Map<String, Object> _context;
}
```

The internal service should implement an event handler for each completion event outlined above and complete the
workflow appropriately.
