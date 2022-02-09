package ru.arcein.plugins.heroes.skills.piggify;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;

public class SkillPiggify extends TargettedSkill {
    private final Map<Entity, CharacterTemplate> creatures = new HashMap();

    public SkillPiggify(Heroes plugin) {
        super(plugin, "Piggify");
        this.setDescription("You force your target to ride a pig for $1 seconds.");
        this.setUsage("/skill piggify <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill piggify"});
        this.setTypes(new SkillType[]{SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPiggify.SkillEntityListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        EntityType type = !target.getLocation().getBlock().getType().equals(Material.WATER) && !target.getLocation().getBlock().getType().equals(Material.STATIONARY_WATER) ? EntityType.PIG : EntityType.SQUID;
        Entity creature = target.getWorld().spawnEntity(target.getLocation(), type);
        int duration = (int)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new SkillPiggify.PigEffect(this, hero.getPlayer(), duration, (Creature)creature));

        ((Creature) creature).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 10));
        creature.setFireTicks(duration/50);

        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription().replace("$1", duration / 1000 + "");
    }

    public class SkillEntityListener implements Listener {
        public SkillEntityListener() {
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled()) return;
            if (SkillPiggify.this.creatures.containsKey(event.getEntity())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onPlayerDismount(VehicleExitEvent event) {
            Bukkit.getLogger().info("Checking event");
            if(SkillPiggify.this.creatures.containsKey(event.getVehicle())){
                Bukkit.getLogger().info("Trying to prevent it!!");
                event.setCancelled(true);
            }
        }
    }

    public class PigEffect extends ExpirableEffect {
        private final Creature creature;

        public PigEffect(Skill skill, Player applier, long duration, Creature creature) {
            super(skill, "Piggify", applier, duration);
            this.creature = creature;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.DISABLE);
        }

        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            this.creature.setPassenger(monster.getEntity());
            SkillPiggify.this.creatures.put(this.creature, monster);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            this.creature.setPassenger(player);
            SkillPiggify.this.creatures.put(this.creature, hero);
        }

        public void removeFromMonster(Monster rider) {
            super.removeFromMonster(rider);
            SkillPiggify.this.creatures.remove(this.creature);
            this.creature.remove();
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            SkillPiggify.this.creatures.remove(this.creature);
            this.creature.remove();
        }
    }
}

