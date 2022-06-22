# sp-scheduler

- [**Overview**](#overview)
- [**Getting Started**](#getting-started)
  - [Install Prerequisites](#install-prerequisites)
  - [Running the Service Locally](#running-the-service-locally)
- [**Integration with Scheduler**](#integration-with-scheduler)
  - [Scheduled Action](#scheduled-action)
  - [Create a Scheduled Action](#create-a-scheduled-action)
    - [Cron expressions and timezones](#regarding-cron-expressions-for-scheduled actions-and-timezones)
  - [Delete a Scheduled Action by ID](#delete-a-scheduled-action-by-id)
  - [List Actions by Metadata](#list-actions-by-metadata)
  - [Bulk Delete Actions by Metadata](#bulk-delete-actions-by-metadata)
- [**Testing**](#testing)
- [**Linting**](#linting)

Overview
--------
Scheduler enables an action to be delayed until a pre-determined time. A microservice can use this feature to delay some work, or schedule some work for a specific time the future.

Getting Started
--------

### Install Prerequisites

- [Go 1.16](https://golang.org/dl/)

sp-scheduler requires the following infrastructure components:
- redis
- postgres
- kafka

### Running the Service Locally

To use a specific branch of [atlas-go](https://github.com/sailpoint/atlas-go) (Optional) :
```bash
export GOPRIVATE=github.com/sailpoint

go get github.com/sailpoint/atlas-go@{branch/commitSHA}
go mod vendor
```

To run unit tests:
```bash
make test
```

To run integration tests:
```bash
make integration
```

To run service in [Beacon](https://sailpoint.atlassian.net/wiki/x/_4BiDQ) mode:
```bash
export BEACON_TENANT={org-name}:{vpn-name}
export ATLAS_REST_PORT=7100

make run
```

---

To build a docker image:
```bash
make docker/build
```

To build and publish a docker image:
```bash
make docker/push
```

---


Integration with Scheduler
--------

### Scheduled Action
A scheduled action is a combination of an Atlas Event, a deadline and a meta field. Scheduler will send the event to its topic when the deadline is reached, you may also provide an additional cron string, to set up repeatable events. If you do not wish to provide a cron string, you may omit this field or leave it as an empty string. Meta field is a non-modifiable, JSON formatted field provided to scheduler when an action is created. It is used to query existing actions from scheduler service.

### Create a Scheduled Action
To schedule an action, post a scheduled action in JSON format to `/scheduled-actions` endpoint. The response will have two extra fields `created` and `id`. Id can be used later to delete an action.

Here is an example payload to create a scheduled action:
```
{
  "deadline": "2021-04-12T15:20:00-06:00",
  "cronString": "*/5 * * * *",
  "event": {
    "topic": "internal_test",
    "type": "SCHEDULER_INTERNAL_TEST",
    "content": {
      "accessRequestId": "1234",
      "requester": {
        "type": "IDENTITY",
        "id": "12352352345",
        "name": "john.doe"
      }
    }
  },
  "meta": {
    "some": "label",
    "to": "queryThisOrSimilarEventsAfterCreation"
  }
}
```

Header isn't required, since it will automatically be created and added.  However, if you wanted to specify a partition
key (or a group id) for the event, then you could specify it within the event object.  See the following example:
```
{
  "deadline": "2021-04-12T15:20:00-06:00",
  "cronString": "*/5 * * * *",
  "event": {
    "topic": "internal_test",
    "type": "SCHEDULER_INTERNAL_TEST",
    "headers": {
      "partitionKey" : "YOUR_KEY_HERE",
      "groupId" : "YOUR_GROUP_ID"
    }
    "content": {
      "accessRequestId": "1234",
      "requester": {
        "type": "IDENTITY",
        "id": "12352352345",
        "name": "john.doe"
      }
    }
  },
  "meta": {
    "some": "label",
    "to": "queryThisOrSimilarEventsAfterCreation"
  }
}
```

#### Regarding cron expressions for scheduled actions and timezones
You can specify a cron string expression to indicate that you want a scheduled action to be recurring.  To add a cron string expression, you would add the following to the payload:
```
"cronString": "* */5 * * *"
```
To confirm that the cron expression is evaluating to what you want, you may use the following site: https://crontab.guru/ .  This site also has several examples for commonly used expressions; e.g. hourly, daily, weekly, etc.

By default, the cron expression is assumed to be evaluated in UTC.  For example `"cronString" : "0 1 * * *"` evaluates to
 1 AM every day (for every month, etc).  This would trigger the scheduled action at 1 AM UTC.  If you want to trigger the scheduled
action everyday at 1 AM America/Detroit (or some other timezone other than UTC), then you would need to use the `cronTimezone` attribute. This
attribute takes a map with either `"location"` or `"offset"` -- but not both.  So for example, this a payload using location:
```
{
    "cronTimezone" : { "location" : "America/Santiago"},
    "cronString": "* 18 * * *",
    "event": {
        "topic": "internal_test",
        "type": "SCHEDULER_INTERNAL_TEST",
        "content": {
            "accessRequestId": "1234",
            "requester": {
                "type": "IDENTITY",
                "id": "12352352345",
                "name": "john.doe"
            }
        }
    },
    "meta": {
        
    }
}
```
Specifying the timezone location will account for daylight savings.  Please note, that only timezone locations that are in the IANA timezone database are acceptable and will be used by scheduler.
See https://en.wikipedia.org/wiki/List_of_tz_database_time_zones for a list of acceptable values.  If you supply a location that is
 not supported by the timezone location list or by scheduler, then a 400 will be returned.  If a location isn't supported, then please use a suitable substitute or specify a timezone offset.

If you don't wish to specify a timezone location, then you may specify a timezone offset.  Please note, there might be some odd 
behavior around daylight savings since you are specifying an exact offset that is constant.  To specify a timezone offset, we expect
a '+' or '-', followed by one or two digits, a single colon, and followed by two digits.  For example, "+1:00" or "-10:00" would be acceptable.
The following is how it would used in a payload:
```
{
    "cronTimezone" : { "offset" : "+12:00"},
    "cronString": "* 18 * * *",
    "event": {
        "topic": "internal_test",
        "type": "SCHEDULER_INTERNAL_TEST",
        "content": {
            "accessRequestId": "1234",
            "requester": {
                "type": "IDENTITY",
                "id": "12352352345",
                "name": "john.doe"
            }
        }
    },
    "meta": {
        
    }
}
```
If you supply a timezone offset that isn't formatted as described above, then a client error will be returned from the endpoint. 
You will need to format the timezone offset correctly, remove the timezone and use a suitable location, or use UTC.

### Delete a Scheduled Action by ID
Action can be deleted one at a time by its ID. To do that, make a DELETE request to `/scheduled-actions` end point with the event ID. For example, the URL may look like :

`/scheduled-actions/e203eb5e-85a3-4a4d-8c20-5e807c0fd362`

### List Actions by Metadata
Actions can be queried by metadata by sending a GET request to `/scheduled-actions` endpoint. Metadata needs to be in JSON string format under `meta` query param. This endpoint also allows pagination with `limit` and `offset` query params.

Here is an example request url:
```
/scheduled-actions?filters=meta eq "{"key":"value"}"&limit=20&offset=1
```

### Bulk Delete Actions by Metadata
Actions can also be deleted in bulk by metadata. This can be achieved by sending a POST request to `/scheduled-actions/bulk-delete` endpoint.

The payload will be the same as the listing actions described above.

### Testing

#### Running e2e tests "locally"
* In order to run e2e tests locally you need to build and deploy your code to a give branch, we suggest megapod. Once you have done that you can run the integration tests with the following command.
* You can build your branch [here](https://construct.identitysoon.com/view/sp-scheduler/job/sp-scheduler/)
* You can deploy via dry docker [here](https://drydock.infra.identitynow.com/ecsapp/sp-scheduler/deploy)
* Then you can run the tests local with the following command
    ```bash
    cd api-tests
    ./gradlew clean test  -Dorg.url='https://YOURORG.api.cloud.sailpoint.com' -Dorg.username='ORGUSERNAME' -Dorg.password='ORGPASSWORD!' -PapiTest=true -info
    ```
* It should be noted that to run these locally you need to be using java 8, java 11 will give you a host of errors.

#### Running e2e tests in the cloud
* You can build your branch [here](https://construct.identitysoon.com/view/sp-scheduler/job/sp-scheduler/)
* You can deploy via dry docker [here](https://drydock.infra.identitynow.com/ecsapp/sp-scheduler/deploy)
* You can run the verify job here [here](https://construct.identitysoon.com/view/sp-scheduler/job/sp-scheduler-verify/)

#### Debugging integration tests locally
* Start postgres with `make postgres`
* Set the following in your IDE Environment `ATLAS_DB_HOST=127.0.0.1;ATLAS_DB_NAME=postgres;ATLAS_DB_USER=postgres;ATLAS_DB_PASSWORD=postgres`
* Start a debug session in your IDE
* You can inspect the database by connecting with the information noted above

#### Deployment
Lastly, the deployment script can be found here [here](https://pipeline.infra.identitynow.com/job/cloud-sp-scheduler/job/master/)
In the event the deployment fails, this is where you will find your logs.

### Linting
* Linting for the repo can be performed via Make command.
* Requires [Docker](https://docs.docker.com/get-docker/).
* Run `make lint` in order to perform the linter check.

### Deprecated

The /scripts folder contains docker-compose file with postgres and redis. You can use the beacon VPN for redis/kafka if you wish.
During local testing I use the docker-compose file, along with the confluent platform Kafka.

```bash
# start redis and postgres
cd scripts
docker-compose up -d
```

```bash
# download and start confluent kafka
git clone https://github.com/confluentinc/examples confluent
cd confluent
git checkout 5.4.0-post
cd cp-all-in-one
docker-compose up -d --build
```

More details here: [Confluent Kafka Docker Setup](https://docs.confluent.io/current/quickstart/ce-docker-quickstart.html)

The following native libraries/binaries are required to build outside of the container:
- librdkafka - native library for communication with Kafka
- pkg-config - used by the go build tool to locate native dependency compile flags

To install via Homebrew on MacOS:
```bash
brew install librdkafka
brew install pkg-config
```
