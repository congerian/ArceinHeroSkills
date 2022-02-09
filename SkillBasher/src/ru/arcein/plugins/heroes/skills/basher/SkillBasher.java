package ru.arcein.plugins.heroes.skills.basher;

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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillBasher extends PassiveSkill {

    public SkillBasher(Heroes plugin) {
        super(plugin, "Basher");
        this.setDescription("Шанс оглушить противника при ударе.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBasherListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("stun-chance", 0.1D);
        node.set("stun-duration", 2000L);
        node.set("stun-apply-text", "%hero% оглушает %target%!");
        node.set("stun-expire-text", "%target% больше не оглушен!");
        return node;
    }

    public void init() {
        super.init();
    }

    public class SkillBasherListener implements Listener {
        private final SkillBasher skill;

        public SkillBasherListener(SkillBasher skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (event.isCancelled()) return;

            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

            Entity attackerEntity = event.getDamager();
            Entity attackedEntity = event.getEntity();

            if (!(attackerEntity instanceof Player)) return;
            if (!(attackedEntity instanceof Player)) return;

            Player attackerPlayer = (Player) attackerEntity;
            Player attackedPlayer = (Player) attackedEntity;

            Hero attackerHero = Heroes.getInstance().getCharacterManager().getHero(attackerPlayer);
            Hero attackedHero = Heroes.getInstance().getCharacterManager().getHero(attackedPlayer);

            if(!attackerHero.hasEffect("Basher")) return;

            double chance = SkillConfigManager.getUseSetting(attackerHero, this.skill, "stun-chance", 0.1D, false);

            if(Math.random() >= chance) return;

            int duration = SkillConfigManager.getUseSetting(attackerHero, this.skill, "stun-duration", 1000, false);

            String applyText = SkillConfigManager.getUseSetting(
                    attackerHero, this.skill, "stun-apply-text", "%hero% оглушает %target%!")
                    .replace("%hero%", attackerHero.getName())
                    .replace("%target%", attackedHero.getName());

            String expireText = SkillConfigManager.getUseSetting(
                    attackerHero, this.skill, "stun-expire-text", "%target% больше не оглушен!")
                    .replace("%target%", attackedHero.getName());

            attackedHero.addEffect(new StunEffect(this.skill, attackedPlayer, duration, applyText, expireText));
        }

    }
}
