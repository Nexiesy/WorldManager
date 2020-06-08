package net.world.manager;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Level;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.Task;
import org.iq80.leveldb.util.FileUtils;

import java.io.File;
import java.util.Map;
import java.util.Random;

public class WorldManagerCommand extends PluginCommand {

    private WorldManager wm;

    public WorldManagerCommand(WorldManager wm) {
        super("wm", wm);
        this.setAliases(new String[]{"worldmanager"});
        this.setDescription("World manager command");
        this.setPermission("worldmanager.command");
        this.wm = wm;
    }


    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        return true;
    }

    public boolean execute(CommandSender o, String commandLabel, String[] args) {
        Player sender = (Player) o;
        if (!this.testPermission(o)) {
            return false;
        }
        if (args.length < 1) {
            sender.sendMessage("/wm help");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "help":
            case "h":
                helpMessage(sender);
                break;
            case "create":
                if (args.length < 4){
                    sender.sendMessage("/wm create <name>  <seed> <generator>");
                    return false;
                }
                Long seed = Long.parseLong("0");
                if(isInteger(args[2])){
                         seed = Long.parseLong(args[2]);
                }else {
                    seed = new Random().nextLong();
                }

                Class<? extends Generator> gen = Generator.getGenerator(args[3]);
                if (!wm.getServer().generateLevel(args[1], seed, gen)) {
                    sender.sendMessage("The world can't be created");
                    return true;
                }
                sender.sendMessage("World created");
                break;
            case "tp":
                if (args.length < 1) {
                    sender.sendMessage("Enter a world name");
                    return false;
                }
                Level w = wm.getServer().getLevelByName(args[1]);

                if (w == null) {
                    sender.sendMessage("World not found");
                    return false;
                }

            sender.teleport(w.getSpawnLocation());
                sender.sendMessage("Teleporting...");
                break;
            case "load":
                if (args.length < 1) {
                    sender.sendMessage("Enter a world name");
                    return false;
                }
                if (!wm.getServer().loadLevel(args[1])) {
                    sender.sendMessage("World not found");
                    return false;
                }


                sender.sendMessage("Loading specified world");
                break;
            case "list":
                sender.sendMessage("All the Worlds on your server:");
                Map<Integer, Level> level = wm.getServer().getLevels();
                level.forEach((i, l) -> {
                    sender.sendMessage("- " + l.getFolderName());
                });
                break;
            case "spawn":
                Level le = sender.getLevel();
                sender.teleport(le.getSpawnLocation());
                sender.sendMessage("You teleported to the spawn area of the world you are in");
                break;
            case "setspawn":
                Level lev = sender.getLevel();
                lev.setSpawnLocation(new Vector3(sender.getX(), sender.getY(), sender.getZ()));
                sender.sendMessage("The spawn field of the world you're in is set.");
                break;
            case "unload":
                if (args.length < 1) {
                    sender.sendMessage("Enter a world name");
                    return false;
                }
                Level sw = wm.getServer().getLevelByName(args[1]);
                if (sw == null) {
                    sender.sendMessage("World not found");
                    return false;
                }
                if (sw.getPlayers() != null) {
                    sw.getPlayers().forEach((lo, p) -> {
                        Level def = wm.getServer().getDefaultLevel();
                        p.teleport(def.getSpawnLocation());
                    });
                }
                wm.getServer().getScheduler().scheduleDelayedTask(new UnloadTask(sw, wm), 25);
                sender.sendMessage("The world was unload");
                break;
            case "delete":
                if (args.length < 1) {
                    sender.sendMessage("Enter a world name");
                    return false;
                }
                Level pw = wm.getServer().getLevelByName(args[1]);
                if (pw == null) {
                    sender.sendMessage("World not found");
                    return false;
                }
                if (pw.getPlayers() != null) {
                    pw.getPlayers().forEach((lo, p) -> {
                        Level def = wm.getServer().getDefaultLevel();
                        p.teleport(def.getSpawnLocation());
                    });
                }
                wm.getServer().getScheduler().scheduleDelayedTask(new DeleteTask(pw, wm), 25);
                sender.sendMessage("World has been removed");
                break;
            case "default":
                if (args.length < 1) {
                    sender.sendMessage("Enter a world name");
                    return false;
                }
                Level dw = wm.getServer().getLevelByName(args[1]);
                if (dw == null) {
                    sender.sendMessage("World not found");
                    return false;
                }
                wm.getServer().setDefaultLevel(dw);
                wm.getServer().setPropertyString("level-name", dw.getFolderName());

                sender.sendMessage("Default world has been set");
                break;
            case "info":
                sender.sendMessage("Default server world: "+wm.getServer().getDefaultLevel().getFolderName());
                wm.getServer().getLevels().forEach((i, al)->{
                    sender.sendMessage("- "+al.getFolderName() + " §b(Players: "+al.getPlayers().size()+")");
                });
                break;
        }
        return true;
    }

    public void helpMessage(Player sender) {
        sender.sendMessage("World manager commands list:");
        sender.sendMessage("/wm create <name>  <seed> <generator> > Create a new world");
        sender.sendMessage("/wm tp <name> > teleports you to world");
        sender.sendMessage("/wm load <name> > loads world you select");
        sender.sendMessage("/wm list > shows all worlds");
        sender.sendMessage("/wm spawn >  teleports you to world spawn you are in");
        sender.sendMessage("/wm setspawn > set the world spawn");
        sender.sendMessage("/wm unload <name> > unloads world you select");
        sender.sendMessage("/wm delete <name> > Deletes world you select");
        sender.sendMessage("/wm default <name> > Allows you to set the default server world");
        sender.sendMessage("/wm info > Gives information about worlds");
    }
}

class UnloadTask extends Task {

    private Level w;
    private WorldManager wm;

    public UnloadTask(Level w, WorldManager wm) {
        this.wm = wm;
        this.w = w;
    }

    @Override
    public void onRun(int i) {
        wm.getServer().unloadLevel(w);
    }
}

class DeleteTask extends Task {
    private Level w;
    private WorldManager wm;

    public DeleteTask(Level w, WorldManager wm) {
        this.wm = wm;
        this.w = w;
    }

    @Override
    public void onRun(int i) {
        File f = new File(wm.getServer().getDataPath()+"/worlds/"+w.getFolderName());
        wm.getServer().getLogger().info(w.getFolderName()+" removed");
        FileUtils.deleteDirectoryContents(f);
    }
}