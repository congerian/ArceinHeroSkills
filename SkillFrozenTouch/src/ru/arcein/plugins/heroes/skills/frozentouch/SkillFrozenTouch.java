package ru.arcein.plugins.heroes.skills.frozentouch;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillFrozenTouch extends PassiveSkill {
    public SkillFrozenTouch(Heroes plugin) {
        super(plugin, "FrozenTouch");
        this.setDescription("Ваши удары накладывают на противника замедление.");
        this.setTypes(new SkillType[]{SkillType.DISABLE_COUNTERING, SkillType.BUFFING, SkillType.MANA_INCREASING});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillFrozenTouch.SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("slow-duration", 2000);
        node.set("slow-multiplier", 2);
        node.set("chance", 0.3);

        node.set("particle-amount", 20);

        return node;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
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

            if(!attackerHero.hasEffect("FrozenTouch")) return;

            int slowDuration = SkillConfigManager.getUseSetting(attackerHero, skill, "slow-duration", 2000, false);
            int slowMultiplier = SkillConfigManager.getUseSetting(attackerHero, skill, "slow-multiplier", 2, false);
            double chance = SkillConfigManager.getUseSetting(attackerHero, skill, "chance", 0.3, false);

            if(Math.random() >= chance) return;

            SlowEffect iceSlowEffect = new SlowEffect(skill, attackerPlayer, slowDuration, slowMultiplier);

            iceSlowEffect.types.add(EffectType.DISPELLABLE);
            iceSlowEffect.types.add(EffectType.ICE);

            attackedHero.addEffect(iceSlowEffect);

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
