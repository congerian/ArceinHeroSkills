package ru.arcein.plugins.heroes.skills.dampen;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDampen extends PassiveSkill {

    public SkillDampen(Heroes plugin) {
        super(plugin, "Dampen");
        this.setDescription("Вы неуязвимы к атакам противников с маной меньше $1");
        this.setArgumentRange(0, 0);
        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.DISABLE_COUNTERING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDampen.EntityDamageListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double manaReq = SkillConfigManager.getUseSetting(hero, this, "block-if-mana-below", 0.3D, false);

        String description = this.getDescription().replace("$1", 100 * manaReq + "%");

        return description;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("block-if-mana-below", 0.3D);
        node.set("block-text", "Защитное поле %hero% предотвращает атаку %target%!");
        return node;
    }

    public void init() {
        super.init();
    }

    public class EntityDamageListener implements Listener {
        private SkillDampen skill;

        public EntityDamageListener(SkillDampen skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if(event.isCancelled()) return;

            Entity attackerEntity = event.getDamager();
            Entity attackedEntity = event.getEntity();

            if(attackerEntity instanceof Projectile){
                attackerEntity = (Entity) (((Projectile) attackerEntity).getShooter());
            }

            if(!(attackerEntity instanceof Player)) return;
            if(!(attackedEntity instanceof Player)) return;

            Player attackerPlayer = (Player) attackerEntity;
            Player attackedPlayer = (Player) attackedEntity;

            Hero attackerHero = Heroes.getInstance().getCharacterManager().getHero(attackerPlayer);
            Hero attackedHero = Heroes.getInstance().getCharacterManager().getHero(attackedPlayer);

            if(!attackedHero.hasEffect("Dampen")) return;

            String blockText = SkillConfigManager.getUseSetting(attackedHero, skill, "block-text",
                    "Защитное поле %hero% предотвращает атаку %target%!")
                    .replace("%hero%", attackedHero.getName())
                    .replace("%target%", attackerHero.getName());

            double minMANA = SkillConfigManager.getUseSetting(attackedHero, skill, "block-if-mana-below", 0.3D, false);
            double curMANA = ((double) attackerHero.getMana()) / attackerHero.getMaxMana();

            if(curMANA >= minMANA) return;

            event.setCancelled(true);

            skill.broadcast(attackedHero.getPlayer().getLocation(), blockText);
        }
    }
}