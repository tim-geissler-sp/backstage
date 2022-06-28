This section provides information about how to verify and debug a trigger invocation.

#### Note: This guide is for internal developers only who have access to Kibana.

## Did the trigger invocation get started successfully? (HTTP)

If the trigger invocation could not be started due to a client error, such as missing the required trigger input field,
you can expect to receive a 4xx client error response.

There is a case where trigger invocation does not start because the tenant has not subscribed to the trigger being
invoked. In this case, you can expect to receive a 204 No Content response.

Successful response will return a 200 OK with the `id` (invocationId) field as part of the response.

Once a trigger has been successfully invoked, a list of trigger invocations that are currently active
can be retrieved with the following request:
```
GET https://<org>.api.cloud.sailpoint.com/ets/trigger-invocations
```
- `created`: UTC timestamp of when the invocation has started
- `id`: Invocation ID
- `deadline`: UTC timestamp of the deadline for completing the invocation
- `triggerId`: Trigger ID
```json
[
    {
        "created": "2020-04-08T12:50:11.355Z",
        "id": "0b9faa60-cbc4-4d55-a124-410804caf9c0",
        "deadline": "2020-04-08T13:05:11.352Z",
        "triggerId": "test:request-response"
    }
]
```


## Kibana

When the ETS API is not enough to meet your debugging needs, it is possible to trace every step of the invocation
workflow by querying Kibana for ETS logs.

### Query
```
// Lucene
message_json.invocationId:<id>
```

For example, when the trigger invocation has been started successfully, the above query will result in the following log:
```json
{
  "message": "Trigger invocation started successfully.",
  "invocationId": "fe326936-6ac2-4673-aa33-e09104eb76db",
  "triggerId": "test:request-response",
  "triggerType": "requestResponse",
  "tenantId": "echo#sangjintest",
  "subscriptionId": "d13fb156-49a3-40c5-868d-d179e3994273",
  "subscriptionType": "http"
}
```

> All trigger logs are indexed by field names, so other custom queries can be made with the query:
>
> message_json.propName:"value"

---

## Did the trigger invocation get completed? Did it succeed or fail? What's its status?

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

When the trigger invocation completes successfully, you should expect to see the following success log associated with
the Invocation ID:

### Query
```
// Lucene
message_json.invocationId:<id>
```

```json
{
  "message": "Trigger invocation completion succeeded.",
  "invocationId": "75170107-202a-4f6c-9ea8-03b9d9fb781b",
  "triggerId": "test:request-response",
  "tenantId": "echo#sangjintest"
}
```

---

Trigger invocation can fail numerous ways, e.g. invalid output response from the custom application,
custom applications fails to complete the invocation within the deadline, etc.

Here is an example of the failure log associated with the Invocation ID when the
custom application fails to complete the invocation within the deadline:
```json
{
  "message": "Trigger invocation completion failed.",
  "invocationId": "75170107-202a-4f6c-9ea8-03b9d9fb781b",
  "triggerId": "test:request-response",
  "tenantId": "echo#sangjintest",
  "reason": "invocation timed out"
}
```

> The reason for invocation completing with a failure can be determined from the `reason` field.
> Other reasons can include "no output was provided", when the custom application doesn't include the output payload
> during completion.


## Debugging Event-based Trigger

Debugging a trigger invocation from an event may be challenging.

In ETS, `com.sailpoint.ets.infrastructure.event.EtsEventHandler`, as a consumer of the event,
will catch and log any errors that occur.

If the event's `contentJson` fails validation against the trigger's input schema a json parse
error will be logged by `EtsEventHandler`.

You may also be able to trace the event via the log's `request_id`:

```
// Lucene
request_id:<request_id>
```

## Debugging invocation sent to AWS Event Bridge

Debugging Event Bridge invocations requires some extra work because we are sending event bridge events to another AWS account that we don't have much control of from our dev AWS account. Some extra configuration also need to be done on the target AWS account for you to actually see the event our partners will receive. 

Here are the steps to debug event bridge invocations:
- Login to the AWS account that you are planning to send events to. This will be the account that you will put in `eventBridgeConfig` when you are subscribing to a trigger.
- Create a SQS queue. Later we will dump events to this queue so that you can poll for messages and look at the events that this account received from event bridge integration.
- Use ETS to create an event bridge subscription. Afterwards, go to Event Bridge from partner's AWS account and verify that the event source is created.
- Select the event source and click on the *Associate with event bus* button. This will create an event bus for the event source so that we can then forward it to SQS.
- Go to Rule section. Select the event bus you just created and create a rule for it. For event pattern you need to select *Event pattern* and then *Pre-defined pattern by service* and lastly, select *All events* for service provider. Finally, in the *target* section, select the SQS queue you created just now, and save the rule.
- Invoke the trigger from ETS. If everything works, you can poll the SQS queue from AWS UI and see the JSON payload that the parter received from our event source.

Here is an example JSON payload:
```
{
  "version": "0",
  "id": "96fdd35a-15a4-5770-8b42-e88c36790c22",
  "detail-type": "test:request-response",
  "source": "aws.partner/sailpoint.com.test/fangmingecho/idn/identity-created",
  "account": "831458296683",
  "time": "2020-09-30T17:13:59Z",
  "region": "us-east-1",
  "resources": [],
  "detail": {
    "_metadata": {
      "secret": "someSecret",
      "triggerId": "test:request-response",
      "invocationId": "2a179714-9913-4738-9932-d5d531e660c0",
      "responseMode": "async",
      "triggerType": "requestResponse",
      "callbackURL": "https://fangmingecho.api.cloud.sailpoint.com/beta/trigger-invocations/2a179714-9913-4738-9932-d5d531e660c0/complete"
    },
    "key": "value"
  }
}
```
