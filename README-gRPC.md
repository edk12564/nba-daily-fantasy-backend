# gRPC Integration for NBA Daily Fantasy Backend

This document describes the gRPC integration added to the NBA Daily Fantasy backend application.

## Overview

The application now supports both REST API and gRPC protocols simultaneously:
- **REST API**: Port 80 (existing functionality)
- **gRPC**: Port 9090 (new gRPC services)

## gRPC Services

The gRPC service provides the same functionality as the REST API through the `NbaFantasyService`:

### Available gRPC Methods

1. **GetTodaysPlayers** - Get today's available NBA players
2. **GetPlayerRoster** - Get a player's current roster
3. **SetRosterPlayer** - Set a player in the roster
4. **GetLeaderboard** - Get server leaderboard
5. **GetGlobalLeaderboard** - Get global leaderboard
6. **IsLocked** - Check if rosters are locked
7. **GetUserInfo** - Get Discord user information

## Protocol Buffer Definition

The service is defined in `src/main/proto/nba-fantasy.proto` with the following structure:

```protobuf
service NbaFantasyService {
  rpc GetTodaysPlayers(GetPlayersRequest) returns (GetPlayersResponse);
  rpc GetPlayerRoster(GetRosterRequest) returns (GetRosterResponse);
  rpc SetRosterPlayer(SetRosterRequest) returns (SetRosterResponse);
  rpc GetLeaderboard(GetLeaderboardRequest) returns (GetLeaderboardResponse);
  rpc GetGlobalLeaderboard(GetGlobalLeaderboardRequest) returns (GetGlobalLeaderboardResponse);
  rpc IsLocked(IsLockedRequest) returns (IsLockedResponse);
  rpc GetUserInfo(GetUserInfoRequest) returns (GetUserInfoResponse);
}
```

## Configuration

### Maven Dependencies Added

```xml
<!-- gRPC Dependencies -->
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>2.15.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-server-spring-boot-starter</artifactId>
    <version>2.15.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.21.12</version>
</dependency>
```

### Application Properties

```properties
# gRPC Server Configuration
grpc.server.port=9090
grpc.server.address=0.0.0.0
```

## Building and Running

### Generate Protocol Buffer Classes

```bash
mvn clean compile
```

This will automatically generate the Java classes from the `.proto` file.

### Running the Application

```bash
mvn spring-boot:run
```

The application will start both:
- REST API server on port 80
- gRPC server on port 9090

## Testing gRPC Services

### Using grpcurl (Command Line)

```bash
# Install grpcurl
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest

# List available services
grpcurl -plaintext localhost:9090 list

# Test GetTodaysPlayers
grpcurl -plaintext -d '{}' localhost:9090 com.picknroll.demo.grpc.NbaFantasyService/GetTodaysPlayers

# Test GetPlayerRoster
grpcurl -plaintext -d '{"guild_id": "123", "discord_player_id": "456"}' localhost:9090 com.picknroll.demo.grpc.NbaFantasyService/GetPlayerRoster
```

### Using Java gRPC Client

```java
import com.picknroll.demo.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClientExample {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
            .usePlaintext()
            .build();

        NbaFantasyServiceGrpc.NbaFantasyServiceBlockingStub stub = 
            NbaFantasyServiceGrpc.newBlockingStub(channel);

        // Get today's players
        GetPlayersResponse response = stub.getTodaysPlayers(
            GetPlayersRequest.newBuilder().build()
        );
        
        System.out.println("Players: " + response.getPlayersList());
        
        channel.shutdown();
    }
}
```

## Architecture Benefits

1. **Dual Protocol Support**: Same business logic exposed via both REST and gRPC
2. **Performance**: gRPC offers better performance for internal service communication
3. **Type Safety**: Protocol buffers provide strong typing and schema evolution
4. **Streaming**: gRPC supports streaming (not implemented in this basic setup)
5. **Code Generation**: Automatic client/server code generation from `.proto` files

## File Structure

```
src/main/
├── proto/
│   └── nba-fantasy.proto          # Protocol buffer definition
├── java/com/picknroll/demo/
│   ├── grpc/
│   │   ├── NbaFantasyGrpcService.java    # gRPC service implementation
│   │   └── GrpcServerConfig.java         # gRPC configuration
│   └── ... (existing REST controllers and services)
└── resources/
    └── application.properties      # Updated with gRPC config
```

## Notes

- The gRPC service uses the same underlying service classes as the REST API
- No modifications were made to existing REST API code
- gRPC server runs independently on port 9090
- Both protocols can be used simultaneously without conflicts
- Protocol buffer classes are generated during Maven compilation
