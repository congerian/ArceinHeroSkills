package ru.arcein.plugins.heroes.skills.smoke;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillSmoke extends ActiveSkill {
    private String applyText;
    private String expireText;

    private Long damageTimeTreshold = 20000L;

    public Map<Player, Long> damages = new LinkedHashMap<Player, Long>(200) {
        public boolean removeEldestEntry(Map.Entry<Player, Long> eldest) {
            return this.size() > 200 || (Long)eldest.getValue() + damageTimeTreshold <= System.currentTimeMillis();
        }
    };

    public SkillSmoke(Heroes plugin) {
        super(plugin, "Smoke");
        this.setDescription("Вы скрываетесь в облаке дыма.");
        this.setUsage("/skill smoke");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill smoke"});
        this.setNotes(new String[]{"При получении урона вы выходите из невидимости."});
        this.setTypes(new SkillType[]{SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.DISABLE_COUNTERING, SkillType.STEALTHY});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillSmoke.SkillHeroListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set("damage-threshold-ms", 500L);
        node.set(SkillSetting.APPLY_TEXT.node(), "Вы исчезаете в облаке дыма!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "Вас снова видно!");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "Вы исчезаете в облаке дыма!");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Вас снова видно!");
    }

    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();


        if(damages.containsKey(player)){
            if(SkillConfigManager.getUseSetting(hero, this, "damage-threshold-ms", 500, false) >
                (System.currentTimeMillis() - damages.get(player))){
                player.sendMessage("Вы недавно получили урон, подождите!");
                return SkillResult.FAIL;
            }
        }

        this.broadcastExecuteText(hero);
        long duration = (long)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
        hero.addEffect(new InvisibleEffect(this, player, duration, this.applyText, this.expireText));
        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        private final SkillSmoke skill;

        public SkillHeroListener(SkillSmoke skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.LOW
        )
        public void onOtherDamage(EntityDamageEvent event) {
            if (!event.isCancelled() && event.getEntity() instanceof Player) {
                skill.damages.put((Player)event.getEntity(), System.currentTimeMillis());
            }
        }

    }

    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}