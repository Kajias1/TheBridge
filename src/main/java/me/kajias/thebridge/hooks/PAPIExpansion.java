package me.kajias.thebridge.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.enums.TeamColor;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.objects.Game;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PAPIExpansion extends PlaceholderExpansion
{
    private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

    @Override
    public @NotNull String getIdentifier() {
        return "thebridge";
    }

    @Override
    public @NotNull String getAuthor() {
        return "KAJIAS";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(@NotNull OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            Arena arena = Arena.getPlayerArenaMap().get(player);
            GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());

            switch (params.toLowerCase()) {
                case "games_played": return String.valueOf(gamePlayer.getGamesPlayed());
                case "games_won": return String.valueOf(gamePlayer.getGamesWon());
                case "goals": return String.valueOf(gamePlayer.getGoals());
                case "kills": return String.valueOf(gamePlayer.getKills());
            }

            if (arena != null) {
                switch (params.toLowerCase()) {
                    case "arena_players_total": return String.valueOf(arena.getAllowedPlayerAmount());
                    case "arena_players": return String.valueOf(arena.getPlayers().size());
                    case "arena_name": return arena.getName();
                    case "arena_type": return arena.getTypeString();
                    case "game_type": return arena.getGameTypeString();
                }

                Game game = arena.getGame();
                if (game != null) {
                    switch (params.toLowerCase()) {
                        case "game_start_countdown": return String.valueOf(game.getCountDown());
                        case "game_time": return Utils.convertToTime(game.getGameTime());
                        case "game_winner_team":
                            if (game.getWinnerTeamColor() == null) return config.getString("messages.draw-title");
                            if (arena.getTeamColor(player.getPlayer()) == game.getWinnerTeamColor()) return config.getString("messages.victory-title");
                            return config.getString("messages.defeat-title");
                        case "game_red_scores":
                        case "game_blue_scores":
                        case "game_green_scores":
                        case "game_yellow_scores":
                            TeamColor teamColor = TeamColor.valueOf(params.split("_")[1].toUpperCase());
                            try {
                                String line;
                                String scoreFormat = "scores";
                                if (arena.getGameType() == GameType.LIVES) {
                                    line = Utils.getColorCode(teamColor) + new String(new char[game.getTeamLivesMap().get(teamColor)])
                                                + Utils.getColorCode(null) + new String(new char[game.getMaxTeamLives() - game.getTeamLivesMap().get(teamColor)]);
                                    scoreFormat = "lives";
                                } else {
                                        line = Utils.getColorCode(teamColor) + new String(new char[game.getTeamScoresMap().get(teamColor)])
                                                + Utils.getColorCode(null) + new String(new char[config.getInt("game-config.rounds") - game.getTeamScoresMap().get(teamColor)]);
                                }
                                return line.replaceAll("\0", config.getString("messages.team-scores-format." + scoreFormat));
                            } catch (Exception ex) {
                                return "none team";
                            }
                        case "game_round":
                            return config.getString("messages.game-rounds-format")
                                .replace("%current_round%", String.valueOf(game.getRound()))
                                .replace("%max_rounds%", String.valueOf(config.getInt("game-config.rounds")));
                        case "game_kills":
                            return String.valueOf(game.getCurrentRoundPlayersData().stream()
                                    .filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(new GamePlayer(player.getUniqueId())).getKills());
                        case "game_goals":
                            return String.valueOf(game.getCurrentRoundPlayersData().stream()
                                    .filter(x -> x.getUniqueId().equals(player.getUniqueId())).findAny().orElse(new GamePlayer(player.getUniqueId())).getGoals());
                    }
                }
            }
        }
        return "Not found";
    }

    public static void registerHook() {
        new PAPIExpansion().register();
    }
}
