/* 犟牛地牢扩展，遵循 GPL-3.0。使用原版卷轴美术。 */
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/** Tactical reset: vulnerable nearby hostiles, a short shield, and recharge. */
public class ScrollOfCowCouncil extends Scroll {

    @Override public void reset() {
        super.reset();
        image = ItemSpriteSheet.SCROLL_ODAL;
    }

    @Override public boolean isKnown() { return true; }

    /** Exposed separately for deterministic combat tests; this does not consume the paper. */
    public static int council(Hero hero) {
        if (hero == null || !hero.isAlive() || Dungeon.level == null) return 0;
        int affected = 0;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (!mob.isAlive() || mob.alignment != Char.Alignment.ENEMY
                    || !Dungeon.level.insideMap(mob.pos)
                    || Dungeon.level.distance(hero.pos, mob.pos) > 3
                    || hero.fieldOfView == null || !hero.fieldOfView[mob.pos]) continue;
            if (new Ballistica(hero.pos, mob.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID)
                    .collisionPos != mob.pos) continue;
            Buff.prolong(mob, Weakness.class, 4f);
            Buff.prolong(mob, Cripple.class, 3f);
            affected++;
        }
        Buff.affect(hero, CowGuard.class).set(4 + hero.lvl/5, 5);
        Buff.prolong(hero, Recharging.class, 5f);
        return affected;
    }

    @Override public void doRead() {
        if (curUser == null || !curUser.isAlive()) return;
        detach(curUser.belongings.backpack);
        int affected = council(curUser);
        identify();
        if (curUser.sprite != null) GLog.p(Messages.get(this, "used", affected));
        readAnimation();
    }

    @Override public int value() { return 45 * quantity(); }
}
