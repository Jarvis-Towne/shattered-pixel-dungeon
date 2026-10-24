/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowCraft;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NightShift;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class MidnightSun extends ArmorAbility {
    { baseChargeUse = 40f; }
    public static float duration(Hero hero) {
        return 8f + 2f*hero.pointsInTalent(Talent.COW_LONG_NIGHT)
                + (CowCraft.rank(hero, CowCraft.NIGHT) >= 2 ? 2f : 0f);
    }
    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (armor == null || hero == null || !hero.isAlive() || hero.paralysed > 0
                || Dungeon.level == null || armor.charge < chargeUse(hero)) return;
        float duration = duration(hero);
        Buff.prolong(hero, NightShift.class, duration).beginPractice();
        if (CowCraft.rank(hero, CowCraft.NIGHT) >= 1) Buff.detach(hero, Cripple.class);
        Buff.prolong(hero, Light.class, duration);
        Buff.prolong(hero, Recharging.class, duration);
        Buff.affect(hero, Stubbornness.class).gain(2);
        int dawn = hero.pointsInTalent(Talent.COW_DAWN_BRINGER);
        if (dawn > 0) {
            Buff.affect(hero, CowGuard.class).set(3*dawn, (int)duration);
            Buff.detach(hero, Burning.class);
            Buff.detach(hero, Poison.class);
        }
        armor.charge -= chargeUse(hero);
        Item.updateQuickslot();
        if (hero.sprite != null) {
            hero.sprite.operate(hero.pos);
            SpellSprite.show(hero, SpellSprite.CHARGE);
            Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
            GLog.i(Messages.get(this, "cast"));
        }
        Invisibility.dispel();
        hero.spendAndNext(Actor.TICK);
    }
    @Override public int icon() { return HeroIcon.COW_MIDNIGHT_SUN; }
    @Override public String desc() {
        return super.desc() + "\n\n" + CowCraft.progress(Dungeon.hero, CowCraft.NIGHT)
                + "\n" + Messages.get(this, "evolution");
    }
    @Override public Talent[] talents() { return new Talent[]{Talent.COW_LONG_NIGHT, Talent.COW_SOLAR_FLARE, Talent.COW_DAWN_BRINGER, Talent.HEROIC_ENERGY}; }
}
