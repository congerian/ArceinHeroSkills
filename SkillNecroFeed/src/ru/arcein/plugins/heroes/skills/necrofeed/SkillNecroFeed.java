package ru.arcein.plugins.heroes.skills.necrofeed;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class SkillNecroFeed extends PassiveSkill {
    public SkillNecroFeed(Heroes plugin) {
        super(plugin, "NecroFeed");
        this.setDescription("Passive $1 health gain on enemy killed");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillNecroFeed.SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double health = SkillConfigManager.getUseSetting(hero, this, "health-percent", 0.5D, false) + SkillConfigManager.getUseSetting(hero, this, "health-percent-increase", 0.0D, false) * (double)hero.getSkillLevel(this);
        health = health > 0.0D ? health : 0.0D;
        String description = this.getDescription().replace("$1", health + "");
        return description;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("health-percent", 1);
        node.set("health-percent-increase", 0);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% feeds on an enemies corpse");
        return node;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(
                ignoreCancelled = true
        )
        public void onEntityDeath(EntityDeathEvent event) {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent edby = (EntityDamageByEntityEvent)event.getEntity().getLastDamageCause();
                Player player = null;
                if (edby.getDamager().getClass().equals(Player.class)) {
                    player = (Player)edby.getDamager();
                } else {
                    if (!(edby.getDamager() instanceof Projectile)) {
                        return;
                    }

                    if (!( ((Projectile) edby.getDamager()).getShooter() instanceof Player)) return;

                    player = (Player) ((Projectile)edby.getDamager()).getShooter();

                }

                if (player != null) {
                    Hero hero = SkillNecroFeed.this.plugin.getCharacterManager().getHero(player);
                    if (hero.hasEffect("NecroFeed")) {
                        LivingEntity ex;
                        try {
                            ex = event.getEntity();
                        } catch (Exception var10) {
                            return;
                        }

                        double health = SkillConfigManager.getUseSetting(hero, this.skill, "health-percent", 0.5D, false) + SkillConfigManager.getUseSetting(hero, this.skill, "health-percent-increase", 0.0D, false) * (double)hero.getSkillLevel(this.skill);
                        health = health > 0.0D ? health : 0.0D;

                        double amount = health * ex.getMaxHealth();

                        hero.tryHeal(amount);

                        SkillNecroFeed.this.broadcast(player.getLocation(), SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.USE_TEXT, "%hero% feeds on an enemies corpse").replace("%hero%", player.getDisplayName()), new Object[0]);
                    }
                }
            }
        }
    }
}
