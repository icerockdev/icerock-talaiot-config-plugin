# About
Configuration of [Talaiot gradle plugin](https://github.com/cdsap/Talaiot/) with IceRock Development setup. 
All build statistics will be sent on IceRock's analytics database for analysis.

# Setup
in `settings.gradle.kts` (gradle 6.9+):
```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.icerock.gradle.talaiot") version("3.+")
}
```

# Development
In plugin catalog create a file `gradle.properties`  

add keys:  
```
influx.url=  
influx.org=
influx.bucket=  
influx.token= 
slack.webhook=
```

for values you could use [influxdata.com](https://www.influxdata.com/) service

before testing or Gradle Sync you should publish plugin to mavenLocal:
```shell
./gradlew -p plugin publishToMavenLocal
```
