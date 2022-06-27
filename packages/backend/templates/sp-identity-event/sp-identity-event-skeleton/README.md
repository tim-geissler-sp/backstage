# sp-identity-event

## Overview

sp-identity-event is a microservice that watches the IDENTITY_CHANGED and IDENTITY_DELETED events from
IdentityNow and exposes more granular events to the rest of the system.

## References

- [RFC: pm_identity_events](https://github.com/sailpoint/saas-rfcs/blob/master/text/0007-pm_identity_events.md)

## Building sp-identity-event

* Build Tool: [Gradle 5.6.2](https://docs.gradle.org/5.6.2/userguide/userguide.html)
* Platform Dependency: [atlas-boot](https://github.com/sailpoint/atlas-boot)

#### Lombok Plugin

sp-identity-event uses the [Project Lombok](https://projectlombok.org/features/all) annotations to generate getters,
setters, builders, etc.

Lombok requires annotation processing, so in order to build sp-identity-event in IntelliJ, Lombok Plugin
(_Lombok by Michail Plushnikov_) must be installed first.

Setup instructions: ["Setting up Lombok with Eclipse and Intellij"](https://www.baeldung.com/lombok-ide)

After installing the plugin and restarting, IDE errors should disappear, and the project can be built using
Gradle as usual.

#### Testcontainers
sp-identity-event uses [Testcontainers](https://www.testcontainers.org/) framework to test repository classes 
in a PostgreSQL DB running in a Docker container.

Therefore, it is required to have Docker Desktop running when building ETS.
                  
[Download](https://www.docker.com/products/docker-desktop)


```shell script
# Verify Docker is running
docker info

# From root project directory
./gradlew clean build
```

## Running sp-identity-event in Beacon Mode

Set up Beacon: ["IdentityNow Web Stack (Beacon)"](https://app.getguru.com/card/crk86dXi/IdentityNow-Web-Stack-Beacon)

```shell script
# From root project directory
export BEACON_TENANT=<tenant-name>:<vpn-name>;
export SERVER_PORT=443;
export SPRING_PROFILES_ACTIVE=production;
./gradlew bootRun
```

## Identity events

There are a number of new, identity-centric events available as **FIRE_AND_FORGET** triggers. 
Each event is described below with example content.
Identity events are generated per single identity.

### idn:identity-created

***Overview***

This event is emitted the first-time the service sees a new identity. It includes the current values of all attributes. This will
*NOT* include internal attributes used for IDN bookkeeping (eg. cloudLifecycleState - lifecycle state change probably warrants a unique
event itself).

***Example***

```json
{
    "identity": {
        "id": "ee769173319b41d19ccec6cea52f237b",
        "name": "john.doe",
        "type": "IDENTITY"
    },
    "attributes": {
        "firstname": "John",
        "lastname": "Doe",
        "email": "john.doe@gmail.com",
        "department": "Sales",
        "displayName": "John Doe",
        "created": "2020-04-27T16:48:33.597Z",
        "employeeNumber": "E009",
        "uid": "E009",
        "inactive": "true",
        "phone": null,
        "identificationNumber": "E009",
        "isManager": false,
        "manager": {
            "id": "ee769173319b41d19ccec6c235423237b",
            "name": "nice.guy",
            "type": "IDENTITY"
        },
        "customAttribute1": "customValue",
        "customAttribute2": "customValue2"
    }
}
```
### idn:identity-attributes-changed

***Overview***

This event is emitted anytime one of our core (or custom) identity attributes changes value. The event will include
all attributes that have changed value and include both the old and new values. For net-new attributes, the old value
will be null.

***Example***

```json
{
    "identity": {
        "id": "ee769173319b41d19ccec6cea52f237b",
        "name": "john.doe",
        "type": "IDENTITY"
    },
    "changes": [{
        "attribute": "department",
        "oldValue": "sales",
        "newValue": "marketing"
    }, {
        "attribute": "manager",
        "oldValue": {
            "id": "ee769173319b41d19ccec6c235423237b",
            "name": "nice.guy",
            "type": "IDENTITY"
        },
        "newValue": {
            "id": "ee769173319b41d19ccec6c235423236c",
            "name": "mean.guy",
            "type": "IDENTITY"
        }
    }, {
        "attribute": "email",
        "oldValue": "john.doe@hotmail.com",
        "newValue": "john.doe@gmail.com"
    }]
}
```
### idn:identity-deleted

***Overview***

This event is emitted when an identity has been deleted from the system. This can be either due to the Identity losing an account
on an authoritative source, or losing values for required attributes (first name, last name, email address). The last known state
of all attributes is included in the event payload.

***Example***

```json
{
    "identity": {
        "id": "ee769173319b41d19ccec6cea52f237b",
        "name": "john.doe",
        "type": "IDENTITY"
    },
    "attributes": {
        "firstname": "John",
        "lastname": "Doe",
        "email": "john.doe@gmail.com",
        "department": "Sales",
        "displayName": "John Doe",
        "created": "2020-04-27T16:48:33.597Z",
        "employeeNumber": "E009",
        "uid": "E009",
        "inactive": "true",
        "phone": null,
        "identificationNumber": "E009",
        "isManager": false,
        "manager": {
            "id": "ee769173319b41d19ccec6c235423237b",
            "name": "nice.guy",
            "type": "IDENTITY"
        },
        "customAttribute1": "customValue",
        "customAttribute2": "customValue2"
    }
}
```

### Schema

Most of the attributes defined by customers are described here: [Mapping Identity Profiles](https://community.sailpoint.com/t5/Admin-Help/Mapping-Identity-Profiles/ta-p/77877)

sp-identity-event service will include the following attributes in identity events if provided in customer identity profile:

```
Objects "identity" or "manager" with the following fields:
    "id" : "string",
    "name" : "string",
    "type" : "string"

List of identity attributes:
    "displayName", "string"
    "firstname", "string"
    "lastname", "string"
    "email", "string"
    "created", "string (ISO 8601)"
    "phone",  "string"
    "inactive",  "boolean"
    "employeeNumber",  "string"
    "isManager", "boolean"
```

## Account Events

### idn:identity-account-correlated

***Overview***

This event is emitted whenever an account is correlated with an identity. It includes the identity it was correlated with, the account, the source
and the account attributes.    
Note: This will currently *NOT* include entitlement attributes.

***Example***

```json
{
	"identity": {
		"id":"ee769173319b41d19ccec6cea52f237b",
		"name":"leroy.sitara",
		"type":"IDENTITY"
	},
	"account": {
		"id":"2c9180845e865b19015e9ff4d2ef5108",
		"name":"leroy.sitara",
		"nativeIdentity":"leroy.sitara",
		"type":"ACCOUNT",
		"uuid":null
	},
	"source": {
		"id":"2c9180855e820007015e9ff4b5a735cc",
		"name":"EndToEnd-GenericSource",
		"type":"SOURCE"
	},
	"attributes": {
		"serviceType": "DelimitedFile",
		"passwordLastSet": null,
		"created": "2017-09-20T15:42:55.728Z",
		"serviceName": "EndToEnd-GenericSource",
		"manuallyCorrelated": false,
		"nativeIdentity": "leroy.sitara",
		"supportsPasswordChange": false,
		"privileged": false,
		"accountId": "leroy.sitara",
		"name": "leroy.sitara",
		"disabled": false,
		"id": "2c9180845e865b19015e9ff4d2ef5108",
		"serviceId": "2c9180855e820007015e9ff4b5a735cc",
		"locked": false
	}
}
```

### idn:identity-account-attributes-changed

***Overview***

This event is emitted whenever a correlated account's attribute changes value. The event will include the account attributes that have 
changed value and include both the old and new values, in addition to the identity it was correlated with, the account, and the source
details. For net-new attributes, the old value will be null.  

Note: This includes only the account's schema attributes. 

***Example***

```json
{
	"identity": {
		"id":"ee769173319b41d19ccec6cea52f237b",
		"name":"leroy.sitara",
		"type":"IDENTITY"
	},
	"account": {
		"id":"2c9180845e865b19015e9ff4d2ef5108",
		"name":"jeff.sitara",
		"nativeIdentity":"leroy.sitara",
		"type":"ACCOUNT",
		"uuid":"{111}"
	},
	"source": {
		"id":"2c9180855e820007015e9ff4b5a735cc",
		"name":"EndToEnd-GenericSource",
		"type":"SOURCE"
	},
	"changes" : [
		{
			"attribute":"name",
			"oldValue":"leroy.sitara",
			"newValue":"jeff.sitara"
		},
		{
			"attribute":"country",
			"oldValue":"US",
			"newValue":"UK"
		}
	]
}

```

### idn:identity-account-uncorrelated

***Overview***

This event is emitted whenever an account is uncorrelated from an identity. It includes the identity it was correlated with, the account, and the source 
details.

***Example***

```json
{
	"identity": {
		"id":"ee769173319b41d19ccec6cea52f237b",
		"name":"leroy.sitara",
		"type":"IDENTITY"
	},
	"account": {
		"id":"2c9180845e865b19015e9ff4d2ef5108",
		"name":"leroy.sitara",
		"nativeIdentity":"leroy.sitara",
		"type":"ACCOUNT",
		"uuid":null
	},
	"source": {
		"id":"2c9180855e820007015e9ff4b5a735cc",
		"name":"EndToEnd-GenericSource",
		"type":"SOURCE"
	}
}
```

## On Creating New Events

See for additional info on creating new events: https://sailpoint.atlassian.net/wiki/spaces/PLAT/pages/1349779498/Adding+or+Changing+Identity+Events
