package ru.arcein.plugins.heroes.skills.manacirculation;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillCompleteEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class SkillManaCirculation extends PassiveSkill {
    public SkillManaCirculation(Heroes plugin) {
        super(plugin, "ManaCirculation");
        this.setDescription("Шанс восстановить часть от максимального запаса маны при использовании заклинания.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillManaCirculation.SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("mana-regen", 0.25);
        node.set("chance", 0.3);

        node.set(SkillSetting.USE_TEXT.node(), "%hero% восстанавливает часть маны, используя заклинание!");
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
        public void onSkillResult(SkillCompleteEvent event){
            if (event.getResult() != SkillResult.NORMAL) return;
            Hero hero = event.getHero();
            if (!hero.hasEffect("ManaCirculation")) return;

            double manaRegen = SkillConfigManager.getUseSetting(hero, skill, "mana-regen", 0.25, false);
            double chance    = SkillConfigManager.getUseSetting(hero, skill, "chance", 0.3, false);

            if(Math.random() >= chance) return;

            hero.tryRestoreMana(hero, skill, (int)(hero.getMaxMana() * manaRegen));
        }

    }
}
