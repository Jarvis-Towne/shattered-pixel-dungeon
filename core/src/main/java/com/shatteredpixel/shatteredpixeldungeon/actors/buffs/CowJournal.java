/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeadowCake;
import com.shatteredpixel.shatteredpixeldungeon.items.food.WarmMilk;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/** A finite collection of one-time discoveries. It has no repeating reward tick. */
public class CowJournal extends Buff {
    public static final int TOTAL = 18;
    private static final int ALL_ENTRIES = (1 << TOTAL) - 1;

    private int discoveries;
    private int waterSteps;
    private int doorSteps;
    private int meals;
    private int hits;

    { revivePersists = true; }

    private static boolean valid(Hero hero) {
        return hero != null && hero.heroClass == HeroClass.COW && hero.isAlive();
    }

    /** Event handlers may create the journal; displaying an album must not. */
    public static CowJournal get(Hero hero) {
        return valid(hero) ? Buff.affect(hero, CowJournal.class) : null;
    }

    public boolean found(int entry) {
        return entry >= 0 && entry < TOTAL && (discoveries & (1 << entry)) != 0;
    }

    public int count() { return Integer.bitCount(discoveries); }

    public boolean discover(Hero hero, int entry) {
        if (!valid(hero) || entry < 0 || entry >= TOTAL || found(entry)) return false;
        discoveries |= 1 << entry;
        if (hero.sprite != null) GLog.p(Messages.get(CowJournal.class, "entry_" + entry));
        if (entry == 3 || entry == 17) Buff.affect(hero, CowGuard.class).set(2, 3);
        return true;
    }

    public static void onMove(Hero hero, int terrain) {
        CowJournal journal = get(hero);
        if (journal == null) return;
        if (terrain == Terrain.WATER) {
            journal.waterSteps = Math.min(6, journal.waterSteps + 1);
            if (journal.waterSteps == 6) journal.discover(hero, 0);
        }
        if (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR) {
            journal.doorSteps = Math.min(6, journal.doorSteps + 1);
            if (journal.doorSteps == 6) journal.discover(hero, 1);
        }
        if (Dungeon.depth >= 6) journal.discover(hero, 11);
        if (Dungeon.depth >= 16) journal.discover(hero, 12);
        if (Dungeon.depth > 1 && hero.belongings.armor == null) journal.discover(hero, 16);
    }

    public static void onFood(Hero hero, Food food) {
        CowJournal journal = get(hero);
        if (journal == null) return;
        if (food instanceof WarmMilk) journal.discover(hero, 2);
        if (food instanceof MeadowCake) journal.discover(hero, 3);
        if (food.getClass().getSimpleName().equals("FrozenCarpaccio")) journal.discover(hero, 4);
        journal.meals = Math.min(5, journal.meals + 1);
        if (journal.meals == 5) journal.discover(hero, 5);
    }

    /** Called after damage resolution, so killing blows still count. */
    public static void onHit(Hero hero, Char enemy, int actualDamage) {
        if (actualDamage <= 0 || enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;
        CowJournal journal = get(hero);
        if (journal == null) return;
        String kind = enemy.getClass().getSimpleName();
        if (kind.equals("Crab")) journal.discover(hero, 6);
        if (kind.equals("Skeleton")) journal.discover(hero, 7);
        if (kind.equals("Mimic") || kind.equals("GoldenMimic") || kind.equals("EbonyMimic")) {
            journal.discover(hero, 8);
        }
        if (kind.equals("DM100") || kind.equals("DM200")) journal.discover(hero, 9);
        journal.hits = Math.min(100, journal.hits + 1);
        if (journal.hits == 100) journal.discover(hero, 10);
    }

    public static void onPractice(Hero hero, int skill, int uses) {
        CowJournal journal = get(hero);
        if (journal == null || uses != 3 || skill < 0 || skill >= 3) return;
        journal.discover(hero, 13 + skill);
    }

    public static void onDamage(Hero hero, Object source, int actualDamage) {
        if (!valid(hero) || hero.HP != 1 || actualDamage <= 0 || !(source instanceof Char)
                || ((Char) source).alignment != Char.Alignment.ENEMY) return;
        get(hero).discover(hero, 17);
    }

    public static String album(Hero hero) {
        CowJournal journal = hero == null ? null : hero.buff(CowJournal.class);
        int count = journal == null ? 0 : journal.count();
        StringBuilder text = new StringBuilder(Messages.get(CowJournal.class, "album", count, TOTAL));
        for (int entry = 0; entry < TOTAL; entry++) {
            if (journal != null && journal.found(entry)) {
                text.append("\n\n").append(Messages.get(CowJournal.class, "entry_" + entry));
            }
        }
        return text.toString();
    }

    @Override public String heroMessage() { return null; }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("cow_discoveries", discoveries);
        bundle.put("cow_water", waterSteps);
        bundle.put("cow_doors", doorSteps);
        bundle.put("cow_meals", meals);
        bundle.put("cow_journal_hits", hits);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        discoveries = bundle.getInt("cow_discoveries") & ALL_ENTRIES;
        waterSteps = Math.max(0, Math.min(6, bundle.getInt("cow_water")));
        doorSteps = Math.max(0, Math.min(6, bundle.getInt("cow_doors")));
        meals = Math.max(0, Math.min(5, bundle.getInt("cow_meals")));
        hits = Math.max(0, Math.min(100, bundle.getInt("cow_journal_hits")));
    }
}
