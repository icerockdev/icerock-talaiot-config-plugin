# About
Configuration of [Talaiot gradle plugin](https://github.com/cdsap/Talaiot/) with IceRock Development setup. 
All build statistics will be sent on IceRock's analytics database for analysis.

# Setup
in root `build.gradle.kts` (gradle 6.9+):
```kotlin
plugins {
  id("dev.icerock.gradle.talaiot") version("2.+")
}
```
# Development
In plugin catalog create a file `gradle.properties`  

add keys:  
`influx.url`  
`influx.org`  
`influx.bucket`  
`influx.token`  

for values you could use [influxdata.com](https://www.influxdata.com/) service
