/* 犟牛地牢扩展，遵循 GPL-3.0。使用原游戏药水图标，不新增或改动任何专属装备美术。 */
package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/** A low-frequency, known supply potion, distributed by cow encounters rather than the global potion deck. */
public class PotionOfBullishResolve extends Potion {
    @Override public void reset() {
        super.reset();
        image = ItemSpriteSheet.POTION_AMBER;
    }

    @Override public boolean isKnown() { return true; }

    @Override public void apply(Hero hero) {
        if (hero == null || !hero.isAlive()) return;
        Buff.prolong(hero, Bless.class, 5f);
        Buff.affect(hero, CowGuard.class).set(3 + hero.lvl/6, 5);
        if (hero.heroClass == HeroClass.COW) {
            Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
            stubborn.gain(3 + hero.lvl/10);
        }
        if (hero.sprite != null) GLog.p(Messages.get(this, "used"));
    }

    @Override public int value() { return 35 * quantity(); }
}
