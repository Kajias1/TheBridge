package me.kajias.thebridge.configurations;

public class MenuConfiguration
{
   public static final BaseConfiguration baseConfig = new BaseConfiguration("menus");

   public static void initialize() {
      baseConfig.load();
   }
}
