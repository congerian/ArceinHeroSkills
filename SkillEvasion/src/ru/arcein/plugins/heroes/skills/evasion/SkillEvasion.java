package ru.arcein.plugins.heroes.skills.evasion;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import java.util.Random;

import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillEvasion extends PassiveSkill {
    public SkillEvasion(Heroes plugin) {
        super(plugin, "Evasion");
        this.setDescription("Шанс $1% уклониться от урона");
        this.setIdentifiers(new String[]{"skill evasion"});
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEvasion.SkillEvasionListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("particle-power", 0.5D);
        node.set("particle-amount", 10);

        node.set("chance", 0.25D);

        return node;
    }

    public String getDescription(Hero hero) {
        String description = "" + this.getDescription();

        double chance = SkillConfigManager.getUseSetting(hero, this, "chance", 0.25D, false);

        description = description.replace("$1", "" + (chance * 100));

        return description;
    }

    public class SkillEvasionListener implements Listener {

        SkillEvasion skill;

        public SkillEvasionListener(SkillEvasion skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled()) return;
            if (!(event.getEntity() instanceof Player)) return;

            if (event.getDamage() < 0.25) return;

            Player player = (Player)event.getEntity();
            Hero hero = skill.plugin.getCharacterManager().getHero(player);

            if (!hero.hasEffect("Evasion")) return;

            double chance = SkillConfigManager.getUseSetting(hero, skill, "chance", 0.5D, false);

            if (Math.random() >= chance) return;

            event.setCancelled(true);
            SkillEvasion.this.broadcast(player.getLocation(), "", new Object[]{player.getDisplayName()});
            this.playEffect(hero);
        }

        public void playEffect(Hero hero) {
            float power = (float)SkillConfigManager.getUseSetting(hero, SkillEvasion.this, "particle-power", 0.5D, false);
            int amount = SkillConfigManager.getUseSetting(hero, SkillEvasion.this, "particle-amount", 10, false);
            Location location = hero.getPlayer().getLocation();
            location.setY(location.getY() + 0.5D);
            hero.getPlayer().getWorld().spigot().playEffect(location, Effect.MAGIC_CRIT,
                    0, 0, 0.0F, 0.0F, 0.0F, power, amount, 64);
        }
    }
}