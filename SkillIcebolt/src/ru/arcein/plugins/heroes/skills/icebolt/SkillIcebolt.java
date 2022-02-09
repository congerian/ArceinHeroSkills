package ru.arcein.plugins.heroes.skills.icebolt;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

public class SkillIcebolt extends ActiveSkill {
    private Map<Snowball, Long> snowballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4632858378318784263L;

        public boolean removeEldestEntry(Entry<Snowball, Long> eldest) {
            return this.size() > 60 || (Long)eldest.getValue() + 5000L <= System.currentTimeMillis();
        }
    };
    private String applyText;
    private String expireText;

    public SkillIcebolt(Heroes plugin) {
        super(plugin, "Icebolt");
        this.setDescription("Вы запускаете ледяной шар, наносящий $1 урона и замедляющий вашу цель на $2 секунд.");
        this.setUsage("/skill icebolt");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill icebolt"});
        this.setTypes(new SkillType[]{SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_ICE,
                SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillIcebolt.SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0D, false);
        damage += (int)(damageIncrease * (double)hero.getAttributeValue(AttributeType.INTELLECT));
        String formattedDuration = Util.decFormat.format((double)duration / 1000.0D);
        return this.getDescription().replace("$1", String.valueOf(damage)).replace("$2", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0D);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set("slow-multiplier", 1);
        node.set("velocity-multiplier", 1.2D);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% был замедлен %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% больше не замедлен!");
        return node;
    }

    public void init() {
        super.init();

        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% замедлен %hero%!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% больше не замедлен!")
                .replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Snowball snowball = (Snowball)player.launchProjectile(Snowball.class);
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.1D, false);
        snowball.setVelocity(snowball.getVelocity().normalize().multiply(mult));
        this.snowballs.put(snowball, System.currentTimeMillis());
        this.broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onEntityDamage(ProjectileHitEvent event) {
            if (event.getHitEntity() instanceof LivingEntity) {
                Entity proj = event.getEntity();
                if (proj instanceof Snowball) {
                    Snowball projectile = (Snowball)proj;
                    if (SkillIcebolt.this.snowballs.containsKey(projectile)) {
                        SkillIcebolt.this.snowballs.remove(projectile);
                        ProjectileSource source = projectile.getShooter();
                        if (source instanceof Entity) {
                            Entity dmger = (Entity)source;
                            if (dmger instanceof Player) {
                                Hero hero = SkillIcebolt.this.plugin.getCharacterManager().getHero((Player)dmger);
                                if (!Skill.damageCheck((Player)dmger, (LivingEntity)event.getHitEntity())) {
                                    return;
                                }

                                event.getHitEntity().setFireTicks(0);
                                double damage = (double)SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50, false);
                                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill,
                                        SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0D, false);
                                damage += damageIncrease * (double)hero.getAttributeValue(AttributeType.INTELLECT);

                                long duration = (long)SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 4000, false);
                                int amplifier = SkillConfigManager.getUseSetting(hero, skill, "slow-multiplier", 1, false);

                                SlowEffect iceSlowEffect = new SlowEffect(skill, (Player)dmger, duration, amplifier,
                                        SkillIcebolt.this.applyText, SkillIcebolt.this.expireText);

                                iceSlowEffect.types.add(EffectType.DISPELLABLE);
                                iceSlowEffect.types.add(EffectType.ICE);
                                LivingEntity target = (LivingEntity)event.getHitEntity();

                                skill.plugin.getCharacterManager().getCharacter(target).addEffect(iceSlowEffect);
                                skill.addSpellTarget(event.getHitEntity(), hero);
                                skill.damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC);

                                target.getWorld().spigot().playEffect(target.getLocation().add(0.0D, 0.5D, 0.0D), Effect.TILE_BREAK,
                                        Material.ICE.getId(), 0, 0.2F, 0.2F, 0.2F, 0.1F, 50, 16);
                                target.getWorld().playSound(target.getLocation(), CompatSound.BLOCK_GLASS_BREAK.value(), 7.0F, 0.7F);
                            }

                        }
                    }
                }
            }
        }
    }
}