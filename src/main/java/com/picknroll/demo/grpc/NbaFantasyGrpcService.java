package com.picknroll.demo.grpc;

import com.picknroll.demo.models.dtos.IsLocked;
import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.services.DailyRosterServices;
import com.picknroll.demo.services.IsLockedServices;
import com.picknroll.demo.services.NbaPlayerServices;
import com.squareup.okhttp.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * gRPC service implementation for NBA Daily Fantasy operations.
 * This service provides the same functionality as the REST API but through gRPC protocol.
 */
@Service
public class NbaFantasyGrpcService extends NbaFantasyServiceGrpc.NbaFantasyServiceImplBase {

    private final NbaPlayerServices nbaPlayerServices;
    private final DailyRosterServices dailyRosterServices;
    private final IsLockedServices isLockedServices;
    private final OkHttpClient httpClient;

    @Value("${discord.client.id}")
    private String clientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    public NbaFantasyGrpcService(NbaPlayerServices nbaPlayerServices, 
                                DailyRosterServices dailyRosterServices,
                                IsLockedServices isLockedServices) {
        this.nbaPlayerServices = nbaPlayerServices;
        this.dailyRosterServices = dailyRosterServices;
        this.isLockedServices = isLockedServices;
        this.httpClient = new OkHttpClient();
    }

    @Override
    public void getTodaysPlayers(GetPlayersRequest request, 
                                StreamObserver<GetPlayersResponse> responseObserver) {
        try {
            LocalDate date = request.hasDate() ? 
                LocalDate.parse(request.getDate()) : LocalDate.now();
            
            List<NbaPlayerTeam> players = nbaPlayerServices.getNbaPlayersWithTeam(date);
            
            GetPlayersResponse.Builder responseBuilder = GetPlayersResponse.newBuilder();
            
            for (NbaPlayerTeam player : players) {
                NbaPlayer grpcPlayer = NbaPlayer.newBuilder()
                    .setNbaPlayerUid(player.getNba_player_uid().toString())
                    .setNbaPlayerId(player.getNba_player_id())
                    .setName(player.getName())
                    .setDate(player.getDate())
                    .setPosition(player.getPosition())
                    .setAgainstTeam(player.getAgainst_team())
                    .setDollarValue(player.getDollar_value())
                    .setFantasyScore(player.getFantasy_score())
                    .setTeamId(player.getTeam_id())
                    .setTeamName(player.getTeam_name())
                    .setAgainstTeamName(player.getAgainst_team_name())
                    .build();
                responseBuilder.addPlayers(grpcPlayer);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error getting today's players: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getPlayerRoster(GetRosterRequest request, 
                               StreamObserver<GetRosterResponse> responseObserver) {
        try {
            LocalDate date = request.hasDate() ? 
                LocalDate.parse(request.getDate()) : LocalDate.now();
            
            List<DailyRosterPlayer> roster = dailyRosterServices.getPlayerRoster(
                request.getDiscordPlayerId(), 
                request.getGuildId(), 
                date
            );
            
            GetRosterResponse.Builder responseBuilder = GetRosterResponse.newBuilder();
            
            for (DailyRosterPlayer player : roster) {
                RosterPlayer grpcPlayer = RosterPlayer.newBuilder()
                    .setDiscordPlayerId(player.getDiscordPlayerId())
                    .setNbaPlayerUid(player.getNbaPlayerUid().toString())
                    .setGuildId(player.getGuildId())
                    .setDate(player.getDate().toString())
                    .setNickname(player.getNickname())
                    .setPosition(player.getPosition().toString())
                    .setName(player.getName())
                    .setDollarValue(player.getDollarValue())
                    .setFantasyScore(player.getFantasyScore())
                    .build();
                responseBuilder.addPlayers(grpcPlayer);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error getting player roster: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void setRosterPlayer(SetRosterRequest request, 
                               StreamObserver<SetRosterResponse> responseObserver) {
        try {
            LocalDate date = LocalDate.parse(request.getDate());
            
            dailyRosterServices.saveRosterChoice(
                UUID.fromString(request.getNbaPlayerUid()),
                request.getDiscordPlayerId(),
                request.getGuildId(),
                request.getNickname(),
                request.getPosition(),
                date
            );
            
            SetRosterResponse response = SetRosterResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Roster player set successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            SetRosterResponse response = SetRosterResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Error setting roster player: " + e.getMessage())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLeaderboard(GetLeaderboardRequest request, 
                              StreamObserver<GetLeaderboardResponse> responseObserver) {
        try {
            LocalDate date = LocalDate.parse(request.getDate());
            
            List<DailyRosterPlayer> leaderboard = dailyRosterServices.getLeaderboard(
                request.getGuildId(), 
                date
            );
            
            GetLeaderboardResponse.Builder responseBuilder = GetLeaderboardResponse.newBuilder();
            
            // Group players by discord player for leaderboard entries
            // This is a simplified implementation - you might want to enhance this
            for (DailyRosterPlayer player : leaderboard) {
                LeaderboardEntry entry = LeaderboardEntry.newBuilder()
                    .setDiscordPlayerId(player.getDiscordPlayerId())
                    .setGuildId(player.getGuildId())
                    .setNickname(player.getNickname())
                    .setTotalFantasyScore(player.getFantasyScore())
                    .addPlayers(RosterPlayer.newBuilder()
                        .setDiscordPlayerId(player.getDiscordPlayerId())
                        .setNbaPlayerUid(player.getNbaPlayerUid().toString())
                        .setGuildId(player.getGuildId())
                        .setDate(player.getDate().toString())
                        .setNickname(player.getNickname())
                        .setPosition(player.getPosition().toString())
                        .setName(player.getName())
                        .setDollarValue(player.getDollarValue())
                        .setFantasyScore(player.getFantasyScore())
                        .build())
                    .build();
                responseBuilder.addEntries(entry);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error getting leaderboard: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void getGlobalLeaderboard(GetGlobalLeaderboardRequest request, 
                                    StreamObserver<GetGlobalLeaderboardResponse> responseObserver) {
        try {
            LocalDate date = LocalDate.parse(request.getDate());
            
            List<DailyRosterPlayer> globalLeaderboard = dailyRosterServices.getGlobalLeaderboard(date);
            
            GetGlobalLeaderboardResponse.Builder responseBuilder = GetGlobalLeaderboardResponse.newBuilder();
            
            for (DailyRosterPlayer player : globalLeaderboard) {
                LeaderboardEntry entry = LeaderboardEntry.newBuilder()
                    .setDiscordPlayerId(player.getDiscordPlayerId())
                    .setGuildId(player.getGuildId())
                    .setNickname(player.getNickname())
                    .setTotalFantasyScore(player.getFantasyScore())
                    .addPlayers(RosterPlayer.newBuilder()
                        .setDiscordPlayerId(player.getDiscordPlayerId())
                        .setNbaPlayerUid(player.getNbaPlayerUid().toString())
                        .setGuildId(player.getGuildId())
                        .setDate(player.getDate().toString())
                        .setNickname(player.getNickname())
                        .setPosition(player.getPosition().toString())
                        .setName(player.getName())
                        .setDollarValue(player.getDollarValue())
                        .setFantasyScore(player.getFantasyScore())
                        .build())
                    .build();
                responseBuilder.addEntries(entry);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error getting global leaderboard: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void isLocked(IsLockedRequest request, 
                        StreamObserver<IsLockedResponse> responseObserver) {
        try {
            LocalDate date = request.hasDate() ? 
                LocalDate.parse(request.getDate()) : LocalDate.now();
            
            IsLocked lockStatus = isLockedServices.isLocked(date);
            
            IsLockedResponse.Builder responseBuilder = IsLockedResponse.newBuilder()
                .setIsLocked(lockStatus != null && lockStatus.getLockTime().isAfter(java.time.OffsetDateTime.now()))
                .setDate(date.toString());
            
            if (lockStatus != null) {
                responseBuilder.setLockTime(lockStatus.getLockTime().toString());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error checking lock status: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    @SneakyThrows
    public void getUserInfo(GetUserInfoRequest request, 
                           StreamObserver<GetUserInfoResponse> responseObserver) {
        try {
            Request httpRequest = new Request.Builder()
                .url("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer " + request.getAccessToken())
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String userInfo = response.body() != null ? response.body().string() : response.message();
                
                GetUserInfoResponse grpcResponse = GetUserInfoResponse.newBuilder()
                    .setSuccess(response.isSuccessful())
                    .setUserInfo(userInfo)
                    .setMessage(response.isSuccessful() ? "Success" : "Failed to get user info")
                    .build();
                
                responseObserver.onNext(grpcResponse);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            GetUserInfoResponse response = GetUserInfoResponse.newBuilder()
                .setSuccess(false)
                .setUserInfo("")
                .setMessage("Error getting user info: " + e.getMessage())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}