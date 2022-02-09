package ru.arcein.plugins.heroes.skills.passivemagicshield;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SkillPassiveMagicShield extends PassiveSkill {

    public SkillPassiveMagicShield(Heroes plugin) {
        super(plugin, "PassiveMagicShield");
        this.setDescription("Вы имеете способность частично смягчать магический урон.");
        this.setArgumentRange(0, 0);
        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.DISABLE_COUNTERING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPassiveMagicShield.EntityDamageListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("resist-value", 0.2D);
        return node;
    }

    public void init() {
        super.init();
    }


    public class EntityDamageListener implements Listener {
        private SkillPassiveMagicShield skill;

        public EntityDamageListener(SkillPassiveMagicShield skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST,
                ignoreCancelled = true
        )
        public void onSkillDamage(SkillDamageEvent event) {
            Skill eventSkill = event.getSkill();

            if (eventSkill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL)) return;
            if (!eventSkill.isType(SkillType.DAMAGING)) return;

            if (!(event.getEntity() instanceof Player)) return;

            Hero hero = skill.plugin.getCharacterManager().getHero((Player)event.getEntity());

            if (!hero.hasEffect("PassiveMagicShield")) return;

            double resistValue = 1.0D - SkillConfigManager.getUseSetting(hero, this.skill, "resist-value", 0.2D, false);
            double newDamage = event.getDamage() * resistValue;
            event.setDamage(newDamage);
        }

    }
}