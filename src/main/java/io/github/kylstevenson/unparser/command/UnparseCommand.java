package io.github.kylstevenson.unparser.command;

import io.github.kylstevenson.unparser.UnparserManager;
import io.github.kylstevenson.unparser.command.framework.CommandArgs;
import io.github.kylstevenson.unparser.command.framework.annotation.Command;
import io.github.kylstevenson.unparser.command.framework.annotation.Completer;
import io.github.kylstevenson.unparser.model.GameType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;

@RequiredArgsConstructor
public class UnparseCommand {

  private final UnparserManager unparserManager;

  @Command(
      name = "unparse",
      aliases = {"unmap"},
      description = "Unparse maps: run, dryrun, status, help",
      permission = "command.unparse",
      inGameOnly = true)
  public void root(CommandArgs args) {
    sendHelp(args);
  }

  @Command(
      name = "unparse.help",
      aliases = {"unmap.help"},
      permission = "command.unparse",
      inGameOnly = true)
  public void help(CommandArgs args) {
    sendHelp(args);
  }

  @Command(
      name = "unparse.status",
      aliases = {"unmap.status"},
      permission = "command.unparse",
      inGameOnly = true)
  public void status(CommandArgs args) {
    args.getSender().sendMessage(color(unparserManager.getStatusLine()));
  }

  @Command(
      name = "unparse.run",
      aliases = {"unmap.run"},
      permission = "command.unparse",
      inGameOnly = true)
  public void run(CommandArgs args) {
    executeBatch(args, false);
  }

  @Command(
      name = "unparse.dryrun",
      aliases = {"unmap.dryrun"},
      permission = "command.unparse",
      inGameOnly = true)
  public void dryRun(CommandArgs args) {
    executeBatch(args, true);
  }

  private void executeBatch(CommandArgs args, boolean dryRun) {
    String commandLabel = resolveCommandLabel(args);

    if (args.length() < 2) {
      sendHelp(args);
      return;
    }

    String typeArg = args.getArgs(0);
    List<String> sources =
        parseSources(Arrays.copyOfRange(args.getArgs(), 1, args.getArgs().length));

    unparserManager
        .processBatch(sources, typeArg, args.getPlayer().getName(), dryRun, args.getSender())
        .forEach(message -> args.getSender().sendMessage(color(message)));
  }

  @Completer(
      name = "unparse",
      aliases = {"unmap"})
  public List<String> rootCompleter(CommandArgs args) {
    List<String> list = new ArrayList<>();

    if (args.length() == 1) {
      Arrays.asList("run", "dryrun", "status", "help").stream()
          .filter(name -> name.toLowerCase().startsWith(args.getArgs(0).toLowerCase()))
          .forEach(list::add);
      return list;
    }

    return list;
  }

  @Completer(
      name = "unparse.run",
      aliases = {"unmap.run"})
  public List<String> runCompleter(CommandArgs args) {
    return gameTypeCompletions(args);
  }

  @Completer(
      name = "unparse.dryrun",
      aliases = {"unmap.dryrun"})
  public List<String> dryRunCompleter(CommandArgs args) {
    return gameTypeCompletions(args);
  }

  private List<String> gameTypeCompletions(CommandArgs args) {
    List<String> list = new ArrayList<>();
    if (args.length() == 1) {
      list.add("auto");
      Arrays.stream(GameType.values())
          .map(Enum::name)
          .filter(name -> name.toLowerCase().startsWith(args.getArgs(0).toLowerCase()))
          .sorted(String::compareToIgnoreCase)
          .forEach(list::add);
    }
    return list;
  }

  private void sendHelp(CommandArgs args) {
    String commandLabel = resolveCommandLabel(args);
    args.getSender().sendMessage(color("&9Unparse> &7Usage:"));
    args.getSender()
        .sendMessage(color("&9Unparse> &e" + commandLabel + " run <gametype|auto> <source...>"));
    args.getSender()
        .sendMessage(color("&9Unparse> &e" + commandLabel + " dryrun <gametype|auto> <source...>"));
    args.getSender().sendMessage(color("&9Unparse> &e" + commandLabel + " status"));
    args.getSender().sendMessage(color("&9Unparse> &e" + commandLabel + " help"));
  }

  private String resolveCommandLabel(CommandArgs args) {
    String label = args.getLabel() == null ? "unparse" : args.getLabel();
    return "/" + label.split("\\.")[0];
  }

  private String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  private List<String> parseSources(String[] raw) {
    List<String> sources = new ArrayList<>();
    StringBuilder current = null;

    for (String token : raw) {
      if (current == null) {
        if (startsQuoted(token) && !endsQuoted(token)) {
          current = new StringBuilder(token);
        } else {
          sources.add(stripMatchingQuotes(token));
        }
      } else {
        current.append(" ").append(token);
        if (endsQuoted(token)) {
          sources.add(stripMatchingQuotes(current.toString()));
          current = null;
        }
      }
    }

    if (current != null) {
      sources.add(stripMatchingQuotes(current.toString()));
    }

    return sources;
  }

  private boolean startsQuoted(String value) {
    return value.startsWith("\"") || value.startsWith("'");
  }

  private boolean endsQuoted(String value) {
    return value.endsWith("\"") || value.endsWith("'");
  }

  private String stripMatchingQuotes(String value) {
    String trimmed = value.trim();
    if (trimmed.length() >= 2) {
      char first = trimmed.charAt(0);
      char last = trimmed.charAt(trimmed.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return trimmed.substring(1, trimmed.length() - 1).trim();
      }
    }
    return trimmed;
  }
}
