package me.kajias.thebridge.configurations;

public class ScoreboardConfiguration
{
   public static final BaseConfiguration baseConfig = new BaseConfiguration("scoreboard");

   public static void initialize() {
      baseConfig.load();
   }
}
