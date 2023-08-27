# GetMap Server's Awareness Level

## Status
Proposed

## Context
In our system architecture, we have three primary components:

1. GetMap Server: Responsible for handling map-related operations.
2. GetApp Server: Interacts with GetMap and provides application-level services.
3. GetApp Client (SDK): A client-side library that interfaces with GetApp Server.
As we evolve our system and introduce new features and terminologies, it's crucial to define the boundaries and responsibilities of each component clearly.

## Decision
The GetMap Server will not be aware of:

1. The term "request". This term might be used in other parts of the system, but GetMap will not recognize or process it. The term request for multiple stamps is not handled by GETMAP server.
2. The operation "createDeliveryRequest". Any delivery-related operations will be abstracted away from GetMap.
Instead, the GetMap Server will be aware only at the level of a single geopackage file, which we also refer to as a "stamp". All operations that the GetMap Server handles will be based on these stamps, without any higher-level abstractions or terminologies.

## Rationale
Keeping the GetMap Server focused on single geopackage files (stamps) ensures:

1. Simplicity: By limiting the scope of GetMap's responsibilities, we can ensure that it does its job efficiently without unnecessary complexities.
2. Decoupling: This decision allows us to evolve the concepts of "extent" and "delivery requests" in the GetApp Server or Client without affecting the GetMap Server.
3. Scalability: Handling operations at the geopackage file level can lead to more granular control, better caching, and improved scalability.

## Implications
1. GetApp Server's Responsibility: Since GetMap is unaware of "extent" and "createDeliveryRequest", the GetApp Server will need to handle these concepts and translate them into operations that the GetMap Server understands.
2. SDK Adjustments: The GetApp Client (SDK) might need adjustments to ensure it communicates effectively with both servers, given their distinct responsibilities.