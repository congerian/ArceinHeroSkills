package ru.arcein.plugins.heroes.skills.mortalwound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class SkillMortalWound extends TargettedSkill {

    public SkillMortalWound(Heroes plugin) {
        super(plugin, "MortalWound");
        this.setDescription("Вы наносите глубокую рану противнику, временно предотвращая восстановление его здоровья.");
        this.setUsage("/skill MortalWound <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers(new String[]{"skill MortalWound"});
        this.setTypes(new SkillType[]{SkillType.DISABLING, SkillType.INTERRUPTING,
                SkillType.AGGRESSIVE, SkillType.SILENCEABLE});

        Bukkit.getPluginManager().registerEvents(new SkillMortalWoundListener(), plugin);
    }

    public String getDescription(Hero hero) { return this.getDescription(); }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.MAX_DISTANCE.node(), 5);

        node.set("initial-damage", 10.0);

        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% увечит %target%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% оправился от увечья!");

        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, LivingEntity target, String[] strings) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) return SkillResult.INVALID_TARGET;
        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET;

        Hero targetHero = this.plugin.getCharacterManager().getHero((Player)target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double initialDamage = SkillConfigManager.getUseSetting(hero, this, "initial-damage", 10.0, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% увечит %target%!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT.node(),
                "%target% оправился от увечья!")
                .replace("%hero%", hero.getName())
                .replace("%target%", targetHero.getName());

        MortalWoundEffect effect = new MortalWoundEffect(
                this, player,
                duration,
                applyText, expireText);

        targetHero.addEffect(effect);

        return SkillResult.NORMAL;

    }

    private class MortalWoundEffect extends ExpirableEffect {

        public MortalWoundEffect(Skill skill, Player applier, long duration, String applyText, String expireText) {
            super(skill, "MortalWound", applier, duration, applyText, expireText);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.HEALING_REDUCTION);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }
    }

    private class SkillMortalWoundListener implements Listener{
        @EventHandler
        public void onHealthRegain(EntityRegainHealthEvent event){
            if(event.isCancelled()) return;
            if(!(event.getEntity() instanceof Player)) return;

            Player player = (Player) event.getEntity();
            Hero hero = Heroes.getInstance().getCharacterManager().getHero(player);

            if(!hero.hasEffect("MortalWound")) return;
            event.setCancelled(true);
         }
    }

}