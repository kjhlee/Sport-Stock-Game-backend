Site API (General Data):
https://site.api.espn.com/apis/site/v2/sports/{sport}/{league}/{resource}
Resource	Path
Scoreboard	/scoreboard
Teams	/teams
Team Detail	/teams/{id}
Standings	/standings
News	/news
Game Summary	/summary?event={id}

Core API (Detailed Data):
https://sports.core.api.espn.com/v2/sports/{sport}/leagues/{league}/{resource}
Resource	Path
Athletes	/athletes?limit=1000
Seasons	/seasons
Events	/events?dates=2024
Odds	/events/{id}/competitions/{id}/odds