package me.kajias.thebridge.objects;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.enums.BlockColorType;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.listeners.PlayerDamage;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.TeamColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class Game
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
   private static final int defaultStartCountDown = config.getInt("game-config.start-countdown");
   private static final int gameDuration = config.getInt("game-config.game-duration");
   private static final int maxRounds = config.getInt("game-config.rounds");

   private final HashMap<Player, Location> playerLockedPositionMap;
   private final Arena arena;

   private boolean isStarted;
   private int countDown;
   private int nextRoundCountDown;
   private int gameTime;
   private BukkitTask startCountDownTask;
   private BukkitTask nextRoundStartCountDownTask;
   private BukkitTask gameTimerTask;
   private TeamColor winnerTeamColor;
   private final HashMap<TeamColor, Integer> teamScoresMap;
   private final HashMap<TeamColor, Integer> teamLivesMap;
   private final List<GamePlayer> currentRoundPlayersData;

   private final List<Player> deadPlayers;
   private int round;
   private int maxTeamLives = 8;

   public Game(Arena arena) {
      this.arena = arena;
      isStarted = false;
      startCountDownTask = null;
      gameTimerTask = null;
      winnerTeamColor = null;
      round = 0;
      teamScoresMap = new HashMap<>();
      teamLivesMap = new HashMap<>();
      playerLockedPositionMap = new HashMap<>();
      currentRoundPlayersData = new ArrayList<>();
      deadPlayers = new ArrayList<>();

      arena.getTeamMap().keySet().forEach(x -> teamScoresMap.put(x, 0));
      arena.getTeamMap().keySet().forEach(x -> teamLivesMap.put(x, 2));
      if (teamLivesMap.size() <= 2) {
         arena.getTeamMap().keySet().forEach(x -> teamLivesMap.put(x, 3));
         maxTeamLives = 6;
      }
   }

   public void startCountDownTimer(int startCountDown) {
      countDown = startCountDown;
      arena.setState(ArenaState.STARTING);

      for (Player player : arena.getPlayers()) {
         if (player.isOp()) {
            ItemStack fastStartItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.fast-start.material")));
            ItemMeta fastStartItemMeta = fastStartItem.getItemMeta();
            fastStartItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.fast-start.name")));
            fastStartItem.setItemMeta(fastStartItemMeta);
            player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.fast-start.slot") - 1, fastStartItem);
         }
      }

      startCountDownTask = Bukkit.getScheduler().runTaskTimer(TheBridge.INSTANCE, () -> {
         if (countDown <= 0) {
            startGame();
         }

         for (Player player : arena.getPlayers()) {
            player.setLevel(countDown);
            if (countDown == 30 || countDown == 15 || countDown == 10 || countDown <= 5) {
               Sounds.CLICK.play(player);
               Utils.sendMessage(player, config.getString("messages.start-countdown").replace("%time%", String.valueOf(countDown)));
            }
         }

         countDown--;
      }, 0L,  20L);
   }

   public void shortenCountDownTimer(int newValue) {
      if (countDown > newValue) countDown = newValue;
   }

   public void stopCountDownTimer() {
      arena.setState(ArenaState.WAITING);
      countDown = defaultStartCountDown;
      if (startCountDownTask != null) startCountDownTask.cancel();
      for (Player p : arena.getPlayers()) p.setLevel(0);
   }

   public void startGame() {
      if (!isStarted) {
         isStarted = true;
         arena.setState(ArenaState.STARTED);
         if (startCountDownTask != null) startCountDownTask.cancel();
         arena.getPlayers().forEach(player -> {
            PlayerDamage.lastToDamage.remove(player);
            currentRoundPlayersData.add(new GamePlayer(player.getUniqueId()));
            player.setGameMode(GameMode.SURVIVAL);
            player.setNoDamageTicks(20);
            if (PlayersData.getPlayerData(player.getUniqueId()).getBoughtBonuses()
                    .containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.double-jump.name")))) {
               player.setAllowFlight(true);
               player.setFlying(false);
            }

            if (arena.getTeamColor(player) == null) {
               for (TeamColor teamColor : arena.getTeamMap().keySet()) {
                  if (arena.getTeamMap().get(teamColor).size() < arena.getAllowedPlayerAmount() / arena.getTeamMap().size()) {
                     arena.addToTeam(teamColor, player);
                  }
               }
            }
         });
         startNextRound(null);
      }

      gameTime = gameDuration;
      gameTimerTask = Bukkit.getScheduler().runTaskTimer(TheBridge.INSTANCE, () -> {
         if (gameTime <= 0) {
            endGame(null);
         }

         if (gameTime % config.getInt("game-config.teleport-to-base-item-drop-period") == 0) {
            for (Player player : arena.getPlayers()) {
               GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
               if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.teleport-to-base.name")))) {
                  if (!deadPlayers.contains(player) && new Random().nextInt(config.getInt("game-config.teleport-to-base-item-drop-chance")) == 0) {
                     ItemStack teleportToBaseItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.teleport-to-base.material")));
                     ItemMeta teleportToBaseMeta = teleportToBaseItem.getItemMeta();
                     teleportToBaseMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.teleport-to-base.name")));
                     teleportToBaseItem.setItemMeta(teleportToBaseMeta);
                     if (!player.getInventory().contains(teleportToBaseItem.getType()) && arena.getState() == ArenaState.STARTED) {
                        player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.teleport-to-base.slot") - 1, teleportToBaseItem);
                        Utils.sendMessage(player, config.getString("messages.teleport-to-base-item-drop-message"));
                        Sounds.ORB_PICKUP.play(player);
                     }
                  }
               }
            }
         }

         if (gameTime % config.getInt("game-config.arrow-drop-period") == 0) {
            arena.getPlayers().forEach(player -> {
               if (!player.getInventory().contains(Material.ARROW) && !deadPlayers.contains(player))
                  player.getInventory().setItem(8, new ItemStack(Material.ARROW));
               else {
                  int arrowCount = 0;
                  for (ItemStack itemStack : player.getInventory().getContents()) {
                     if (itemStack != null && itemStack.getType() == Material.ARROW) arrowCount += itemStack.getAmount();
                  }
                  if (arrowCount < config.getInt("game-config.arrow-max-per-player") && !deadPlayers.contains(player)) player.getInventory().addItem(new ItemStack(Material.ARROW));
               }
            });
         }

         gameTime--;
      }, 0L, 20L);
   }

   public void handlePlayerScore(Player scoredPlayer) {
      if (arena.getGameType() == GameType.SCORES) startNextRound(scoredPlayer);
      else if (scoredPlayer != null) {
         boolean finalGoal = false;
         TeamColor scoredTeamColor = arena.getTeamColor(scoredPlayer);
         TeamColor victimTeamColor = arena.getTeamColor(scoredPlayer.getLocation());
         if (scoredTeamColor != null) {
            if (teamLivesMap.get(victimTeamColor) > 0) {
               GamePlayer scoredPlayerData = currentRoundPlayersData.stream().filter(x -> x.getUniqueId().equals(scoredPlayer.getUniqueId()))
                       .findAny().orElse(new GamePlayer(scoredPlayer.getUniqueId()));
               scoredPlayerData.setGoals(scoredPlayerData.getGoals() + 1);
               teamLivesMap.computeIfPresent(scoredTeamColor, (tColor, lives) -> lives + 1);
               teamLivesMap.computeIfPresent(victimTeamColor, (tColor, lives) -> lives - 1);
               if (teamLivesMap.get(TeamColor.valueOf(scoredTeamColor.toString())) >= maxTeamLives) {
                  finalGoal = true;
               }
               if (teamLivesMap.get(victimTeamColor) <= 0) {
                  int deadTeams = 0;
                  for (Player player : arena.getPlayers()) {
                     Utils.sendMessage(player, config.getString("messages.team-eliminated-message." + victimTeamColor.toString().toLowerCase()));
                     if (arena.getTeamColor(player) == victimTeamColor) {
                        deadPlayers.add(player);
                        deadTeams++;

                        GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
                        GamePlayer currentRoundPlayer = currentRoundPlayersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId()))
                                .findAny().orElse(new GamePlayer(player.getUniqueId()));
                        gamePlayer.setGamesPlayed(gamePlayer.getGamesPlayed() + 1);
                        gamePlayer.setKills(gamePlayer.getKills() + currentRoundPlayer.getKills());
                        gamePlayer.setGoals(gamePlayer.getGoals() + currentRoundPlayer.getGoals());
                        Utils.resetPlayerAttributes(player);
                        player.setAllowFlight(true);
                        player.setFlying(true);

                        ItemStack newGameItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.new-game.material")));
                        ItemMeta newGameItemMeta = newGameItem.getItemMeta();
                        newGameItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.new-game.name")));
                        newGameItem.setItemMeta(newGameItemMeta);
                        player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.new-game.slot") - 1, newGameItem);

                        ItemStack leaveGameItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.leave-game.material")));
                        ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
                        leaveGameItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.leave-game.name")));
                        leaveGameItem.setItemMeta(leaveGameItemMeta);
                        player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.leave-game.slot") - 1, leaveGameItem);

                        arena.getPlayers().forEach(x -> x.hidePlayer(player));
                        player.sendTitle(Utils.colorize(config.getString("messages.defeat-title")), "");
                     }
                  }
                  finalGoal = deadTeams == getTeamLivesMap().size() - 1;
               }
               for (Player player : arena.getPlayers()) {
                  player.sendTitle(Utils.colorize(config.getString("messages.next-round-start.scored-player-to-team." + victimTeamColor.toString().toLowerCase())
                          .replace("%player_name%", Utils.getColorCode(scoredTeamColor) + scoredPlayer.getDisplayName())
                  ), "");
                  Sounds.ORB_PICKUP.play(player);
                  if (arena.getTeamColor(player) == scoredTeamColor) launchFireWork(player);
               }
               launchFireWork(scoredPlayer);
            }
            if (teamLivesMap.size() > 2) {
               scoredPlayer.setFallDistance(0.0f);
               scoredPlayer.teleport(arena.getTeamRespawnLocationMap().get(arena.getTeamColor(scoredPlayer)), PlayerTeleportEvent.TeleportCause.PLUGIN);
               scoredPlayer.setHealth(scoredPlayer.getMaxHealth());
            } else {
               for (Player player : arena.getPlayers()) {
                  player.setFallDistance(0.0f);
                  player.teleport(arena.getTeamRespawnLocationMap().get(arena.getTeamColor(player)), PlayerTeleportEvent.TeleportCause.PLUGIN);
                  player.setHealth(player.getMaxHealth());
               }
            }
            giveKit(scoredPlayer);
         }
         if (finalGoal) endGame(scoredTeamColor);
      }
   }

   public void startNextRound(Player scoredPlayer) {
      boolean finalGoal = false;
      TeamColor scoredTeamColor = null;
      if (scoredPlayer != null) {
         scoredTeamColor = arena.getTeamColor(scoredPlayer);
         teamScoresMap.put(scoredTeamColor, teamScoresMap.get(scoredTeamColor) + 1);
         GamePlayer currentRoundPlayer = currentRoundPlayersData.stream().filter(x -> x.getUniqueId().equals(scoredPlayer.getUniqueId()))
                 .findAny().orElse(new GamePlayer(scoredPlayer.getUniqueId()));
         currentRoundPlayer.setGoals(currentRoundPlayer.getGoals() + 1);
         if (teamScoresMap.get(scoredTeamColor) >= maxRounds) {
            finalGoal = true;
         }
      }

      for (Player player : arena.getPlayers()) {
         giveKit(player);
         player.teleport(arena.getTeamSpawnLocationMap().get(arena.getTeamColor(player)), PlayerTeleportEvent.TeleportCause.PLUGIN);
         playerLockedPositionMap.put(player, player.getLocation());
         if (arena.getTeamColor(player) == scoredTeamColor) {
            launchFireWork(player);
         }
      }

      if (finalGoal) {
         endGame(scoredTeamColor);
         return;
      }

      if (scoredTeamColor != null) round++;

      nextRoundCountDown = config.getInt("game-config.next-round-start-countdown");
      nextRoundStartCountDownTask = Bukkit.getScheduler().runTaskTimer(TheBridge.INSTANCE, () -> {
         for (Player player : arena.getPlayers()) {
            player.sendTitle(
                    scoredPlayer == null ? "" : Utils.colorize(config.getString("messages.next-round-start.scored-player").replace("%player_name%", Utils.getColorCode(arena.getTeamColor(scoredPlayer)) + scoredPlayer.getDisplayName())),
                    Utils.colorize(config.getString("messages.next-round-start.countdown").replace("%time%", String.valueOf(nextRoundCountDown)))
            );
            Sounds.ORB_PICKUP.play(player);
         }
         if (nextRoundCountDown <= 0) {
            playerLockedPositionMap.clear();
            for (Player player : arena.getPlayers()) {
               player.sendTitle(Utils.colorize(config.getString("messages.game-round-start-title")), "");
               player.setHealth(player.getMaxHealth());
            }
            nextRoundStartCountDownTask.cancel();
         }
         nextRoundCountDown--;
      }, 0L, 20L);
   }

   public void respawnPlayer(Player player, EntityDamageEvent.DamageCause damageCause) {
      Player lastDamageDealer = PlayerDamage.lastToDamage.get(player);
      if (damageCause == EntityDamageEvent.DamageCause.VOID) {
         if (lastDamageDealer == null) {
            arena.getPlayers().forEach(x -> Utils.sendMessage(x, config.getString("messages.death-messages.player-fell-into-void")
                    .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getDisplayName())));
         } else {
            arena.getPlayers().forEach(x -> Utils.sendMessage(x, config.getString("messages.death-messages.player-kicked-into-void-by-player")
                    .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getDisplayName())
                    .replace("%killer_name%", Utils.getColorCode(arena.getTeamColor(lastDamageDealer)) + lastDamageDealer.getDisplayName())
            ));
            giveRewardForKill(lastDamageDealer);
         }
      } else if (damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && lastDamageDealer != null) {
         arena.getPlayers().forEach(x -> Utils.sendMessage(x, config.getString("messages.death-messages.player-killed-by-player")
                 .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getDisplayName())
                 .replace("%killer_name%", Utils.getColorCode(arena.getTeamColor(lastDamageDealer)) + lastDamageDealer.getDisplayName())
         ));
         giveRewardForKill(lastDamageDealer);
      } else if (damageCause == EntityDamageEvent.DamageCause.FALL) {
         if (lastDamageDealer != null) {
            arena.getPlayers().forEach(x -> Utils.sendMessage(x, config.getString("messages.death-messages.player-kicked-to-ground-by-player")
                    .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getDisplayName())
                    .replace("%killer_name%", Utils.getColorCode(arena.getTeamColor(lastDamageDealer)) + lastDamageDealer.getDisplayName())
            ));
            giveRewardForKill(lastDamageDealer);
         } else {
            arena.getPlayers().forEach(x -> Utils.sendMessage(x, config.getString("messages.death-messages.player-hit-ground")
                    .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getDisplayName())
            ));
         }
      }
      PlayerDamage.lastToDamage.remove(player);
      player.setFallDistance(0.0f);
      Utils.removePotionEffects(player);
      player.setVelocity(player.getVelocity().multiply(0));
      player.teleport(arena.getTeamRespawnLocationMap().get(arena.getTeamColor(player)), PlayerTeleportEvent.TeleportCause.PLUGIN);
      player.setHealth(20.0);
   }

   public void endGame(TeamColor winnerTeamColor) {
      this.winnerTeamColor = winnerTeamColor;

      arena.setState(ArenaState.ENDING);
      if (gameTimerTask != null) gameTimerTask.cancel();

      ItemStack newGameItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.new-game.material")));
      ItemMeta newGameItemMeta = newGameItem.getItemMeta();
      newGameItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.new-game.name")));
      newGameItem.setItemMeta(newGameItemMeta);

      ItemStack leaveGameItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.leave-game.material")));
      ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
      leaveGameItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.leave-game.name")));
      leaveGameItem.setItemMeta(leaveGameItemMeta);

      for (Player player : arena.getPlayers()) {
         Bukkit.getOnlinePlayers().forEach(x -> x.showPlayer(player));
         PlayerDamage.lastToDamage.remove(player);
         GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
         GamePlayer currentRoundPlayer = currentRoundPlayersData.stream().filter(x -> x.getUniqueId().equals(player.getUniqueId()))
                 .findAny().orElse(new GamePlayer(player.getUniqueId()));
         if (!deadPlayers.contains(player)) gamePlayer.setGamesPlayed(gamePlayer.getGamesPlayed() + 1);
         gamePlayer.setKills(gamePlayer.getKills() + currentRoundPlayer.getKills());
         gamePlayer.setGoals(gamePlayer.getGoals() + currentRoundPlayer.getGoals());
         Utils.resetPlayerAttributes(player);
         player.setAllowFlight(true);
         player.setFlying(true);
         Bukkit.getScheduler().scheduleSyncDelayedTask(TheBridge.INSTANCE, () -> {
            player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.new-game.slot") - 1, newGameItem);
            player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.leave-game.slot") - 1, leaveGameItem);
         }, 20L);

         List<GamePlayer> currentRoundPlayersDataSortedByGoals = null;

         int bonusReward = 0;
         String title = Utils.colorize(config.getString("messages.draw-title"));
         if (winnerTeamColor != null) {
            title = Utils.colorize(config.getString("messages.game-end-title.team-color." + winnerTeamColor.toString().toLowerCase()));
            if (arena.getTeamColor(player) == winnerTeamColor) {
               launchFireWork(player);
               if (!deadPlayers.contains(player)) gamePlayer.setGamesWon(gamePlayer.getGamesWon() + 1);
               bonusReward += config.getInt("game-config.bonus-rewards." + arena.getType() + ".victory");
            }

            bonusReward += currentRoundPlayer.getKills() / config.getInt("game-config.bonus-rewards." + arena.getType() + ".kills");
            if (config.getIntegerList("game-config.bonus-rewards." + arena.getType() + ".reward-for-place-by-goals") != null &&
                    !config.getIntegerList("game-config.bonus-rewards." + arena.getType() + ".reward-for-place-by-goals").isEmpty()) {
               if (currentRoundPlayersData.size() >= 3) {
                  int bonusRewardForPlaceByGoals = 0;
                  currentRoundPlayersDataSortedByGoals = currentRoundPlayersData.stream().sorted(Comparator.comparing(GamePlayer::getGoals)).collect(Collectors.toList());
                  Collections.reverse(currentRoundPlayersDataSortedByGoals);
                  for (int i = 0; i < 3; i++) if (player.getName().equals(currentRoundPlayersDataSortedByGoals.get(2 - i).getPlayerName()))
                     bonusRewardForPlaceByGoals = config.getIntegerList("game-config.bonus-rewards." + arena.getType() + ".reward-for-place-by-goals").get(i);
                  bonusReward += bonusRewardForPlaceByGoals;
               }
            }
            if (bonusReward > 0) {
               TheBridge.getEconomy().depositPlayer(player, bonusReward);
               Utils.sendMessage(player, config.getString("messages.game-end-bonus-reward-message").replace("%bonus%", String.valueOf(bonusReward)));
            }
         }

         if (currentRoundPlayersDataSortedByGoals != null && currentRoundPlayersDataSortedByGoals.size() >= 3) {
            int nicknameMaxLength = currentRoundPlayersDataSortedByGoals.get(0).getPlayerName().length();
            if (nicknameMaxLength < currentRoundPlayersDataSortedByGoals.get(1).getPlayerName().length())
               nicknameMaxLength = currentRoundPlayersDataSortedByGoals.get(1).getPlayerName().length();
            if (nicknameMaxLength < currentRoundPlayersDataSortedByGoals.get(2).getPlayerName().length())
               nicknameMaxLength = currentRoundPlayersDataSortedByGoals.get(2).getPlayerName().length();

            for (String line : config.getStringList("messages.game-end-goals-top")) {
               player.sendMessage(Utils.colorize(line
                       .replace("%top_1_player%", (Utils.getColorCode(arena.getTeamColor(
                               currentRoundPlayersDataSortedByGoals.get(0).getPlayerName())) + currentRoundPlayersDataSortedByGoals.get(0).getPlayerName())
                       + new String(new char[nicknameMaxLength - currentRoundPlayersDataSortedByGoals.get(0).getPlayerName().length()]).replace("\0", " "))
                       .replace("%top_2_player%", (Utils.getColorCode(arena.getTeamColor(
                               currentRoundPlayersDataSortedByGoals.get(1).getPlayerName())) + currentRoundPlayersDataSortedByGoals.get(1).getPlayerName())
                       + new String(new char[nicknameMaxLength - currentRoundPlayersDataSortedByGoals.get(1).getPlayerName().length()]).replace("\0", " "))
                       .replace("%top_3_player%", (Utils.getColorCode(arena.getTeamColor(
                               currentRoundPlayersDataSortedByGoals.get(2).getPlayerName())) + currentRoundPlayersDataSortedByGoals.get(2).getPlayerName())
                       + new String(new char[nicknameMaxLength - currentRoundPlayersDataSortedByGoals.get(2).getPlayerName().length()]).replace("\0", " "))
                       .replace("%top_1_player_goals%", String.valueOf(currentRoundPlayersDataSortedByGoals.get(0).getGoals()))
                       .replace("%top_2_player_goals%", String.valueOf(currentRoundPlayersDataSortedByGoals.get(1).getGoals()))
                       .replace("%top_3_player_goals%", String.valueOf(currentRoundPlayersDataSortedByGoals.get(2).getGoals()))
               ));
            }
         }

         if (arena.getGameType() == GameType.SCORES) {
            player.sendTitle(title, Utils.colorize(config.getString("messages.game-end-title.scores-subtitle")
                    .replace("%red%", String.valueOf(teamScoresMap.get(TeamColor.RED))).replace("%blue%", String.valueOf(teamScoresMap.get(TeamColor.BLUE)))
            ));
         } else {
            player.sendTitle(title, Utils.colorize(Utils.getColorCode(winnerTeamColor) + new String(new char[teamLivesMap.get(winnerTeamColor)])
                    .replaceAll("\0", config.getString("messages.team-scores-format.lives"))
            ));
         }
      }
      deadPlayers.clear();
      playerLockedPositionMap.clear();

      if (isStarted) Bukkit.getScheduler().scheduleSyncDelayedTask(TheBridge.INSTANCE, () -> {
         isStarted = false;
         arena.restart();
      }, 8 * 20L);
   }

   public void destroy() {
      isStarted = false;
      if (startCountDownTask != null) startCountDownTask.cancel();
      if (gameTimerTask != null) gameTimerTask.cancel();
   }

   public void launchFireWork(Player player) {
      final Firework f = player.getWorld().spawn(player.getLocation(), Firework.class);
      FireworkMeta fm = f.getFireworkMeta();
      fm.addEffect(FireworkEffect.builder()
              .flicker(true)
              .trail(true)
              .with(FireworkEffect.Type.values()[Integer.max(0, new Random().nextInt(FireworkEffect.Type.values().length - 1))])
              .with(FireworkEffect.Type.values()[Integer.max(0, new Random().nextInt(FireworkEffect.Type.values().length - 1))])
              .with(FireworkEffect.Type.values()[Integer.max(0, new Random().nextInt(FireworkEffect.Type.values().length - 1))])
              .withColor(Color.fromRGB(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
              .withColor(Color.fromRGB(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
              .withColor(Color.fromRGB(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
              .withColor(Color.fromRGB(new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)))
              .build());
      fm.setPower(0);
      f.setFireworkMeta(fm);
      Bukkit.getScheduler().scheduleSyncDelayedTask(TheBridge.INSTANCE, () -> Sounds.FIREWORK_LARGE_BLAST.play(player), 25L);
   }

   private void giveRewardForKill(Player player) {
      GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
      GamePlayer currentRoundGamePlayer = currentRoundPlayersData.stream()
              .filter(x -> x.getUniqueId().equals(gamePlayer.getUniqueId())).findAny().orElse(new GamePlayer(gamePlayer.getUniqueId()));
      currentRoundGamePlayer.setKills(currentRoundGamePlayer.getKills() + 1);
      if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.time-back.name")))) {
         if (new Random().nextInt(config.getInt("game-config.time-back-item-drop-chance")) == 0) {
            ItemStack timeBackTime = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.time-back.material")));
            ItemMeta timeBackTimeMeta = timeBackTime.getItemMeta();
            timeBackTimeMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.time-back.name")));
            timeBackTime.setItemMeta(timeBackTimeMeta);
            if (!player.getInventory().contains(timeBackTime.getType())) {
               player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.time-back.slot") - 1, timeBackTime);
               Sounds.ORB_PICKUP.play(player);
               Utils.sendMessage(player, config.getString("messages.time-back-item-drop-message"));
            }
         }
      }
      if (new Random().nextInt(config.getInt("game-config.golden-apple-drop-chance")) == 0) {
         ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 1);
         int goldenAppleAmount = 0;
         for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == goldenApple.getType()) {
               goldenAppleAmount += itemStack.getAmount();
            }
         }
         if (goldenAppleAmount < config.getInt("game-config.golden-apple-max-amount")) {
            player.getInventory().addItem(goldenApple);
            Sounds.ORB_PICKUP.play(player);
            Utils.sendMessage(player, config.getString("messages.golden-apple-drop-message"));
         }
      }
   }

   private void giveKit(Player player) {
      GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());

      player.getInventory().clear();
      player.getInventory().setArmorContents(null);

      ItemStack sword = new ItemStack(Material.IRON_SWORD);
      ItemMeta swordMeta = sword.getItemMeta();
      swordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      swordMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
      swordMeta.spigot().setUnbreakable(true);
      sword.setItemMeta(swordMeta);
      player.getInventory().setItem(0, sword);

      ItemStack bow = new ItemStack(Material.BOW);
      ItemMeta bowMeta = bow.getItemMeta();
      bowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
      bowMeta.spigot().setUnbreakable(true);
      bow.setItemMeta(bowMeta);
      player.getInventory().setItem(1, bow);

      ItemStack pickaxe = new ItemStack(gamePlayer.getPickaxeType());
      ItemMeta pickaxeMeta = pickaxe.getItemMeta();
      pickaxeMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      pickaxeMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
      pickaxeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      int enchantmentLevel = 0;
      switch (pickaxe.getType()) {
         case WOOD_PICKAXE:
         case GOLD_PICKAXE:
            enchantmentLevel = 3; break;
         case STONE_PICKAXE:
         case IRON_PICKAXE:
            enchantmentLevel = 2; break;
         case DIAMOND_PICKAXE:
            enchantmentLevel = 1; break;
      }
      if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.fast-pickaxe.name")))) {
         switch (pickaxe.getType()) {
            case WOOD_PICKAXE:
               enchantmentLevel = 5; break;
            case STONE_PICKAXE:
            case DIAMOND_PICKAXE:
               enchantmentLevel = 5; break;
            case IRON_PICKAXE:
               enchantmentLevel = 4; break;
            case GOLD_PICKAXE:
               enchantmentLevel = 4; break;
         }
      }
      pickaxeMeta.addEnchant(Enchantment.DIG_SPEED, enchantmentLevel, false);
      pickaxeMeta.spigot().setUnbreakable(true);
      pickaxe.setItemMeta(pickaxeMeta);
      player.getInventory().setItem(2, pickaxe);

      ItemStack blocks = new ItemStack(Material.valueOf(config.getString("game-config.main-block-material")), 64);
      if (gamePlayer.getBlockColorType() == BlockColorType.RANDOM) {
         blocks.setDurability(Short.parseShort(gamePlayer.getAvailableBlockColors().get(new Random().nextInt(gamePlayer.getAvailableBlockColors().size()))));
      } else {
         switch (arena.getTeamColor(player)) {
            case RED:
               blocks.setDurability((short) 14);
               break;
            case BLUE:
               blocks.setDurability((short) 11);
               break;
            case GREEN:
               blocks.setDurability((short) 5);
               break;
            case YELLOW:
               blocks.setDurability((short) 4);
               break;
         }
      }
      player.getInventory().setItem(3, blocks);

      ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 2);
      player.getInventory().setItem(4, goldenApple);

      ItemStack arrow = new ItemStack(Material.ARROW, 3);
      player.getInventory().setItem(8, arrow);

      player.getInventory().setLeggings(createArmorItem(Material.LEATHER_LEGGINGS, arena.getTeamColor(player)));
      player.getInventory().setChestplate(createArmorItem(Material.LEATHER_CHESTPLATE, arena.getTeamColor(player)));

      if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.lightning-stick.name")))) {
         ItemStack lightningStick = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.lightning-stick.material")));
         ItemMeta lightningStickMeta = lightningStick.getItemMeta();
         lightningStickMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.lightning-stick.name")));
         lightningStickMeta.setLore(Utils.colorize(itemsConfig.getStringList("hot-bar-items.lightning-stick.lore")));
         lightningStick.setItemMeta(lightningStickMeta);
         player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.lightning-stick.slot") - 1, lightningStick);
      }

      if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.time-back.name")))) {
         ItemStack timeBackTime = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.time-back.material")));
         ItemMeta timeBackTimeMeta = timeBackTime.getItemMeta();
         timeBackTimeMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.time-back.name")));
         timeBackTime.setItemMeta(timeBackTimeMeta);
         player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.time-back.slot") - 1, timeBackTime);
      }

      if (gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.teleport-to-base.name")))) {
         ItemStack teleportToBaseItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.teleport-to-base.material")));
         ItemMeta teleportToBaseMeta = teleportToBaseItem.getItemMeta();
         teleportToBaseMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.teleport-to-base.name")));
         teleportToBaseItem.setItemMeta(teleportToBaseMeta);
         player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.teleport-to-base.slot") - 1, teleportToBaseItem);
      }
   }

   private ItemStack createArmorItem(Material material, TeamColor teamColor) {
      ItemStack itemStack = new ItemStack(material);
      LeatherArmorMeta itemMeta = (LeatherArmorMeta) itemStack.getItemMeta();
      itemMeta.spigot().setUnbreakable(true);
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
      switch (teamColor) {
         case RED: itemMeta.setColor(Color.RED); break;
         case BLUE: itemMeta.setColor(Color.BLUE); break;
         case GREEN: itemMeta.setColor(Color.GREEN); break;
         case YELLOW: itemMeta.setColor(Color.YELLOW); break;
      }
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   public boolean isStarted() {
      return isStarted;
   }

   public int getCountDown() {
      return countDown;
   }

   public int getGameTime() {
      return gameTime;
   }

   public TeamColor getWinnerTeamColor() {
      return winnerTeamColor;
   }

   public HashMap<TeamColor, Integer> getTeamScoresMap() {
      return teamScoresMap;
   }

   public HashMap<Player, Location> getPlayerLockedPositionMap() {
      return playerLockedPositionMap;
   }

   public List<GamePlayer> getCurrentRoundPlayersData() {
      return currentRoundPlayersData;
   }

   public int getRound() {
      return round;
   }

   public int getMaxTeamLives() {
      return maxTeamLives;
   }

   public HashMap<TeamColor, Integer> getTeamLivesMap() {
      return teamLivesMap;
   }

   public List<Player> getDeadPlayers() {
      return deadPlayers;
   }
}
