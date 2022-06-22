# Cloud Audit 

> Mantis Micro service to facilitate running/retreiving audit event/reports.


## Building AER

[Java 8 (HotSpot)](https://adoptium.net/?variant=openjdk8&jvmVariant=hotspot)

```bash
./gradlew clean build
```

### Lombok

AER uses [Project Lombok](https://projectlombok.org/), which automates the writing of builders. These builders are
fully featured and don't have to be tested. Lombok plugin is natively supported in IntelliJ IDEA -- ensure annotation
processing is enabled during local development.


## Audit Event Persistence Migration - 2021Q3

As of 2021Q3 the AER micro-service supports persisting Audit Event records to both the CIS MySQL DB and to S3 buckets. 
A pair of feature flags controls the state of whether a particular org writes to one or both of these; if neither is 
configured then CIS is used as a fail-safe during roll out.   The feature flags in question:

 - PLTDP_PERSIST_TO_CIS_MYSQL  - toggles writing to CIS MySQL, default value True.
 - PLTDP_PERSIST_TO_S3_BY_TIME - toggles writing to S3 regional bucket, default value False.

The bucket audit events are actually written to varies by environment and do not match what was presented in the RFC 
proposal document [rfc link](https://github.com/sailpoint/saas-rfcs/blob/master/text/0067-eng_auditdb.md).  The buckets 
vary by environment and follow the template:

 - `sp${environmentLetter}-aer-audit-events-${regionShort}`

The `regionShort` token has values like "useast1" and “uswest2”.  The `environmentLetter` token is one of 3: p=production, 
t=dev/test,  i=internal.  Notes on the nomenclature can be found on 
[SAASKEEL-1720](https://sailpoint.atlassian.net/browse/SAASKEEL-1720).  This pattern produces bucket names like the 
following:

 - spt-aer-audit-events-useast1
 - spt-aer-audit-events-uswest2
 - spp-aer-audit-events-useast1

The non-production environments can be accessed by developers via the AWS S3 user interface. This link provdes access
to the bucket for useast1 dev: [AWS S3](https://s3.console.aws.amazon.com/s3/buckets/spt-aer-audit-events-useast1?region=us-east-1&tab=objects).

The S3 Object key naming strategy has not changed from what was specified in the RFC linked above, with the note that we
omit the leading slash character from the left-hand side of the string.  Individual audit event JSON records will be 
persisted in S3 under the following object key strategy:

   `${tenantId}/${year}/${month}/${day}/${hour}/${minute}/${eventId}`

The meta-data examples in the RFC remain accurate as well.  If you want to manually search the S3 bucket for your Org or
for a customer org's audit events you will need to find their `tenantId` first.  From there you can walk directly to the
audit events for that org.

There are two other special tokens you might see in the S3 bucket:

 - `_toDeleteQueue/` - a folder for tenantIds that are pending deletion see PLTDP-1495.
 - `tenantId/` - a disposable remnant from some unit and integration tests.

In the future we might add more underscore-prefixed folders that will have special functions for operations like 
reconciliation of record counts between the S3 persistence layer and the CIS persistence layer.

The service emits two Prometheus metrics that will be used during the cut-over period. 

 - `com_sailpoint_aer_persistence_written_s3`
 - `com_sailpoint_aer_persistence_written_cis`

They count by org the number of audit events written to CIS and S3.  The idea is to track the S3 number grow to match 
the CIS number over time, then as we start shutting off writing to CIS watch that number trickle down to zero while the 
S3 metric remains high.

## Bulk Synchronization of CIS AuditEvents to S3 by-tyme Persistence

As of 2021Q4 all production org/tenants are writing AuditEvent data to S3.  Over the first weeks of 2021Q4 we will be 
bulk-syncing the old audit events that are stored only in CIS MySQL over to the new S3 persistence model.  This section 
covers the strategy for how the CIS->S3 bulk sync works.

A request to bulk-sync an org is placed into a Set of Strings persisted in the regional Redis under the key 
`aerCisToS3SyncSet`.  Each member of this Set is a ~~JSON serialization of the OrgData~~ simply the OrgName for the org 
to bulk-sync. When a request is made a `Job` worker thread is started that iterates all the values in that Redis 
Set<String::OrgName>.  The `OrgName` is used to construct the `OrgData`, which is passed to the Job. 

There is only one 
`Job` thread per AER instance.  `Job` thread takes that one Org's data and queries CIS for *counts* of AuditEvents one 
day at a time, starting from the earliest known `AuditEvent.created` date found in CIS. (Note: In development 
environments many tenants go _days_ creating zero AuditEvents.) When the day-by-day loop finds a non-zero count for a 
day the `Job` thread dispatches work to a set  of `Worker` threads where each worker thread is responsible for 
bulk-sync'ing one hour's worth of AuditEvent IDs from CIS to S3. The `Job` thread monitors the workers for completion 
before advancing to the next day.  

For any given hour, the IDs withing the time stamps are queried by the `Job` worker thread and those IDs are passed to 
a `DB I/O` thread. The `DB I/O` thread pulls back fully deserialized AuditEvent records one at a time and passing them 
over to an `S3 I/O` thread for persistence in the cloud storage.  The calls to S3 consist of a GET and a POST and can 
take a relatively long time, on the order of 50-150 msecs, so the S3 I/O worker pool has lots of threads (128) because 
they will be blocking for network I/O most of the time.  The S3 connection pool is tuned to have a very high (160) 
number of outbound connections to prevent contention for S3 HTTP sockets.

In summary: There are 3 thread pools in play: `cis2s3-job-0`, `cis2s3-dbio-N` and `cis2s3-s3id-N`. 

There is no durability or interrupt or restart mechanism for the sync jobs.  To cancel a sync job we need to restart
the AER service.  Jobs must be re-requested to run them again after a restart.  Jobs are re-entrant in the sense that 
they detect AuditEvent records that have already been uploaded to S3 and do not attempt to re-upload them.
 
A request to bulk sync an org can come in via this mechanism:

- An HTTP POST to `audit/bulk/cis2s3/sync` of simple JSON: `{ "orgName": "perflab-ahampton" }`.  A star `*` given for 
orgName indicates a reques to bulk sync all orgs. This POST can be done via PostMan or Courier.  Any org's credentials 
can be used to initiate a bulk-sync for any other org.  These are read-only syncs (i.e. they destroy no data), and only
one org's sync runs at a time, so the risk for DDOS or abuse of this private endpoint is minimal.

When starting a bulk sync via Courier, issue a POST to `/aer/audit/bulk/cis2s3/sync` for the ORG with a Request Body 
like: `{ "orgName": "perflab-05191440" }` or `{ "orgName": "*" }`. 

To perform a bulk sync for all of production, for each region's staging and production instances one POST request must 
be made. Select any intenral or canary org in the region and use Courier to POST in the `{ "orgName": "*" }` payload.  
At the time of writing the complet set of production regions and recommended canary orgs includes:

| region         | staging     | production |
| -----------    | ----------- | ---------- |
| us-east-1      | TBD         | TBD        |
| us-west-2      | TBD         | TBD        |
| ap-southeast-2 | TBD         | TBD        |
| eu-central-1   | TBD         | TBD        |
| eu-west-2      | TBD         | TBD        |
| ca-central-1   | TBD         | TBD        |
| demo (acct03)  | TBD         | TBD        |

(* Note: DevOps might own tenant selection here, based on their choosing and credentials avaialability)

The progress of all submitted sync jobs can be monitored in the logs.  All relevant log messages contain the unique 
string `CIS->S3` and occur in uniquely named worker thread names like:

- `cis2s3-job-0` for the org-scoped Job thread; currently only one of these is allowed per AER container instance. 
- `cis2s3-dbio-NN` for the hour-of-day scoped 32 worker threads.  These only issue DEBUG level logs.
- `cis2s3-s3io-NNN` for the single AuditEvent record persist in S3 worker threads. These only issue DEBUG level logs.

The search tokens `thread_name:cis2s3*` and `CIS->S3` are useful in Kibana for following the progress of sync jobs.

When the synchronization for a day's worth of records completes there is summary report line in the logs:

```
Completed to CIS->S3 day 2020-06-14T00:00:00Z sync of 373766 records in 487825 msecs. Stats: 
 {
    "submittedToQueue":373766,       // Audit Event IDs in the given time window that we tried to process.
    "adoptedByWorkerThread":373766,  // Audit Event IDs that were found by the worker thread querying CIS.
    "missingFromSource":0,           // Audit Event IDs that were found, but no Audit Event could be constructed.
    "newlyCreatedInTarget":184,      // New AuditEvent records persisted in S3.
    "alreadyExistedInTarget":373582, // AuditEvent records that already existed in S3 and matched the checksum.
    "exceptionCounter":0             // Excepions encountered during processing; often S3 network issues.
 }
```

A similar message is emitted for the completion of the entire sync job for an org:

```
Completed CIS->S3 Sync for adam-smalllab in 620485 msecs, stats: {"submittedToQueue":25675,
"adoptedByWorkerThread":25675,"missingFromSource":0,"newlyCreatedInTarget":25675,"alreadyExistedInTarget":0,
"exceptionCounter":0}
```

Example log messages:

```text
Oct 14, 2021 @ 11:00:10.416	HTTP Request to CIS->S3 Bulk Sync org: perflab-05191440
Oct 14, 2021 @ 11:00:10.419	Added org:perflab-05191440 tenantId:46aa867a-adb2-431b-9c05-37574ce8f51f to Redis queue for CIS->S3 sync
Oct 14, 2021 @ 11:00:10.422	Starting CIS->S3 Sync for perflab-05191440
Oct 14, 2021 @ 11:00:10.478	CIS->S3 using earliestDate of: 2017-05-19T00:00:00Z for org:perflab-05191440
Oct 14, 2021 @ 11:00:10.496	CIS->S3 Sync'ing 83 AuditEvents for perflab-05191440 day 2017-05-19T00:00:00Z -> 2017-05-19T23:59:59.999Z
Oct 14, 2021 @ 11:00:10.783	CIS->S3 Dispatching 73 AuditEvents for perflab-05191440 in hour 2017-05-19T19:00:00Z -> 2017-05-19T20:00:00Z
Oct 14, 2021 @ 11:00:10.834	CIS->S3 Dispatching 10 AuditEvents for perflab-05191440 in hour 2017-05-19T20:00:00Z -> 2017-05-19T21:00:00Z
Oct 14, 2021 @ 11:00:11.564	CIS->S3 Poll Ended, desiredCount:83 statsResult:83 duration:500
Oct 14, 2021 @ 11:00:11.565	Completed to CIS->S3 day 2017-05-19T00:00:00Z sync of 83 records in 1069 msecs. Stats: {"submittedToQueue":83,"adoptedByWorkerThread":83,"missingFromSource":0,"newlyCreatedInTarget":0,"alreadyExistedInTarget":83,"exceptionCounter":0}
Oct 14, 2021 @ 11:00:11.578	CIS->S3 Sync'ing 14 AuditEvents for perflab-05191440 day 2017-05-20T00:00:00Z -> 2017-05-20T23:59:59.999Z
Oct 14, 2021 @ 11:00:11.596	CIS->S3 Dispatching 8 AuditEvents for perflab-05191440 in hour 2017-05-20T00:00:00Z -> 2017-05-20T01:00:00Z
Oct 14, 2021 @ 11:00:11.873	CIS->S3 Dispatching 3 AuditEvents for perflab-05191440 in hour 2017-05-20T08:00:00Z -> 2017-05-20T09:00:00Z
Oct 14, 2021 @ 11:00:12.152	CIS->S3 Dispatching 3 AuditEvents for perflab-05191440 in hour 2017-05-20T20:00:00Z -> 2017-05-20T21:00:00Z
Oct 14, 2021 @ 11:00:12.757	CIS->S3 Poll Ended, desiredCount:14 statsResult:14 duration:501
Oct 14, 2021 @ 11:00:12.757	Completed to CIS->S3 day 2017-05-20T00:00:00Z sync of 14 records in 1179 msecs. Stats: {"submittedToQueue":14,"adoptedByWorkerThread":14,"missingFromSource":0,"newlyCreatedInTarget":0,"alreadyExistedInTarget":14,"exceptionCounter":0}
Oct 14, 2021 @ 11:00:12.776	CIS->S3 Sync'ing 6 AuditEvents for perflab-05191440 day 2017-05-21T00:00:00Z -> 2017-05-21T23:59:59.999Z
Oct 14, 2021 @ 11:00:13.008	CIS->S3 Dispatching 3 AuditEvents for perflab-05191440 in hour 2017-05-21T08:00:00Z -> 2017-05-21T09:00:00Z
Oct 14, 2021 @ 11:00:13.296	CIS->S3 Dispatching 3 AuditEvents for perflab-05191440 in hour 2017-05-21T20:00:00Z -> 2017-05-21T21:00:00Z
Oct 14, 2021 @ 11:00:13.856	CIS->S3 Poll Ended, desiredCount:6 statsResult:6 duration:500
Oct 14, 2021 @ 11:00:13.857	Completed to CIS->S3 day 2017-05-21T00:00:00Z sync of 6 records in 1080 msecs. Stats: {"submittedToQueue":6,"adoptedByWorkerThread":6,"missingFromSource":0,"newlyCreatedInTarget":0,"alreadyExistedInTarget":6,"exceptionCounter":0}
```

From Development, showing thread names:
```
XNIO-1: HTTP Request to CIS->S3 Bulk Sync org: perflab-ahampton
cis2s3-job-0: Starting CIS->S3 Sync for perflab-ahampton
cis2s3-job-0: CIS->S3 using earliestDate of: 2020-07-15T16:17:07.936Z for org:perflab-ahampton
...
cis2s3-job-0: CIS->S3 Dispatching 80 AuditEvents for perflab-ahampton in hour 2020-07-15T16:00:00Z -> 2020-07-15T17:00:00Z
cis2s3-s3io-0: NewlyCreated in S3 AuditEvent.id:2c91809172f0f2e401735343ef600a68 // These are DEBUG level now!
cis2s3-s3io-0: NewlyCreated in S3 AuditEvent.id:2c91809172f0f2e401735343ef6b0a6a // These are DEBUG level now!
...
cis2s3-job-0: CIS->S3 Poll Ended, desiredCount:6 statsResult:6 duration:500
cis2s3-job-0: Completed to CIS->S3 day 2020-07-19T00:00:00Z sync of 6 records in 34554 msecs. Stats: {"submittedToQueue":6,"adoptedByWorkerThread":6,"missingFromSource":0,"newlyCreatedInTarget":6,"alreadyExistedInTarget":0,"exceptionCounter":0}
...
cis2s3-job-0: Completed to CIS->S3 day 2020-07-23T00:00:00Z sync of 28 records in 40817 msecs. Stats: {"submittedToQueue":28,"adoptedByWorkerThread":28,"missingFromSource":0,"newlyCreatedInTarget":28,"alreadyExistedInTarget":0,"exceptionCounter":0}
...
cis2s3-job-0: Completed to CIS->S3 day 2020-07-28T16:17:07.936Z sync of 6 records in 35672 msecs. Stats: {"submittedToQueue":6,"adoptedByWorkerThread":6,"missingFromSource":0,"newlyCreatedInTarget":0,"alreadyExistedInTarget":6,"exceptionCounter":0}
...
```

The design of the sync job is to be "wide and slow".  We are not in a particular rush to move this data; it needs to be 
done but not at the expense of availability of the CIS MySQL Database for production uses.  The way this ETL works is it 
gets the AuditEvent IDs for a given hour, and then the worker thread pulls records one-at-a-time reading the AuditEvent 
from either the main or the `_archive` table, and then dispatches it for persistence S3.  There is a lot of thread 
hand off in AER, and a lot of blocking for network I/O from CIS to let CIS breathe a little bit.  All operations to CIS
are read-only, and the only cursors opened are for the IDs for one hour's worth of audit events.

## Older (pre-2021) Documentation
### Endpoints
#### Audit Events List
GET ```/audit/auditEvents```

> list the audit events 

#### Audit Reports List
GET ```/audit/auditReports/list/{type}```

> list of the previous audit reports run ```["types", "action", "user" ]```

#### Bulk Sync events to Search
POST ```/audit/bulkSync```

> Starts the bulk sync of CIS events into search 
* `days` : (default - 90) specify how many days of event data you are syncing
* `action` : specifiy an Action you want to sync. 
* `type` : Specifiy a report type of audit event data to sync. 
* `purgeOrg` : (default - false) Clear the search data before syncing new events.  

#### Audit Events Message Handler

>Endpoint to funnel audit events into CIS db. Single point of logging entry of all audit events. 
 Allows for better classifications and 

