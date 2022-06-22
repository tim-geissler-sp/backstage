## Description
What is the intent of this change and why is it being made?

## How Has This Been Tested?
What testing have you done to verify this change?

Trigger Contributors:
- [ ] Manually tested trigger definition

Service Owners:
- [ ] Unit test
- [ ] Integration test
- [ ] e2e test

## Has a JSON trigger been added or changed?

### Anticipated use cases
- [ ] What use case(s) will the new trigger fulfill?
- [ ] How is the trigger you are adding differentiated from existing triggers?

### Generate ETS trigger DTOs
- [ ] I ran the DTO generator `python dto_generator.py`. (requires Python 3+)

###  Event schema tests added or updated
- [ ] There exist automated tests in the upstream service's build pipeline that ensure the schema of events
are consistent with what's being consumed by ETS.

### Event Expectations
- [ ] Median and max event size - _help us understand the shape of events ETS will
be consuming._
- [ ] Average and max event volume - _help us understand the volume of events ETS
will be consuming.  Max event volume includes any spiky event traffic you might
expect._

## How can this change be verified in dev/stage/production?
e.g Looking for certain data in logs, monitoring resources

- [ ] Logs (Please include kibana link)
- [ ] Grafana (Please include dashboard link/shapshot)

## Deployment requirements
Are there any special steps that must be taken when rolling this change out?

- [ ] Changes are behind a feature flag
- [ ] Breaking Changes
- [ ] New envvars
