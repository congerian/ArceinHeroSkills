package ru.arcein.plugins.heroes.skills.sniping;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillSniping extends PassiveSkill {
    public SkillSniping(Heroes plugin) {
        super(plugin, "Sniping");
        this.setDescription("Ваши стрелы летят только по прямой.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillSnipingListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("min-force", 0.95);

        return node;
    }

    public class SkillSnipingListener implements Listener {
        private Skill skill;

        public SkillSnipingListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST
        )
        public void onShoot(EntityShootBowEvent event){
            if (event.isCancelled()) return;
            if (!(event.getEntity() instanceof Player)) return;
            if (!(event.getProjectile() instanceof Arrow)) return;

            Player player = (Player) event.getEntity();
            Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);
            Arrow arrow = (Arrow) event.getProjectile();

            if (!hero.hasEffect("Sniping")) return;

            double force = event.getForce();
            double minForce = SkillConfigManager.getUseSetting(hero, skill, "min-force", 0.95, false);

            if (force < minForce) {
                event.setCancelled(true);
                return;
            }

            arrow.setGravity(false);
        }

    }
}
