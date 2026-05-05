package io.github.kylstevenson.unparser;

import io.github.kylstevenson.unparser.command.framework.CommandManager;
import io.github.kylstevenson.unparser.world.VoidWorldGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class UnparserPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    saveDefaultConfig();
    CommandManager commandManager = new CommandManager(this);
    new UnparserManager(this, commandManager);
    commandManager.registerHelp();
  }

  @Override
  public void onDisable() {}

  @Override
  public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    return new VoidWorldGenerator();
  }
}
