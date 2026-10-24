/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/** Independent, finite practice for the three armor abilities. */
public class CowCraft extends Buff {
    public static final int STAMPEDE = 0;
    public static final int PASTURE = 1;
    public static final int NIGHT = 2;
    private final int[] uses = new int[3];

    { revivePersists = true; }

    private static boolean validSkill(int skill) { return skill >= STAMPEDE && skill <= NIGHT; }

    /** Reading a menu must not allocate a new progress buff. */
    public static int uses(Hero hero, int skill) {
        if (hero == null || !validSkill(skill)) return 0;
        CowCraft craft = hero.buff(CowCraft.class);
        return craft == null ? 0 : craft.uses[skill];
    }

    public static int rank(Hero hero, int skill) {
        if (hero == null || hero.heroClass != HeroClass.COW || !validSkill(skill)) return 0;
        int count = uses(hero, skill);
        return count >= 12 ? 3 : count >= 7 ? 2 : count >= 3 ? 1 : 0;
    }

    /** Each effective activation earns practice once, regardless of the number of targets. */
    public static void record(Hero hero, int skill, boolean effective) {
        if (!effective || hero == null || !hero.isAlive() || hero.heroClass != HeroClass.COW
                || !validSkill(skill)) return;
        CowCraft craft = Buff.affect(hero, CowCraft.class);
        int oldRank = rank(hero, skill);
        craft.uses[skill] = Math.min(12, craft.uses[skill] + 1);
        CowLegend.recordArmorAbility(hero);
        if (rank(hero, skill) > oldRank && hero.sprite != null) {
            GLog.p(Messages.get(CowCraft.class, "practiced", Messages.get(CowCraft.class, "skill_" + skill)));
        }
        CowJournal.onPractice(hero, skill, craft.uses[skill]);
    }

    public static boolean hasVisibleEnemy(Hero hero) {
        if (hero == null || Dungeon.level == null || hero.fieldOfView == null) return false;
        for (Char enemy : Actor.chars()) {
            if (enemy.alignment == Char.Alignment.ENEMY && enemy.isAlive()
                    && Dungeon.level.insideMap(enemy.pos) && hero.fieldOfView[enemy.pos]) return true;
        }
        return false;
    }

    public static String progress(Hero hero, int skill) {
        return Messages.get(CowCraft.class, "progress", uses(hero, skill), rank(hero, skill));
    }

    @Override public String heroMessage() { return null; }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        for (int i = 0; i < uses.length; i++) bundle.put("cow_practice_" + i, uses[i]);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        for (int i = 0; i < uses.length; i++) uses[i] = Math.max(0, Math.min(12, bundle.getInt("cow_practice_" + i)));
    }
}
