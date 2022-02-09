package ru.arcein.plugins.heroes.skills.glassbarrier;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

public class SkillGlassBarrier extends ActiveSkill {
    private static Set<Location> changedBlockLocations = new HashSet();

    public SkillGlassBarrier(Heroes plugin) {
        super(plugin, "GlassBarrier");
        this.setDescription("Вы окружаете себя барьером из стекла.");
        this.setUsage("/skill GlassBarrier");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill GlassBarrier"});
        this.setTypes(new SkillType[]{SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.BLOCK_MODIFYING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillGlassBarrierListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("radius", 5);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% окружает себя барьером из стекла!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не окружает себя барьером из стекла!");
        return node;
    }

    public void init() {
        super.init();
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% окружает себя барьером из стекла!").replace("%hero%", hero.getName());
        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не окружает себя барьером из стекла!").replace("%hero%", hero.getName());
        int radius = SkillConfigManager.getUseSetting(hero, this, "radius", 0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);

        Set<Vector> blocks = new HashSet<>();
        double minR = (radius-0.5)*(radius-0.5);
        double maxR = (radius+0.5)*(radius+0.5);
        for(int dx = -radius; dx <= radius; ++dx){
            int dx2 = dx*dx;
            for(int dy = -radius; dy <= radius; ++dy){
                int dy2 = dy*dy;
                for(int dz = -radius; dz <= radius; ++dz){
                    int dz2 = dz*dz;
                    if(dx2 + dy2 + dz2 < minR) continue;
                    if(dx2 + dy2 + dz2 > maxR) continue;
                    blocks.add(new Vector(dx, dy, dz));
                }
            }
        }

        SkillGlassBarrierEffect effect = new SkillGlassBarrierEffect(this, player, duration, radius, blocks, applyText, expireText);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }


    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public class SkillGlassBarrierEffect extends ExpirableEffect {
        private Set<Location> locations = new HashSet<>();
        private Set<Vector> blocks = new HashSet<>();
        private Player player;
        private int radius;

        public SkillGlassBarrierEffect(Skill skill, Player player, long duration, int radius, Set<Vector> blocks, String applyText, String expireText) {
            super(skill, "GlassBarrier", player, duration, applyText, expireText);
            this.player = player;
            this.radius = radius;
            this.blocks = blocks;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Location center = player.getLocation().getBlock().getLocation().clone().add(0.0, 1.5, 0.0);

            for(Vector vector : blocks){
                Location location = center.clone().add(vector).getBlock().getLocation();
                Block block = location.getBlock();
                if(block.getType() == Material.AIR){
                    SkillGlassBarrier.changedBlockLocations.add(location);
                    this.locations.add(location);
                    block.setType(Material.GLASS);
                }
            }
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            for(Location location : locations) {
                Block block = location.getBlock();
                block.setType(Material.AIR);
                SkillGlassBarrier.changedBlockLocations.remove(block);
            }
            locations.clear();
        }
    }

    public class SkillGlassBarrierListener implements Listener {
        SkillGlassBarrier skill;

        public SkillGlassBarrierListener(SkillGlassBarrier skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST
        )
        public void onBlockBreak(BlockBreakEvent event) {
            if (!event.isCancelled()) {
                if (SkillGlassBarrier.changedBlockLocations.contains(event.getBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
