package net.world.manager;

import cn.nukkit.plugin.PluginBase;

public class WorldManager extends PluginBase {

    @Override
    public void onEnable(){
        this.getServer().getCommandMap().register("wm", new WorldManagerCommand(this));
    }
}
