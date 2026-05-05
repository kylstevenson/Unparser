package io.github.kylstevenson.unparser.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public enum GameType {

  // Other
  None("None"),
  Other("Other"),
  Unknown("Unknown"),

  // Non Games
  Lobby("Lobby"),
  Event("Mineplex Event"),
  GemHunters("Gem Hunters"),

  // Games
  BaconBrawl("Bacon Brawl", "Bacon_"),
  Barbarians("A Barbarians Life"),
  Bridge("The Bridges", "BR_"),
  Build("Master Builders"),
  BuildMavericks("Mavericks Master Builders"),
  CakeWars2("Cake Wars Duos"),
  CakeWars4("Cake Wars Standard", "CakeWars_"),
  CastleSiege("Castle Siege"),
  CastleAssault("Castle Assault"),
  CastleAssaultTDM("Castle Assault TDM"),
  ChampionsTDM("Champions TDM"),
  ChampionsDominate("Champions Domination"),
  ChampionsCTF("Champions CTF"),
  ChampionsMOBA("Champions MOBA"),
  Christmas("Christmas Chaos"),
  DeathTag("Death Tag"),
  DragonEscape("Dragon Escape"),
  DragonEscapeTeams("Dragon Escape Teams"),
  DragonRiders("Dragon Riders"),
  Dragons("Dragons", "DR_"),
  DragonsTeams("Dragons Teams"),
  Draw("Draw My Thing"),
  Evolution("Evolution"),
  FlappyBird("Flappy Bird"),
  Gladiators("Gladiators"),
  Gravity("Gravity"),
  Halloween("Halloween Horror"),
  Halloween2016("Halloween Horror 2016"),
  HideSeek("Block Hunt"),
  Horse("Horseback"),
  Lobbers("Bomb Lobbers"),
  SurvivalGames("Survival Games", "SG_"),
  SurvivalGamesTeams("Survival Games Teams", "SGT_"),
  Micro("Micro Battle"),
  MineStrike("MineStrike"),
  MineWare("MineWare"),
  MinecraftLeague("MCL"),
  MilkCow("Milk the Cow", "Milk_"),
  HOG("Heroes of GWEN"),
  MonsterLeague("MonsterLeague"),
  MonsterMaze("Monster Maze"),
  NanoGames("Nano Games"),
  Paintball("Super Paintball"),
  Quiver("One in the Quiver", "OITQ_"),
  QuiverTeams("One in the Quiver Teams"),
  Runner("Runner"),
  SearchAndDestroy("Search and Destroy"),
  Sheep("Sheep Quest"),
  Skyfall("Skyfall"),
  Skywars("Skywars"),
  Smash("Super Smash Mobs"),
  SmashTeams("Super Smash Mobs Teams"),
  SmashDomination("Super Smash Mobs Domination"),
  Snake("Snake"),
  SneakyAssassins("Sneaky Assassins"),
  SpeedBuilders("Speed Builders"),
  SnowFight("Snow Fight"),
  Spleef("Super Spleef"),
  SpleefTeams("Super Spleef Teams"),
  Stacker("Super Stacker"),
  SquidShooter("Squid Shooter", "Squid_"),
  Tug("Tug of Wool"),
  TurfWars("Turf Wars", "TF_"),
  UHC("Ultra Hardcore"),
  WitherAssault("Wither Assault"),
  Wizards("Wizards"),
  ZombieSurvival("Zombie Survival", "ZS_");

  private final String name;
  private final List<String> autoPrefixes;

  GameType(String name, String... autoPrefixes) {
    this.name = name;
    this.autoPrefixes = Collections.unmodifiableList(Arrays.asList(autoPrefixes));
  }
}
