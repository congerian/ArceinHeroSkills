package ru.arcein.plugins.heroes.skills.webbarrier;

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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class SkillWebBarrier extends ActiveSkill {
    private static Set<Location> changedBlockLocations = new HashSet();

    private Map<Player, Long> players = new LinkedHashMap<Player, Long>(100) {

        public boolean removeEldestEntry(Map.Entry<Player, Long> eldest) {
            return (Long)eldest.getValue() + 5000L <= System.currentTimeMillis();
        }
    };

    public SkillWebBarrier(Heroes plugin) {
        super(plugin, "WebBarrier");
        this.setDescription("Вы окружаете себя барьером из паутины.");
        this.setUsage("/skill WebBarrier");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill webbarrier"});
        this.setTypes(new SkillType[]{SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.BLOCK_MODIFYING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillWebBarrier.SkillBlockListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set("radius", 5);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% окружает себя барьером из паутины!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не окружает себя барьером из паутины!");
        return node;
    }

    public void init() {
        super.init();
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% окружает себя барьером из паутины!").replace("%hero%", hero.getName());
        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не окружает себя барьером из паутины!").replace("%hero%", hero.getName());
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

        SkillWebBarrier.WebBarrierEffect effect = new SkillWebBarrier.WebBarrierEffect(this, player, duration, radius, blocks, applyText, expireText);
        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }


    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public class WebBarrierEffect extends ExpirableEffect {
        private Set<Location> locations = new HashSet<>();
        private Set<Vector> blocks = new HashSet<>();
        Player player;
        int radius;

        public WebBarrierEffect(Skill skill, Player player, long duration, int radius, Set<Vector> blocks, String applyText, String expireText) {
            super(skill, "WebBarrier", player, duration, applyText, expireText);
            this.player = player;
            this.radius = radius;
            this.blocks = blocks;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            updateBlocks();
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            for(Location location : locations) {
                Block block = location.getBlock();
                block.setType(Material.AIR);
                SkillWebBarrier.changedBlockLocations.remove(block);
            }
            locations.clear();
        }

        public void updateBlocks(){
            Location center = player.getLocation().getBlock().getLocation().clone().add(0.0, 1.5, 0.0);

            for(Location location : locations) {
                Block block = location.getBlock();
                block.setType(Material.AIR);
                SkillWebBarrier.changedBlockLocations.remove(block);
            }
            locations.clear();

            for(Vector vector : blocks){
                Location location = center.clone().add(vector).getBlock().getLocation();
                Block block = location.getBlock();
                if(block.getType() == Material.AIR){
                    SkillWebBarrier.changedBlockLocations.add(location);
                    this.locations.add(location);
                    location.getBlock().setType(Material.WEB);
                }
            }
        }
    }

    public class SkillBlockListener implements Listener {
        SkillWebBarrier skill;

        public SkillBlockListener(SkillWebBarrier skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST
        )
        public void onBlockBreak(BlockBreakEvent event) {
            if (!event.isCancelled()) {
                if (SkillWebBarrier.changedBlockLocations.contains(event.getBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onPlayerMove(PlayerMoveEvent event) {
            if (!event.isCancelled()) {
                Player player = event.getPlayer();
                Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);
                if (hero.hasEffect("WebBarrier")) {
                    if(skill.players.containsKey(player)){
                        if(System.currentTimeMillis() - skill.players.get(player) < 200L) return;
                    }
                    skill.players.put(player, System.currentTimeMillis());
                    ((WebBarrierEffect)hero.getEffect("WebBarrier")).updateBlocks();
                }
            }
        }
    }
}
