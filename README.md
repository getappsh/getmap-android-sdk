# GETAPP-ANDROID-SDK
GETAPP SDK is the api defined frm the application point of view.

## Getting started

Just compile and use accordingly:
- The api is in `sdk/build/outputs/aar` + `getAppClient/build/outputs/aar`

### API flow

```mermaid
sequenceDiagram
    participant User
    participant SDK as GetMapServiceImpl
    participant Server

    User->>SDK: init(configuration, statusCode)
    activate SDK
    SDK-->>Server: POST /api/login
    Server-->>SDK: 200 ok
    SDK-->>User: Returns boolean (success/failure)
    deactivate SDK

    note over SDK: TODO add discovery (which depends on getPolygonPats from MapColonies)
    note over SDK: TODO given polygon, resolve what import request we need to create

    loop for every importRequest in polygon
    User->>SDK: createMapImport(mapProperties)
    activate SDK
    note over SDK: TODO add "StampManagementLogic" here to resolve what stamps are needed from given polygon
    SDK-->>Server: POST /api/map/import/create Request to create map import
    Server-->>SDK: CreateMapImportStatus
    SDK-->>User: Returns CreateMapImportStatus
    deactivate SDK
    end

    loop for every importRequest in polygon
    User->>SDK: getCreateMapImportStatus(importRequestId)
    activate SDK
    SDK-->>Server: Request map import status
    Server-->>SDK: Returns CreateMapImportStatus
    SDK-->>User: Returns CreateMapImportStatus
    deactivate SDK
    end

    loop for every importRequest in polygon
    User->>SDK: setMapImportDeliveryStart(importRequestId)
    activate SDK
    SDK-->>Server: GET /api/map/import/status/{importRequestId} Start map delivery
    Server-->>SDK: Returns MapImportDeliveryStatus

    SDK->>Server: GET (Url from MapImportDeliveryStatus.packageUrl)
    note over Server: TODO what about s3?
    Server-->>SDK: file
    note over SDK, Server: TODO currently we only support sngle stamp per request
    SDK-->>User: Returns MapImportDeliveryStatus
    deactivate SDK
    end

    note over SDK: TODO
    User->>SDK: setMapImportDeploy(importRequestId, deployState)
    activate SDK
    SDK-->>Server: Deploy map
    Server-->>SDK: Returns MapDeployState
    SDK-->>User: Returns MapDeployState
    note over SDK: TODO
    deactivate SDK
```

# OpenApi code generator

Running:

```shell
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli generate -i http://getapp-dev.getapp.sh:3000/docs-yaml -g kotlin --additional-properties=packageName=GetApp.Client,packageVersion=1.0 -o /local/GetApp.Client.Kotlin
```

OR

```shell
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli generate -i local/docs-yaml -g kotlin --additional-properties=packageName=GetApp.Client,packageVersion=1.0 -o /local/GetApp.Client.Kotlin
```

In case owner is not $USER - change ownership of generated directory:

```shell
sudo chown -R $USER GetApp.Client.Kotlin/
```

For config options, see:
https://openapi-generator.tech/docs/generators/kotlin

## Add to SDK

copy-paste `kotlin` directory into `app/sdk/src/main`

# Documentation

To generate KDoc run: 

```cmd
gradlew dokkaHtml
```

https://kotlinlang.org/docs/dokka-gradle.html#single-project-builds

***

Emulator stopped to run from Android Studio from some reason...
Run same command Android Studio uses from terminal instead:

```shell
D:\DevTools\Android\Sdk\emulator\emulator.exe -netdelay none -netspeed full -avd Pixel_5_API_33 -qt-hide-window -grpc-use-token -idle-grpc-timeout 300
```

https://developer.android.com/studio/emulator_archive
