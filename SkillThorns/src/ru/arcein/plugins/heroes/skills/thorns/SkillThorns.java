package ru.arcein.plugins.heroes.skills.thorns;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillThorns extends PassiveSkill {

    public SkillThorns(Heroes plugin) {
        super(plugin, "Thorns");
        this.setDescription("Вы отражаете обратно в противника часть нанесённого в ближнем бою урона.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillThornsListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("reflection-quotient", 0.25D);
        return node;
    }

    public void init() {
        super.init();
    }

    public class SkillThornsListener implements Listener {
        private final SkillThorns skill;

        public SkillThornsListener(SkillThorns skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
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

            if(!attackedHero.hasEffect("Thorns")) return;

            double reflectionQuotient = SkillConfigManager.getUseSetting(attackerHero, this.skill,
                    "reflection-quotient", 0.25D, false);

            double damage = event.getDamage();

            event.setDamage(damage*(1-reflectionQuotient));

            if(!damageCheck(attackedPlayer, (LivingEntity) attackerPlayer)) return;

            skill.damageEntity(attackerPlayer, attackedPlayer, damage*reflectionQuotient, EntityDamageEvent.DamageCause.CUSTOM);
        }

    }
}