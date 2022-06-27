# JSON SCHEMA

## Optionals

Let's say we want to mark something as optional, that is as easy as
listing it in an array titled `optional`. For example:

```json
{
  "definitions": {
    "record:AccessRequestedInput": {
      "type": "object",
      "optional":  [
        "approved"
      ],
      "required": [
        "identityId"
      ],
      "additionalProperties": false,
      "properties": {
        "identityId": {
          "type": "string"
        },
        "approved": {
          "type": "boolean"
        }
      }
    }
  },
  "$ref": "#/definitions/record:AccessRequestedInput"
}
```
Here we have a required "identityId" and an optional "approved".

## Dropping out values from nested attributes

In order for added values to be dropped, each nested attribute needs
to be its own class when it gets converted to a DTO. The best way to
do this is to **NOT** define nested objects inline, but reference them
as other objects. As an example, let's take a look at the following:

```json
{
  "definitions": {
    "record:TestObject": {
      "type": "object",
      "required": [
        "testArrayKey",
        "testNestedPropertyKey1",
        "testStringKey"
      ],
      "additionalProperties": true,
      "properties": {
        "testArrayKey": {
          "type": "array",
          "items": {
            "oneOf": [
              {
                "type": "null"
              },
              {
                "type": "string"
              }
            ]
          }
        },
        "testNestedPropertyKey1": {
          "$ref": "#/definitions/record:TestNestedPropertyKey1"
        },
        "testStringKey": {
          "type": "string"
        }
      }
    },
    "record:TestNestedPropertyKey1": {
      "type": "object",
      "required": [
        "testNestedPropertyKey2"
      ],
      "additionalProperties": true,
      "properties": {
        "testNestedPropertyKey2": {
          "type": "string"
        }
      }
    }
  },
  "$ref": "#/definitions/record:TestObject"
}
```

Here we define a schema with a `testArrayKey`, a
`testNestedPropertyKey1` and a `testStringKey` at the top level. The
`testNestedPropertyKey1` references a `testNestedPropertyKey1`
attribute. This will create a new class object once created to a
DTO. This is desirable, as this class object will be defined with the
`testNestedPropertyKey2` field, which will allow us to drop any other
key in the object when we convert it to a DTO. As a concrete example,
consider the following:

```json
{
  "testStringKey": "testStringValue",
  "extraTestStringKey": "extraTestStringKeyValue",
  "testArrayKey": [
    "testArrayValue0"
  ],
  "testNestedPropertyKey1": {
    "testNestedPropertyKey2": "testNestedProperty2Value",
    "extraTestNestedStringKey": "extraTestNestedStringKeyValue"
  }
}
```

If we pass this object in, not only will this validate the schema, but
when converted to a DTO, we will get this as the following object:

```json
{
  "testStringKey": "testStringValue",
  "extraTestStringKey": "extraTestStringKeyValue",
  "testArrayKey": [
    "testArrayValue0"
  ],
  "testNestedPropertyKey1": {
    "testNestedPropertyKey2": "testNestedProperty2Value"
  }
}
```

Had we not referenced the `testNestedPropertyKey1` the way we did, it
would have been created as a map rather than a class, which would not
allow us to drop the extra value.

## Creating a DTO from a schema
This is the only part that is a little tricky, but I have done what I can to simplify it. I originally wanted to have the DTO class creation done at part of the build. However, the jsonschema2pojo lib does not allow for a simple way to add the `@JsonIgnoreProperties(ignoreUnknown = true)` line to the class values. This line is desirable as this is the magic that allows us to drop out additional keys when converting json to a dto. As such, I have created a python util to generate the DTOs. All you have to do is create your jsonSchema classes, being sure to have them in the `triggers/json` directory. Then run the python `dto_generator.py`. This will regenerate all the DTO classes and store them in the `src/main/java/com/sailpoint/ets/domain/trigger/schemaGeneratedDto` directory.

This is the only part that is a little tricky, but I have done what I
can to simplify it. I originally wanted to have the DTO class creation
done as part of the build. However, the
[jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo)
lib does not allow for a simple way to add the
`@JsonIgnoreProperties(ignoreUnknown = true)` line to the class
values. This line is desirable, as this is the magic that allows us to
drop out additional keys when converting json to a DTO. As such, I
have created a python util to generate the DTOs. All you have to do is
create your jsonSchema classes, the same way you would have created
the Avro classes (being sure to locate them in the
`triggers/jsonSchema` directory). Then run the python
`dto_generator.py` utility - this utility will regenerate all the DTO
classes and store them in the
`src/main/java/com/sailpoint/ets/domain/trigger/schemaGeneratedDto`
directory.

The `dto_generator.py` utility requires jsonschema2pojo:

```bash
brew install jsonschema2pojo
```

In order to run this script, invoke:

```bash
python dto_generator.py
```  
from the project root dir.
