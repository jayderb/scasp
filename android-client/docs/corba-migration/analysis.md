# CORBA legacy interoperability and migration dossier

## CORBA components
- **ORB (Object Request Broker):** locates the remote object and transports invocation requests and replies.
- **IDL:** language-neutral contract defining operations and data types.
- **Object reference:** identifies the remote CORBA object.
- **Naming service:** allows clients to find a named object reference.
- **Stub:** client-side proxy that packages calls.
- **Skeleton/servant dispatch:** server-side mechanism that receives and dispatches calls to the servant implementation.

## Invocation flow
1. Client obtains the ORB and resolves the AcademicAdmin name through the naming service.
2. The client receives an object reference and invokes `countRegistrations`.
3. The client stub marshals the course code and sends the request through the ORB.
4. The server ORB dispatches the call to the servant.
5. The servant obtains the count from the platform service and returns it.
6. The ORB marshals the result and sends it to the client stub.

## Comparison
| Area | CORBA | Java RMI | HTTP/REST |
|---|---|---|---|
| Language support | Language-neutral IDL | Primarily Java | Language-neutral |
| Coupling | IDL contract and ORB | Java interfaces/classes | URI, methods and payload formats |
| Deployment | ORB and naming service | RMI registry and JVMs | Web server/API gateway |
| Security | ORB security configuration | Java transport/security controls | TLS, authentication and authorisation |
| Maintainability | More infrastructure and legacy skills | Convenient in Java-only systems | Broad tooling and client support |

## Migration strategy
Use an adapter/facade. The old CORBA client continues calling a compatibility adapter. The adapter translates IDL data types into SCASP domain requests and invokes the new HTTP or Java service. New clients use HTTP directly. Migrate one operation at a time, compare outputs, monitor failures, then retire the CORBA adapter after legacy clients have moved.

## Risks and controls
- Data type mismatch: define explicit mapping and validation.
- Authentication differences: map legacy identities to current roles.
- Transaction boundaries: keep registration and audit updates atomic in the new service.
- Network and ORB compatibility: test in an isolated environment.
- Operational skills: document support and retirement procedures.

**Implementation note:** Java CORBA modules were removed from the standard JDK (JEP 320). This submission therefore provides the IDL and migration design; a third-party ORB demonstration is optional unless the lecturer supplies an environment.
