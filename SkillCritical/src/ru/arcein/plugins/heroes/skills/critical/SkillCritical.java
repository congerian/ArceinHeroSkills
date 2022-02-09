package ru.arcein.plugins.heroes.skills.critical;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
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

public class SkillCritical extends PassiveSkill {

    public SkillCritical(Heroes plugin) {
        super(plugin, "Critical");
        this.setDescription("Шанс нанести критический урон при ударе.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillCriticalListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("critical-chance", 0.1D);
        node.set("critical-multiplier", 1.5);
        return node;
    }

    public void init() {
        super.init();
    }

    public class SkillCriticalListener implements Listener {
        private final SkillCritical skill;

        public SkillCriticalListener(SkillCritical skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOWEST
        )
        public void onEntityDamage(EntityDamageByEntityEvent event) {

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

            if(!attackerHero.hasEffect("Critical")) return;

            double chance = SkillConfigManager.getUseSetting(attackerHero, this.skill, "critical-chance", 0.1D, false);

            if(Math.random() >= chance) return;

            double multiplier = SkillConfigManager.getUseSetting(attackerHero, this.skill, "critical-multiplier", 1.5, false);

            event.setDamage(event.getDamage() * multiplier);

        }

    }
}
