# Unparser

Unparser is a Bukkit/Spigot 1.8.8 plugin. It takes map archive files made by Mineplex's MapParser and rebuilds them as normal, editable worlds for your Build Server.

## What It Does

- Takes one or more parsed map `.zip` files.
- Rebuilds them into regular worlds you can open and edit.

## Commands

- `/unparse run <gameType|auto> <source...>`: rebuilds maps from the source files.
- `/unparse dryrun <gameType|auto> <source...>`: shows what would be rebuilt, without making changes.
- `/unparse status`: shows whether a rebuild is currently running.

## Source Paths

- A source can be a `.zip` file or a folder.
- If you use a folder, Unparser checks that folder and all subfolders for `.zip` files.
- You can include multiple sources in one command.
- Put quotes around paths with spaces.

Example:

```text
/unparse run auto "worlds/Survival Games" "worlds/Dragon Escape"
```
