This section provides information about a feature flag behind each trigger.


## Overview
The release process of trigger also includes changes to feature flags. You will also need to update trigger
ownership in a confluence document so that others know who to ask for if they want to enable triggers for
orgs. For details about trigger release process, make sure you follow this confluence document after creating
trigger JSON files in ETS.
https://sailpoint.atlassian.net/wiki/spaces/SAAS/pages/298288570/ETS+trigger+release+process

## Trigger Feature Flags
Each trigger is accompanied by a feature flag with a predefined naming scheme.

Any interaction with a trigger is enabled only when a corresponding feature flag is turned on for the org.

Here is an example of the *idn:identity-created* trigger's feature flag:

```shell script
  name                                 : ETS_IDN_IDENTITY_CREATED
  key                                  : ETS_IDN_IDENTITY_CREATED
  kind                                 : boolean
  environments.production.on           : false
  default                              : true
  description                          : Control visibility of ETS trigger idn:identity-created for tenants
  tags                                 : []
  environments.production.lastModified : 04/09/2020 04:44:16 PM CDT
  creationDate                         : 04/09/2020 04:44:16 PM CDT

  Feature flag turned off in production, returning: false
```

> The feature flag MUST follow the naming scheme of ETS-prefix followed by all caps, snakecase trigger name.

The trigger's feature flag can be turned on by default in dev and test environment but must be reviewed and approved for
production in [saas-feature-flags](https://github.com/sailpoint/saas-feature-flags).
