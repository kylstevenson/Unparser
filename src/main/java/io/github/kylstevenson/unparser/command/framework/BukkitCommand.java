package io.github.kylstevenson.unparser.command.framework;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

public class BukkitCommand extends org.bukkit.command.Command {

  private final Plugin plugin;
  private final CommandExecutor executor;
  protected BukkitCompleter completer;

  protected BukkitCommand(String label, CommandExecutor executor, Plugin plugin) {
    super(label);
    this.executor = executor;
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String commandLabel, String[] args) {
    boolean success;

    if (!plugin.isEnabled()) return false;

    if (!testPermission(sender)) return true;

    try {
      success = executor.onCommand(sender, this, commandLabel, args);
    } catch (Exception ex) {
      throw new CommandException(
          "Unhandled exception executing command '"
              + commandLabel
              + "' in plugin "
              + plugin.getDescription().getFullName(),
          ex);
    }

    if (!success && !usageMessage.isEmpty())
      Arrays.stream(usageMessage.replace("<command>", commandLabel).split("\n"))
          .forEach(sender::sendMessage);

    return success;
  }

  @Override
  public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args)
      throws CommandException, IllegalArgumentException {
    Validate.notNull(sender, "Sender cannot be null");
    Validate.notNull(args, "Arguments cannot be null");
    Validate.notNull(alias, "Alias cannot be null");

    List<String> completions = null;
    try {
      if (completer != null) completions = completer.onTabComplete(sender, this, alias, args);

      if (completions == null && executor instanceof TabCompleter)
        completions = ((TabCompleter) executor).onTabComplete(sender, this, alias, args);

    } catch (Exception ex) {
      StringBuilder message =
          new StringBuilder("Unhandled exception during tab completion for command '/")
              .append(alias)
              .append(' ');

      for (String arg : args) message.append(arg).append(' ');

      message
          .deleteCharAt(message.length() - 1)
          .append("' in plugin ")
          .append(plugin.getDescription().getFullName());
      throw new CommandException(message.toString(), ex);
    }

    if (completions == null) return super.tabComplete(sender, alias, args);

    return completions;
  }
}
