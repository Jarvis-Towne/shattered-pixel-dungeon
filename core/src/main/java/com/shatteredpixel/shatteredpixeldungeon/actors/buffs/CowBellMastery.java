/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/** Bell-specific progression and one draw per main dungeon depth, including revisits. */
public class CowBellMastery extends Buff {
    private int successfulCasts;
    private int drawnDepths;

    { revivePersists = true; }

    public static int uses(Hero hero) {
        CowBellMastery mastery = hero == null ? null : hero.buff(CowBellMastery.class);
        return mastery == null ? 0 : mastery.successfulCasts;
    }

    public static int rank(Hero hero) {
        if (hero == null || hero.heroClass != HeroClass.COW) return 0;
        int count = uses(hero);
        return count >= 15 ? 3 : count >= 8 ? 2 : count >= 3 ? 1 : 0;
    }

    /** Never count a cast that hit no real hostile HP or shielding. */
    public static void record(Hero hero, int enemiesHit) {
        if (enemiesHit <= 0 || hero == null || !hero.isAlive() || hero.heroClass != HeroClass.COW) return;
        int previous = rank(hero);
        CowBellMastery mastery = Buff.affect(hero, CowBellMastery.class);
        if (mastery.successfulCasts < Integer.MAX_VALUE) mastery.successfulCasts++;
        if (rank(hero) > previous && hero.sprite != null) {
            GLog.p(Messages.get(CowBellMastery.class, "evolved", rank(hero)));
        }
    }

    public static int growth(Hero hero) {
        return (int)Math.round(1.3 * (Math.sqrt(1 + uses(hero)/5.0) - 1));
    }

    /** Depth is indexed across branches, preventing branch-switch or return-trip farming. */
    private static int bitForDepth() {
        return Dungeon.depth > 0 && Dungeon.depth <= 30 ? 1 << Dungeon.depth : 0;
    }

    public static boolean canDraw(Hero hero) {
        if (hero == null || !hero.isAlive() || hero.heroClass != HeroClass.COW
                || Dungeon.level == null || CowLegend.stage(hero) < 2 || rank(hero) < 2) return false;
        CowBellMastery mastery = hero.buff(CowBellMastery.class);
        int bit = bitForDepth();
        return mastery != null && bit != 0 && (mastery.drawnDepths & bit) == 0;
    }

    /** Mark first, before any rewards or effects that can interrupt an event. */
    public static boolean markDraw(Hero hero) {
        if (!canDraw(hero)) return false;
        CowBellMastery mastery = hero.buff(CowBellMastery.class);
        mastery.drawnDepths |= bitForDepth();
        return true;
    }

    public static String progress(Hero hero) {
        return Messages.get(CowBellMastery.class, "progress", uses(hero), rank(hero));
    }

    @Override public String heroMessage() { return null; }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("cow_bell_mastery_uses", successfulCasts);
        bundle.put("cow_bell_drawn_depths", drawnDepths);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        successfulCasts = Math.max(0, bundle.getInt("cow_bell_mastery_uses"));
        drawnDepths = bundle.getInt("cow_bell_drawn_depths") & 0x7ffffffe;
    }
}
