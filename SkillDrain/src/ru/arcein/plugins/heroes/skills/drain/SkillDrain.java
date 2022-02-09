package ru.arcein.plugins.heroes.skills.drain;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDrain extends PassiveSkill {
    public SkillDrain(Heroes plugin) {
        super(plugin, "Drain");
        this.setDescription("Ваши удары выкачивают ману противника.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING, SkillType.MANA_DECREASING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDrain.SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("mana-drain-per-attack", 4);
        node.set("mana-gain-per-attack", 1.0);

        node.set("particle-amount", 20);

        return node;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
                (priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageByEntityEvent event) {

            if(event.isCancelled()) return;

            if(event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

            Entity attackerEntity = event.getDamager();
            Entity attackedEntity = event.getEntity();

            if(!(attackerEntity instanceof Player)) return;
            if(!(attackedEntity instanceof Player)) return;

            Player attackerPlayer = (Player) attackerEntity;
            Player attackedPlayer = (Player) attackedEntity;

            Hero attackerHero = Heroes.getInstance().getCharacterManager().getHero(attackerPlayer);
            Hero attackedHero = Heroes.getInstance().getCharacterManager().getHero(attackedPlayer);

            if(!attackerHero.hasEffect("Drain")) return;

            int manaDrain = SkillConfigManager.getUseSetting(attackerHero, skill, "mana-drain-per-attack", 4, false);
            double manaGain = SkillConfigManager.getUseSetting(attackerHero, skill, "mana-gain-per-attack", 1.0, false);

            if(attackedHero.getMana() < manaDrain){

                attackerHero.setMana(Math.min(
                        attackerHero.getMana() + (int)(attackedHero.getMana() * manaGain),
                        attackerHero.getMaxMana()));

                attackedHero.setMana(0);

            }
            else{

                attackerHero.setMana(Math.min(
                        attackerHero.getMana() + (int)(manaDrain * manaGain),
                        attackerHero.getMaxMana()));

                attackedHero.setMana(attackedHero.getMana() - manaDrain);

            }

            playEffect(attackedHero);

        }

        private void playEffect(Hero hero) {

            int amount = SkillConfigManager.getUseSetting(hero, skill, "particle-amount", 20, false);

            Location location = hero.getPlayer().getLocation().clone().add(0.0, 0.5, 0.0);

            hero.getPlayer().getWorld().spawnParticle(
                    Particle.CRIT_MAGIC,
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                    amount,
                    0.5F, 0.5F, 0.5F);

            hero.getPlayer().getWorld().playSound(location, Sound.ENTITY_BLAZE_DEATH, 0.2F, 4.0F);
        }
    }
}
