package ru.arcein.plugins.heroes.skills.stalemate;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SkillStaleMate extends TargettedSkill {

    public SkillStaleMate(Heroes plugin) {
        super(plugin, "StaleMate");
        this.setDescription("Вы накладываете на себя и на цель печать смирения, запрещающую вам атаковать друг друга.");
        this.setUsage("/skill StaleMate <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill StaleMate"});
        this.setTypes(new SkillType[]{SkillType.DISABLING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE});

        Bukkit.getServer().getPluginManager().registerEvents(new SkillStaleMateListener(this), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% накладывает печать смирения на %target%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не скован печатью смирения!");

        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {

        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;

        Player targetPlayer = (Player) target;
        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        String heroApplyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% накладывает печать смирения на %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", hero.getName());

        String targetApplyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% накладывает печать смирения на %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        String heroExpireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не скован печатью смирения!")
                .replace("%hero%", hero.getName());

        String targetExpireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не скован печатью смирения!")
                .replace("%hero%", targetHero.getName());

        SkillStaleMateEffect heroEffect = new SkillStaleMateEffect(
                this,
                player, targetPlayer,
                duration,
                heroApplyText, heroExpireText);

        SkillStaleMateEffect targetEffect = new SkillStaleMateEffect(
                this,
                player, player,
                duration,
                targetApplyText, targetExpireText);

        targetHero.addEffect(targetEffect);
        hero.addEffect(heroEffect);

        return SkillResult.NORMAL;

    }

    private class SkillStaleMateEffect extends ExpirableEffect {
        private Player staleMate;

        public SkillStaleMateEffect(Skill skill,
                                    Player applier, Player staleMate,
                                    long duration, String applyText, String expireText) {
            super(skill, "StaleMate", applier, duration, applyText, expireText);

            this.staleMate = staleMate;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.DISABLE);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero){
            super.removeFromHero(hero);
        }
    }

    private class SkillStaleMateListener implements Listener{
        SkillStaleMate skill;

        public SkillStaleMateListener(SkillStaleMate skill){
            this.skill = skill;
        }

        @EventHandler
                (priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageByEntityEvent event){
            if (event.isCancelled()) return;

            Entity attackerEntity = event.getDamager();
            Entity attackedEntity = event.getEntity();

            if (attackerEntity instanceof Projectile){
                attackerEntity = (Entity) (((Projectile) attackerEntity).getShooter());
            }

            if (!(attackerEntity instanceof Player)) return;
            if (!(attackedEntity instanceof Player)) return;

            Player attackerPlayer = (Player) attackerEntity;
            Player attackedPlayer = (Player) attackedEntity;

            Hero attackerHero = Heroes.getInstance().getCharacterManager().getHero(attackerPlayer);
            Hero attackedHero = Heroes.getInstance().getCharacterManager().getHero(attackedPlayer);

            if (!attackerHero.hasEffect("StaleMate")) return;

            SkillStaleMateEffect effect = (SkillStaleMateEffect) attackerHero.getEffect("StaleMate");

            if (!effect.staleMate.equals(attackedPlayer)) return;

            event.setCancelled(true);
        }
    }

}