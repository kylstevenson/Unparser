package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.GameType;
import io.github.kylstevenson.unparser.model.ParsedMapData;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.bukkit.World;

public class MapMetadataWriter {

  public boolean write(ParsedMapData map, World world, GameType gameType, String admin) {
    try (BufferedWriter out =
        new BufferedWriter(
            new FileWriter(world.getWorldFolder().getPath() + File.separator + "Map.dat"))) {
      out.write("MAP_NAME:" + map.getName());
      out.newLine();
      out.write("MAP_AUTHOR:" + map.getAuthor());
      out.newLine();
      out.write("GAME_TYPE:" + gameType.name());
      out.newLine();
      out.write("ADMIN_LIST:" + admin + ",");
      out.newLine();
      out.write("currentlyLive:false");
      out.newLine();
      out.write("warps:");
      out.newLine();
      out.write("LOCKED:false");
      return true;
    } catch (Exception exception) {
      exception.printStackTrace();
      return false;
    }
  }
}
