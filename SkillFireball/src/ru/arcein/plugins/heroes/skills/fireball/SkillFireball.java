package ru.arcein.plugins.heroes.skills.fireball;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class SkillFireball extends ActiveSkill {
    private Map<Snowball, Long> fireballs = new LinkedHashMap<Snowball, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        protected boolean removeEldestEntry(Entry<Snowball, Long> eldest) {
            return this.size() > 60 || (Long)eldest.getValue() + 5000L <= System.currentTimeMillis();
        }
    };

    public SkillFireball(Heroes plugin) {
        super(plugin, "Fireball");
        this.setDescription("Вы выстреливаете огненным шаром, наносящим $1 урона и поджигающим вашу цель.");
        this.setUsage("/skill fireball");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill fireball"});
        this.setTypes(new SkillType[]{SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillFireball.SkillEntityListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.25D, false);
        damage += (int)(damageIncrease * (double)hero.getAttributeValue(AttributeType.INTELLECT));
        return this.getDescription().replace("$1", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.25D);
        node.set("velocity-multiplier", 1.5D);
        node.set("fire-ticks", 50);
        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5D, false);
        Snowball fireball = (Snowball)player.launchProjectile(Snowball.class);
        fireball.setVelocity(fireball.getVelocity().normalize().multiply(mult));
        fireball.setFireTicks(1000);
        fireball.setGravity(false);
        //fireball.setShooter(player);
        this.fireballs.put(fireball, System.currentTimeMillis());
        this.broadcastExecuteText(hero);
        player.getWorld().spigot().playEffect(player.getLocation(), Effect.BLAZE_SHOOT);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST,
                ignoreCancelled = true
        )
        public void onEntityDamage(ProjectileHitEvent event) {
            if (event.getHitEntity() instanceof LivingEntity) {
                Entity proj = event.getEntity();
                if (proj instanceof Snowball && SkillFireball.this.fireballs.containsKey(proj)) {
                    SkillFireball.this.fireballs.remove(proj);
                    Snowball projectile = (Snowball)proj;
                    LivingEntity target = (LivingEntity)event.getHitEntity();
                    ProjectileSource source = projectile.getShooter();
                    if (source instanceof Entity) {
                        Entity dmger = (LivingEntity)source;
                        if (dmger instanceof Player) {
                            Hero hero = SkillFireball.this.plugin.getCharacterManager().getHero((Player)dmger);
                            if (!Skill.damageCheck((Player)dmger, target)) {
                                return;
                            }

                            int fireTicks = SkillConfigManager.getUseSetting(hero, this.skill, "fire-ticks", 50, false);

                            target.setFireTicks(fireTicks);

                            SkillFireball.this.plugin.getCharacterManager().getCharacter(target).addEffect(new CombustEffect(this.skill, (Player)dmger));
                            double damage = (double)SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE, 80, false);
                            double damageIncrease = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5D, false);
                            damage += damageIncrease * (double)hero.getAttributeValue(AttributeType.INTELLECT);
                            SkillFireball.this.addSpellTarget(target, hero);
                            SkillFireball.this.damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC);
                            target.getWorld().spigot().playEffect(target.getLocation().add(0.0D, 0.5D, 0.0D), Effect.FLAME, 0, 0, 0.2F, 0.2F, 0.2F, 0.1F, 50, 16);
                            target.getWorld().playSound(target.getLocation(), CompatSound.BLOCK_FIRE_AMBIENT.value(), 7.0F, 1.0F);
                        }

                    }
                }
            }
        }
    }
}
