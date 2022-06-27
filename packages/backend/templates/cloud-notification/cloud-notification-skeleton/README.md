# Notification Service aka "Hermes"

This repo is home to our notification service.  Also known as "Hermes", this is an atlas microservice that subscribes to events to dispatch notifications.  It currently supports emails, Slack and MS Teams.  It is architected with the potential to absorb the likes of UI and SMS notifications. 

## Integrator Quick-Start Guide

### Pre-requisites:
The prerequisite to creating a notification is an Atlas Event that contains the recipient's CIS id. In SaaS Notification parlance, this is called a domain event. 

### Main steps: 
1. Update the [interest matcher](notification-interest-matcher/src/main/resources/com/sailpoint/notification/interest/matcher/repository/impl/json/interestsRepository.json) in this repo with
the appropriate topic name, event type and the discovery config to locate the recipient Id [and a few other properties](notification-interest-matcher/src/main/java/com/sailpoint/notification/interest/matcher/interest/Interest.java) 
    * The enabled property is a special property -- see point 3 below.
  
2. Include a default template in the [default template repository](notification-template/common/src/main/resources/com/sailpoint/notification/template/common/repository/impl/json/templates.json).
    * Tenants can create their own templates using the V3 API, or the UI if it's made available.
    * Internally, one can use the org-portal to modify templates.
     
3. Both opt-in and opt-out are supported.
    * For opt-in start with enabled=false and use the V3 API to set a override preference per tenant. Note: Preference per user is not supported yet.
    * For opt-out, do the opposite

### Guide to writing templates:
Hermes uses velocity templates ([link to guide](https://velocity.apache.org/engine/2.0/user-guide.html)). One of the main features of velocity templates is the ability to access variables and methods from the context. Hermes adds the following to the context:
a. The contents of the domain event  
    The contents of the domain event are available in the outer context.    
    As an example, if your domain event is {"foo":"bar", "user":"12345" ... }, you can access these values in the template with $foo and $user   
b. Recipient data    
    The recipient data -- id, name, email and phone are available as a map in an inner context `"__recipient"`. These can be accessed in the template like `$__recipient.name`
c. Global context  
    The global context data -- productUrl, productName, emailFromAddress are available as a map in an inner context `"__global"`. These can be accessed in the template like `$__global.productName`    
d. Platform Util    
    The util adds the class [TemplateUtil](notification-template/template/src/main/java/com/sailpoint/notification/template/util/TemplateUtil.java) to the `"__util"` namespace. Please refer to the javadoc for what the methods do.      
    Example usage: `$__util.getUser($someId).getName()`  
e. The domain event as a JSON is available as `$__contentJson`   

## V3 Endpoints

### Templates
| HTTP Method | URI                                 | Input Model | Status Code(s) | Response Model | Authorization Notes            |
|-------------|-------------------------------------|-------------|----------------|----------------|--------------------------------|
| GET         | /notification-template-defaults     | ​            | 200, 400, 500  | List           | SecurityGroups: ORG_ADMIN, API |
| GET         | /notification-templates             | ​            | 200, 400, 500  | List           | SecurityGroups: ORG_ADMIN, API |
| GET         | /notification-templates/{id}        | ​            | 200, 404, 500  | TemplateDto    | SecurityGroups: ORG_ADMIN, API |
| POST        | /notification-templates             | TemplateDto | 201, 400, 500  | TemplateDto    | SecurityGroups: ORG_ADMIN, API |
| POST        | /notification-templates/bulk-delete | List        | 204, 400, 500  | ​               | SecurityGroups: ORG_ADMIN, API |

### Preferences
| HTTP Method | URI                             | Input Model    | Status Code(s) | Response Model | Authorization Notes            |
|-------------|---------------------------------|----------------|----------------|----------------|--------------------------------|
| GET         | /notification-preferences/{key} | ​               | 200, 404       | PreferencesDto | SecurityGroups: ORG_ADMIN, API |
| PUT         | /notification-preferences/{key} | PreferencesDto | 200, 400, 404  | PreferencesDto | SecurityGroups: ORG_ADMIN, API |

### Custom From Emails

[RFC](https://github.com/sailpoint/saas-rfcs/pull/11)

[API Specification](https://api.identitynow.com/?urls.primaryName=Beta#/Notifications)

| HTTP Method | URI                           | Description                                   |
|-------------|-------------------------------|-----------------------------------------------|
| GET         | /verified-from-addresses      | Get a list of custom sender emails for tenant |
| POST        | /verified-from-addresses      | Create a custom sender email for tenant       |
| DELETE      | /verified-from/addresses/{id} | Delete a custom sender email for tenant       |


## Environment variables specific to Hermes
Other than Atlas environment variables, the following environment variables are specific to hermes
| Variable name         | Description                                   | Default value                                                     |
| AWS_SES_REGION        | The region for the SES client                 | us-east-1                                                         |
| AWS_SES_SOURCE_ARN    | The SourceArn to use in SES SendMail requests | arn:aws:ses:<ses-region>:<aws-accountid>>:identity/sailpoint.com  |

## Further Readings
1. Architecture doc https://app.getguru.com/card/iKLMgz5T/A-CrossProduct-Notification-Service
2. Custom From Emails API Documentation: https://app.getguru.com/card/igddjqLT/Hermes-Custom-From-Address-API-Internal
   
## Developer Guide
   
### Pre-requisites:
- Java 11 JDK (Note: do not use any J9 flavors of the JDK)
- Gradle
   
### IntelliJ IDEA Configuration (w/ Beacon Mode)
Set the follow to an 11.x JDK flavor (non-J9)

| Setting Location | Setting Name |
|---------| -------------| 
| IntelliJ IDEA > Preferences > Build, Execution, Deployment > Compiler > Java Compiler | *Project Bytecode Version* |
| IntelliJ IDEA > Preferences > Build, Execution, Deployment > Build Tools > Gradle | "Gradle JVM* |
| File > Project Structure > Project Settings > Project | *Project SDK* |

To set up a run profile go to `Edit Configurations...` > `Add New Configuration` > `Gradle Build` and configure as follows:
   
| Config | Value | 
|--------|-------|
| Name | cloud-notification [run] | 
| Run | `run` |
| Gradle project | cloud-notification |
| Environment Variables | |
| `BEACON_TENANT` | `<org name>:<developer vpn name>` |
| `ATLAS_JWT_KEY_PARAM_NAME` | `/service/oathkeeper/dev/encryption_string` |
| `SERVER_PORT` | e.g. 7100 (Only needed if you are running multiple services locally) |
| Include system environment variables |
