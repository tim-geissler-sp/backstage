This section provides information about how Json schema is used to define a trigger in ETS.


<br />


## Json Documentation

Json Schema Specification: https://json-schema.org/


<br />


## Building an Json Schema

The schema can be built and tested in a scratch unit test.

<br />

Suppose we have a trigger input model as follows:

```java
class IdentityCreatedEvent {
    IdentityReference identity;
    Map<String, Object> attributes;
}

class IdentityReference {
    String id;
    String name;
}
```

Here is an example of how `IdentityCreatedEvent` is represented in JSON:

```json
{
  "identity": {
    "id": "ee769173319b41d19ccec6cea52f237b",
    "name": "john.doe"
  },
  "attributes": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@gmail.com",
    "department": "Sales",
    "manager": {
      "id": "ee769173319b41d19ccec6c235423237b",
      "name": "nice.guy"
    },
    "customAttribute1": null,
    "customAttribute2": "customValue2"
  }
}
```

We can build the Json schema that represents the model.

The "IdentityCreatedEvent" schema will be printed out like this:

```json
{
  "definitions" : {
    "record:com.sailpoint.sp.identity.event.domain.event.IdentityCreatedEvent" : {
      "type" : "object",
      "required" : [ "identity", "attributes" ],
      "additionalProperties" : true,
      "properties" : {
        "identity" : {
          "$ref" : "#/definitions/record:com.sailpoint.sp.identity.event.domain.event.IdentityReference"
        },
        "attributes" : {
          "type" : "object",
          "additionalProperties" : {
            "oneOf" : [ {
              "type" : "null"
            }, {
              "type" : "string"
            }, {
              "$ref" : "#/definitions/record:com.sailpoint.sp.identity.event.domain.event.IdentityReference"
            } ]
          }
        }
      }
    },
    "record:com.sailpoint.sp.identity.event.domain.event.IdentityReference" : {
      "type" : "object",
      "required" : [ "id", "name" ],
      "additionalProperties" : true,
      "properties" : {
        "id" : {
          "type" : "string"
        },
        "name" : {
          "type" : "string"
        }
      }
    }
  },
  "$ref" : "#/definitions/record:com.sailpoint.sp.identity.event.domain.event.IdentityCreatedEvent"
}
```
<br />


## Testing an Json Schema

Currently, ETS tests the Json schema by decoding a sample JSON payload to JsonSchema. If the JSON payload does
not match against the specified Json schema, an error will be thrown.

ETS tests each Json schema defined in triggers.json at build time similarly as described below.

<br />

Unit test used by ETS can be found in `com.sailpoint.ets.infrastructure.trigger.TriggerValidationTest`.


<br />
