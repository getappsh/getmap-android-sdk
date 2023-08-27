# GETAPP-ANDROID-SDK
GETAPP SDK is the api defined frm the application point of view.

## Getting started

Just compile and use accordingly:
- the api is in /app/sdk - provides a .aar output

### API flow

```mermaid
sequenceDiagram
    participant User
    participant SDK as GetMapServiceImpl
    participant Server

    User->>SDK: init(configuration, statusCode)
    SDK-->>User: Returns boolean (success/failure)

    User->>SDK: createMapImport(mapProperties)
    SDK-->>Server: Request to create map import
    Server-->>SDK: CreateMapImportStatus
    SDK-->>User: Returns CreateMapImportStatus

    User->>SDK: getCreateMapImportStatus(importRequestId)
    SDK-->>Server: Request map import status
    Server-->>SDK: Returns CreateMapImportStatus
    SDK-->>User: Returns CreateMapImportStatus

    User->>SDK: setMapImportDeliveryStart(importRequestId)
    SDK-->>Server: Start map delivery
    Server-->>SDK: Returns MapImportDeliveryStatus
    SDK-->>User: Returns MapImportDeliveryStatus

    User->>SDK: setMapImportDeploy(importRequestId, deployState)
    SDK-->>Server: Deploy map
    Server-->>SDK: Returns MapDeployState
    SDK-->>User: Returns MapDeployState
```
