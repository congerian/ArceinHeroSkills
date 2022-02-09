package ru.arcein.plugins.heroes.skills.bloodlust;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import net.minecraft.server.v1_12_R1.MobEffects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillBloodlust extends ActiveSkill {

    public SkillBloodlust(Heroes plugin) {
        super(plugin, "Bloodlust");
        this.setDescription("Вы жертвуете частью своего здоровья, получая временное усиление брони и урона.");
        this.setUsage("/skill bloodlust");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill bloodlust"});
        this.setTypes(new SkillType[]{SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.DAMAGING});
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% исполнен жаждой!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% больше не исполнен жаждой!");
        node.set("strength-power", 1);
        node.set("absorption-power", 1);
        node.set("health-percentage", 0.5D);
        return node;
    }

    public void init() { super.init(); }

    public SkillResult use(Hero hero, String[] args) {

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int absAmpl = SkillConfigManager.getUseSetting(hero, this, "strength-power", 1, false);
        int strAmpl = SkillConfigManager.getUseSetting(hero, this, "absorption-power", 1, false);
        double health = SkillConfigManager.getUseSetting(hero, this, "health-percentage", 0.5D, false);

        String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT.node(),
                "%hero% исполнен жаждой!").replace("%hero%", hero.getName());
        String expireText = SkillConfigManager.getUseSetting(hero,this, SkillSetting.EXPIRE_TEXT.node(),
                "%hero% больше не исполнен жаждой!").replace("%hero%", hero.getName());

        hero.getPlayer().setHealth(hero.getPlayer().getHealth() * health);

        SkillBloodlust.BloodlustEffect beffect = new SkillBloodlust.BloodlustEffect(
                this, hero.getPlayer(), duration, applyText, expireText, absAmpl, strAmpl);
        hero.addEffect(beffect);

        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }

    public class BloodlustEffect extends ExpirableEffect {
        private String applyText;
        private String expireText;

        public BloodlustEffect(Skill skill, Player player, long duration, String applyText, String expireText, int absAmpl, int strAmpl) {
            super(skill, "Bloodlust", player, duration, applyText, expireText);
            this.applyText = applyText;
            this.expireText = expireText;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.PHYSICAL);
            this.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)(duration/1000L*20L), absAmpl, false));
            this.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int)(duration/1000L*20L), strAmpl, false));
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }
}