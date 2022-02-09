package ru.arcein.plugins.heroes.skills.poisontouch;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillPoisonTouch extends PassiveSkill {
    public SkillPoisonTouch(Heroes plugin) {
        super(plugin, "PoisonTouch");
        this.setDescription("Ваши удары имеют шанс отравить противника.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_POISON});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPoisonTouchListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("poison-duration-ms", 3000);
        node.set("poison-power", 2);
        node.set("poison-chance", 1.0);

        node.set("particle-amount", 20);

        return node;
    }

    private class SkillPoisonTouchListener implements Listener {
        private Skill skill;

        public SkillPoisonTouchListener(Skill skill) {
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

            if(!attackerHero.hasEffect("PoisonTouch")) return;

            double chance           = SkillConfigManager.getUseSetting(attackerHero, skill, "poison-chance", 1.0, false);
            long   poisonDurationMs = SkillConfigManager.getUseSetting(attackerHero, skill, "poison-duration-ms", 3000, false);
            int    poisonPower      = SkillConfigManager.getUseSetting(attackerHero, skill, "poison-power", 2, false);

            if(Math.random() >= chance) return;

            attackedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int)((poisonDurationMs*20/1000)), poisonPower));
        }
    }
}
