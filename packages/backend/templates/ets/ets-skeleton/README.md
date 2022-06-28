# Event and Trigger Service (ETS)

Overview
--------
ETS is a microservice that mediates event triggers between our SaaS services and external webhooks.

For more information, see the [wiki](https://github.com/sailpoint/ets/wiki).

Building ETS
------------

* Build Tool: [Gradle 5.5.1](https://docs.gradle.org/5.5.1/userguide/userguide.html)
* Platform Dependency: [atlas-boot](https://github.com/sailpoint/atlas-boot)

#### Lombok Plugin

ETS uses the [Project Lombok](https://projectlombok.org/features/all) annotations to generate getters,
setters, builders, etc.

Lombok requires annotation processing, so in order to build ETS in IntelliJ, Lombok Plugin
(_Lombok by Michail Plushnikov_) must be installed first.

Setup instructions: ["Setting up Lombok with Eclipse and Intellij"](https://www.baeldung.com/lombok-ide)

After installing the plugin and restarting, IDE errors should disappear, and the project can be built using
Gradle as usual.

#### Testcontainers

ETS uses [Testcontainers](https://www.testcontainers.org/) framework to test repository classes
in a PostgreSQL DB running in a Docker container.

Therefore, it is required to have Docker Desktop running when building ETS.

[Download](https://www.docker.com/products/docker-desktop)

```shell script
# Verify Docker is running
docker info

# From root project directory
./gradlew clean build
```

Running ETS in Beacon Mode
--------------------------

Set up Beacon: ["IdentityNow Web Stack (Beacon)"](https://app.getguru.com/card/crk86dXi/IdentityNow-Web-Stack-Beacon)

```shell script
# From root project directory
export BEACON_TENANT=<tenant-name>:<vpn-name>;

# Ports 7100-7102 are also available
export SERVER_PORT=443;

export SPRING_PROFILES_ACTIVE=production,beacon;

./gradlew bootRun
```

Developer Guide
----------------
### Pre-requisites:
 - Java 11 JDK (Note: do not use any J9 flavors of the JDK)
 - Gradle

### If using an M1 Mac, make sure the Java SDK you're running the project with is x86_64 architecture

With sdkman: 
 - Open `~/.sdkman/etc/config`
 - Change the value `sdkman_rosetta2_compatible=false` to true
 - Now when you run `sdk list java`, you'll see versions of the jdk that use x86_64 architecture
 - Install one of the versions using `sdk install java <version>`
 - You can double-check by going to `~/.sdkman/candidates/java/<version>/release` and it should say `OS_ARCH="x86_64`"
 - This link has more info: https://itnext.io/how-to-install-x86-and-arm-jdks-on-the-mac-m1-apple-silicon-using-sdkman-872a5adc050d


