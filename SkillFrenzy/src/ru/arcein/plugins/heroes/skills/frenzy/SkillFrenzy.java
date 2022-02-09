package ru.arcein.plugins.heroes.skills.frenzy;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import sun.plugin2.main.server.Plugin;

public class SkillFrenzy extends ActiveSkill {

    public SkillFrenzy(Heroes plugin) {
        super(plugin, "Frenzy");

        this.setDescription("Вы становитесь менее восприимчевы ко входящему урону, " +
                "после действия способности, вы восстанавливаете часть от нанесённого вам урона");

        this.setIdentifiers(new String[]{"skill frenzy"});

        this.setArgumentRange(0, 0);

        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.HEALING});

        Bukkit.getPluginManager().registerEvents(new SkillFrenzyListener(this), plugin);

    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero%" + ChatColor.RED.toString() + " впадает в безумие!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% восстанавливает рассудок!");

        node.set("absorb-percentage", 1.0D);
        node.set("heal-multiplier", 0.5D);

        return node;
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public SkillResult use(Hero hero, String[] args) {

        double absorbPercentage = SkillConfigManager.getUseSetting(hero, this, "absorb-percentage", 1.0D, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double healMultiplier = SkillConfigManager.getUseSetting(hero, this, "heal-multiplier", 0.5D, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero%" + ChatColor.RED.toString() + "впадает в безумие!")
                .replace("%hero%", hero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero,this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% восстанавливает рассудок!")
                .replace("%hero%", hero.getName());

        FrenzyEffect effect = new FrenzyEffect(
                this, hero.getPlayer(),
                duration, absorbPercentage, healMultiplier,
                applyText, expireText);

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    public class SkillFrenzyListener implements Listener {

        private SkillFrenzy skill;

        public SkillFrenzyListener(SkillFrenzy skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled()) return;

            Entity entity = event.getEntity();

            if (!(entity instanceof Player)) return;

            Hero hero = Heroes.getInstance().getCharacterManager().getHero((Player) entity);

            if(!hero.hasEffect("Frenzy")) return;

            FrenzyEffect effect = (FrenzyEffect) hero.getEffect("Frenzy");

            double newDamage = effect.processDamage(event.getDamage());

            event.setDamage(newDamage);

        }
    }

    public class FrenzyEffect extends ExpirableEffect {

        private SkillFrenzy skill;

        private double absorbPercentage;
        private double healMultiplier;

        private double damageDealt;

        public FrenzyEffect(SkillFrenzy skill, Player player,
                            long duration, double absorbPercentage, double healMultiplier,
                            String applyText, String expireText) {

            super(skill, "Frenzy", player, duration, applyText, expireText);
            this.skill = skill;
            this.absorbPercentage = absorbPercentage;
            this.healMultiplier = healMultiplier;
            this.damageDealt = 0;
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public double processDamage(double damage){
            double absorbedDamage = damage * absorbPercentage;
            damageDealt += absorbedDamage;
            return damage-absorbedDamage;
        }

        public void removeFromHero(Hero hero) {

            double healAmount = healMultiplier * damageDealt;

            hero.tryHeal(hero, skill, healAmount);

            super.removeFromHero(hero);
        }
    }
}