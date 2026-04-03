package com.sportstock.league.mapper;

import com.sportstock.common.dto.league.LeagueInviteResponse;
import com.sportstock.common.dto.league.LeagueMemberResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.league.entity.League;
import com.sportstock.league.entity.LeagueInvite;
import com.sportstock.league.entity.LeagueMember;

public final class DtoMapper {

  private DtoMapper() {}

  public static LeagueResponse toLeagueResponse(League entity, int memberCount) {
    return new LeagueResponse(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getName(),
        entity.getStatus().name(),
        entity.getMaxMembers(),
        entity.getSeasonStartAt(),
        entity.getSeasonEndAt(),
        entity.getInitialStipendAmount(),
        entity.getWeeklyStipendAmount(),
        entity.getStartedAt(),
        entity.getCreatedAt(),
        memberCount);
  }

  public static LeagueMemberResponse toLeagueMemberResponse(LeagueMember entity) {
    return new LeagueMemberResponse(
        entity.getId(), entity.getUserId(), entity.getRole(), entity.getJoinedAt());
  }

  public static LeagueInviteResponse toLeagueInviteResponse(LeagueInvite entity) {
    return new LeagueInviteResponse(
        entity.getId(),
        entity.getCode(),
        entity.getExpiresAt(),
        entity.getMaxUses(),
        entity.getUsesCount(),
        entity.getCreatedAt());
  }
}
