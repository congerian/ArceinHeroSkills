package ru.arcein.plugins.heroes.skills.deathfromabove;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDeathFromAbove extends ActiveSkill {

    public SkillDeathFromAbove(Heroes plugin) {
        super(plugin, "DeathFromAbove");
        this.setDescription("Вы подготавливаетесь к прыжку, при следующем падении, " +
                "вы получите меньше урона, нанесёте урон врагам вблизи точки падения.");
        this.setUsage("/skill DeathFromAbove");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill DeathFromAbove"});
        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.DAMAGING});

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDeathFromAboveListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);

        node.set("fall-damage-absorption", 1.0);
        node.set("fall-damage-to-enemies", 1.0);
        node.set("max-damage-to-enemies", 30.0);
        node.set("enemies-radius", 5.0);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% готовится к прыжку!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не готов к прыжку!");

        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, String[] args) {

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        double fallDamageAbsorption = SkillConfigManager.getUseSetting(hero, this,
                "fall-damage-absorption", 1.0, false);
        double fallDamageToEnemies  = SkillConfigManager.getUseSetting(hero, this,
                "fall-damage-to-enemies", 1.0, false);
        double maxDamageToEnemies   = SkillConfigManager.getUseSetting(hero, this,
                "max-damage-to-enemies", 30.0, false);
        double enemiesRadius        = SkillConfigManager.getUseSetting(hero, this,
                "enemies-raduis", 5.0, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% готовится к прыжку!").replace("%hero%", hero.getName());
        String expireText = SkillConfigManager.getUseSetting(hero,this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не готов к прыжку!").replace("%hero%", hero.getName());

        SkillDeathFromAboveEffect effect = new SkillDeathFromAboveEffect(
                this, hero.getPlayer(),
                duration,
                fallDamageAbsorption,
                fallDamageToEnemies,
                maxDamageToEnemies,
                enemiesRadius,
                applyText, expireText);

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    private class SkillDeathFromAboveEffect extends ExpirableEffect {

        SkillDeathFromAbove skill;

        double fallDamageAbsorption;
        double fallDamageToEnemies;
        double maxDamageToEnemies;
        double enemiesRadius;

        public SkillDeathFromAboveEffect(SkillDeathFromAbove skill, Player player,
                                         long duration,
                                         double fallDamageAbsorption,
                                         double fallDamageToEnemies,
                                         double maxDamageToEnemies,
                                         double enemiesRadius,
                                         String applyText, String expireText)
        {
            super(skill, "DeathFromAbove", player, duration, applyText, expireText);

            this.skill = skill;

            this.fallDamageAbsorption = fallDamageAbsorption;
            this.fallDamageToEnemies  = fallDamageToEnemies;
            this.maxDamageToEnemies   = maxDamageToEnemies;
            this.enemiesRadius        = enemiesRadius;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }

    private class SkillDeathFromAboveListener implements Listener{

        @EventHandler
                (priority = EventPriority.LOWEST)

        private void onFallDamage(EntityDamageEvent event){

            if (event.isCancelled()) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
            if (!(event.getEntity() instanceof Player)) return;

            Player player = (Player) event.getEntity();
            Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

            if (!hero.hasEffect("DeathFromAbove")) return;

            SkillDeathFromAboveEffect effect = (SkillDeathFromAboveEffect) hero.getEffect("DeathFromAbove");

            double fallDamageAbsorption = effect.fallDamageAbsorption;
            double fallDamageToEnemies  = effect.fallDamageToEnemies;
            double maxDamageToEnemies   = effect.maxDamageToEnemies;
            double enemiesRadius        = effect.enemiesRadius;

            double damage = event.getDamage();

            for (Entity entity : player.getNearbyEntities(enemiesRadius, enemiesRadius, enemiesRadius)){
                if (!(entity instanceof Player)) continue;
                if (!damageCheck(player, (LivingEntity) entity)) continue;

                Player target = (Player) entity;
                effect.skill.addSpellTarget(entity, hero);
                effect.skill.damageEntity(target, player, Math.min(maxDamageToEnemies, damage*fallDamageToEnemies));
            }

            event.setDamage((1.0 - fallDamageAbsorption)*damage);
            hero.removeEffect(effect);
        }

    }

}