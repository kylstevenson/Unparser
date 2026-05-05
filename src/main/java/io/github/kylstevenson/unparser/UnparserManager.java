package io.github.kylstevenson.unparser;

import io.github.kylstevenson.unparser.command.UnparseCommand;
import io.github.kylstevenson.unparser.command.framework.CommandManager;
import io.github.kylstevenson.unparser.listener.UnparseWorldGuardListener;
import io.github.kylstevenson.unparser.model.GameType;
import io.github.kylstevenson.unparser.service.AutoTypeResolution;
import io.github.kylstevenson.unparser.service.GameTypeParser;
import io.github.kylstevenson.unparser.service.GameTypeResolver;
import io.github.kylstevenson.unparser.service.SourceArchiveResolver;
import io.github.kylstevenson.unparser.service.UnparseResult;
import io.github.kylstevenson.unparser.service.UnparseService;
import io.github.kylstevenson.unparser.service.UnresolvedPolicy;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UnparserManager {

  private final Plugin plugin;
  private final UnparseService unparseService;
  private final SourceArchiveResolver sourceArchiveResolver;
  private final GameTypeParser gameTypeParser;
  private final boolean showPlayerFailureReasons;
  private final GameTypeResolver gameTypeResolver;
  private final UnresolvedPolicy unresolvedPolicy;
  @Getter private volatile boolean running;
  @Getter private volatile String currentItem;
  @Getter private volatile String lastSummary = "No runs yet.";

  public UnparserManager(Plugin plugin, CommandManager commandManager) {
    this.plugin = plugin;
    String mapsFolder = plugin.getConfig().getString("maps-folder", "map");
    String sourceSafeRoot = plugin.getConfig().getString("source-safe-root", "");
    boolean logMetrics = plugin.getConfig().getBoolean("log-metrics", true);
    this.showPlayerFailureReasons =
        plugin.getConfig().getBoolean("show-player-failure-reasons", false);
    this.sourceArchiveResolver = new SourceArchiveResolver(sourceSafeRoot);
    this.gameTypeParser = new GameTypeParser();
    this.unresolvedPolicy =
        UnresolvedPolicy.fromConfig(plugin.getConfig().getString("unresolved-policy", "skip"));
    this.gameTypeResolver = new GameTypeResolver(gameTypeParser.readAliases(plugin.getConfig()));
    String lockedMessage =
        plugin
            .getConfig()
            .getString("messages.world-locked", "&cThis world is currently being processed.");

    this.unparseService = new UnparseService(mapsFolder, logMetrics);
    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(
            new UnparseWorldGuardListener(unparseService, "&9Unparse> " + lockedMessage), plugin);
    commandManager.registerCommands(new UnparseCommand(this));
  }

  public UnparseResult unparseMap(File zipFile, GameType gameType, String admin, boolean dryRun) {
    return unparseService.unparseMap(zipFile, gameType, admin, dryRun);
  }

  public List<String> processBatch(
      List<String> sources, String typeArg, String admin, boolean dryRun, CommandSender sender) {
    if (running) {
      return Collections.singletonList("&9Unparse> &cAn unparse batch is already running.");
    }

    running = true;
    currentItem = null;

    GameType forced = "auto".equalsIgnoreCase(typeArg) ? null : gameTypeParser.parse(typeArg);
    if (forced == null && !"auto".equalsIgnoreCase(typeArg)) {
      running = false;
      return Collections.singletonList("&9Unparse> &cUnknown GameType: " + typeArg);
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              List<File> zipFiles = sourceArchiveResolver.resolveZipFiles(sources);
              List<BatchItem> items = new ArrayList<>();
              int preSkipped = 0;
              final String[] failEarly = new String[1];

              for (File zip : zipFiles) {
                GameType type = forced;
                boolean skipItem = false;

                if (type == null) {
                  AutoTypeResolution resolution = gameTypeResolver.resolve(zip, true);
                  type = resolution.getGameType();

                  if (type == null) {
                    if (unresolvedPolicy == UnresolvedPolicy.FAIL) {
                      failEarly[0] = zip.getName() + " -> unresolved type";
                      Bukkit.getScheduler()
                          .runTask(
                              plugin,
                              () -> {
                                sendActionBar(sender, "Batch failed");
                                send(
                                    sender,
                                    Collections.singletonList(
                                        "&9Unparse> &cBatch failed: &e" + failEarly[0]));
                                finishRun(0, 1, 0, 0);
                              });
                      return;
                    }
                    if (unresolvedPolicy == UnresolvedPolicy.FALLBACK) {
                      type = GameType.Other;
                    } else {
                      preSkipped++;
                      skipItem = true;
                    }
                  }
                }

                if (!skipItem) {
                  items.add(new BatchItem(zip, type));
                }
              }

              final int skippedCount = preSkipped;

              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        send(
                            sender,
                            Collections.singletonList(
                                "&9Unparse> &7Discovered &e" + zipFiles.size() + "&7 zip files."));
                        runBatchOnMainThread(items, sender, admin, dryRun, skippedCount);
                      });
            });

    return Collections.singletonList("&9Unparse> &7Started batch in background.");
  }

  private void runBatchOnMainThread(
      List<BatchItem> items, CommandSender sender, String admin, boolean dryRun, int preSkipped) {
    final long startedAt = System.nanoTime();

    new BukkitRunnable() {
      int index = 0;
      int success = 0;
      int fail = 0;
      final List<String> failedDetails = new ArrayList<>();

      @Override
      public void run() {
        if (index >= items.size()) {
          long totalMs = (System.nanoTime() - startedAt) / 1_000_000L;
          int skip = preSkipped;
          sendActionBar(sender, "Unparse complete");
          send(
              sender,
              Collections.singletonList(
                  "&9Unparse> &7Summary: &a"
                      + success
                      + " ok &c"
                      + fail
                      + " fail &e"
                      + skip
                      + " skip &7in &e"
                      + totalMs
                      + "ms"));
          if (!failedDetails.isEmpty()) {
            plugin.getLogger().warning("[Unparse] Failed zip files:");
            for (String detail : failedDetails) {
              plugin
                  .getLogger()
                  .warning(
                      ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', detail)));
            }

            if (showPlayerFailureReasons) {
              send(sender, Collections.singletonList("&9Unparse> &cFailed zip files:"));
              send(sender, failedDetails);
            } else {
              send(
                  sender,
                  Collections.singletonList(
                      "&9Unparse> &c"
                          + failedDetails.size()
                          + " map(s) failed. Reasons logged to console."));
            }
          }
          finishRun(success, fail, skip, totalMs);
          cancel();
          return;
        }

        BatchItem item = items.get(index++);
        currentItem = item.file.getName();
        sendActionBar(
            sender,
            "Processing "
                + index
                + "/"
                + items.size()
                + ": "
                + item.file.getName()
                + (dryRun ? " (dryrun)" : ""));

        UnparseResult result = unparseService.unparseMap(item.file, item.type, admin, dryRun);
        if (result.isSuccess()) {
          success++;
        } else {
          fail++;
          failedDetails.add(
              "&9Unparse> &e- " + item.file.getName() + " &7-> &c" + result.getMessage());
        }
      }
    }.runTaskTimer(plugin, 1L, 1L);
  }

  private void finishRun(int success, int fail, int skip, long totalMs) {
    lastSummary =
        "Last run: success="
            + success
            + ", fail="
            + fail
            + ", skip="
            + skip
            + ", total="
            + totalMs
            + "ms";
    running = false;
    currentItem = null;
  }

  private void send(CommandSender sender, List<String> lines) {
    for (String line : lines) {
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
    }
  }

  private void sendActionBar(CommandSender sender, String message) {
    if (!(sender instanceof Player)) {
      return;
    }

    Player player = (Player) sender;
    String text = ChatColor.translateAlternateColorCodes('&', "&9Unparse> &7" + message);

    try {
      Class<?> craftPlayerClass = player.getClass();
      Object craftPlayerHandle = craftPlayerClass.getMethod("getHandle").invoke(player);
      Object playerConnection =
          craftPlayerHandle.getClass().getField("playerConnection").get(craftPlayerHandle);

      Class<?> iChatBaseComponentClass =
          Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent");
      Class<?> chatSerializerClass =
          Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer");
      Object chatComponent =
          chatSerializerClass
              .getMethod("a", String.class)
              .invoke(null, "{\"text\":\"" + escapeJson(text) + "\"}");

      Class<?> packetPlayOutChatClass =
          Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat");
      Object packet =
          packetPlayOutChatClass
              .getConstructor(iChatBaseComponentClass, byte.class)
              .newInstance(chatComponent, (byte) 2);

      Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.Packet");
      playerConnection
          .getClass()
          .getMethod("sendPacket", packetClass)
          .invoke(playerConnection, packet);
    } catch (Exception ignored) {
      player.sendMessage(text);
    }
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static class BatchItem {
    private final File file;
    private final GameType type;

    private BatchItem(File file, GameType type) {
      this.file = file;
      this.type = type;
    }
  }

  public String getStatusLine() {
    if (running) {
      return "&9Unparse> "
          + "&7Status: &eRUNNING &7(current=&e"
          + currentItem
          + "&7, locked="
          + unparseService.getLockedWorldNames().size()
          + ")";
    }
    return "&9Unparse> &7Status: &aIDLE &7- " + lastSummary;
  }
}
