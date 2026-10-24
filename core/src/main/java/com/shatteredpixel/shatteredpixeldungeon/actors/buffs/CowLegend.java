/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** Shared, finite practice progress. Merely waiting or inspecting equipment never earns practice. */
public class CowLegend extends Buff {
    private static final int[] PRACTICE = {0, 12, 40, 90};
    private static final int[] LEVELS = {1, 3, 8, 14};
    public static final int MAX_POINTS = 999;

    private int points;
    private int announcedStage;
    private int blame;
    private int guardCooldown;
    private int quipCooldown;

    {
        type = buffType.POSITIVE;
        revivePersists = true;
    }

    /** The only creating accessor; callers must not use it merely to preview foreign equipment. */
    public static CowLegend get(Hero hero) {
        if (hero == null || hero.heroClass != HeroClass.COW) return null;
        CowLegend legend = Buff.affect(hero, CowLegend.class);
        Stubbornness stubborn = hero.buff(Stubbornness.class);
        if (stubborn != null) stubborn.migrateLegend(legend);
        return legend;
    }

    /** Read-only accessor, including during inventory rendering and stat calculation. */
    public static int stage(Hero hero) {
        if (hero == null || hero.heroClass != HeroClass.COW) return 0;
        CowLegend legend = hero.buff(CowLegend.class);
        return legend == null ? 0 : legend.stage();
    }

    public int points() { return points; }
    public int blame() { return blame; }
    public int guardCooldown() { return guardCooldown; }
    public void clearBlame() { blame = 0; BuffIndicator.refreshHero(); }

    public int stage() {
        if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.COW) return 0;
        Hero hero = (Hero) target;
        for (int stage = 3; stage > 0; stage--) {
            if (points >= PRACTICE[stage] && hero.lvl >= LEVELS[stage]) return stage;
        }
        return 0;
    }

    public void addPoints(int amount) {
        if (amount <= 0 || !(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.COW) return;
        points += Math.min(amount, MAX_POINTS - points);
        announceEvolution();
        Item.updateQuickslot();
        BuffIndicator.refreshHero();
    }

    /** Used once when an older Stubbornness save is first opened. No replayed rewards. */
    void importPractice(int oldHits) {
        points = Math.max(points, Math.min(MAX_POINTS, Math.max(0, oldHits)));
    }

    private static boolean realEnemy(Char enemy) {
        return enemy != null && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY;
    }

    public static void recordHit(Hero hero, Char enemy, int damage) {
        if (damage <= 0 || !realEnemy(enemy)) return;
        recordResolvedHit(hero, enemy, damage);
    }

    /** Called after a real attack resolves; a lethal hit is still a successful hit. */
    public static void recordResolvedHit(Hero hero, Char enemy, int actualDamage) {
        if (hero == null || hero.heroClass != HeroClass.COW || !hero.isAlive()
                || enemy == null || enemy.alignment != Char.Alignment.ENEMY || actualDamage <= 0) return;
        CowLegend legend = get(hero);
        if (legend != null) legend.addPoints(1);
    }

    public static void recordDamage(Hero hero, Object source, int actualDamage) {
        if (hero == null || !hero.isAlive() || actualDamage <= 0 || !(source instanceof Char)
                || ((Char) source).alignment != Char.Alignment.ENEMY) return;
        CowLegend legend = get(hero);
        if (legend == null) return;
        legend.addPoints(1);
        // A real hit may store a tiny, bounded compensation. Never reflects damage back.
        if (wearingApron(hero) && legend.stage() > 0) {
            legend.blame = Math.min(legend.blameCap(), legend.blame + 1);
        }
    }

    public static void recordSkill(Hero hero, int enemiesHit) {
        if (enemiesHit <= 0) return;
        CowLegend legend = get(hero);
        if (legend != null) legend.addPoints(2);
    }

    public static void recordArmorAbility(Hero hero) {
        CowLegend legend = get(hero);
        if (legend != null) legend.addPoints(3);
    }

    public static boolean wearingApron(Hero hero) {
        return hero != null && (hero.belongings.armor() instanceof CowhideArmor
                || hero.belongings.armor() instanceof CowArmor);
    }

    public int blameCap() { return stage() == 0 ? 0 : stage() + 1; }

    /** Only a living hostile target and the actually worn apron can spend the stored compensation. */
    public static int unloadBlame(Hero hero, Char enemy, int damage) {
        if (hero == null || damage <= 0 || !realEnemy(enemy) || !wearingApron(hero)) return damage;
        CowLegend legend = hero.buff(CowLegend.class);
        if (legend == null || legend.stage() == 0 || legend.blame <= 0) return damage;
        int used = Math.min(2, legend.blame);
        legend.blame -= used;
        if (used == 2 && legend.stage() >= 2 && legend.guardCooldown == 0) {
            Buff.affect(hero, CowGuard.class).set(legend.stage() - 1, 2);
            legend.guardCooldown = 8;
        }
        legend.quip("unload");
        BuffIndicator.refreshHero();
        return damage + used;
    }

    /** A shared speech cooldown prevents multi-target attacks from flooding the log. */
    public void quip(String key) {
        if (quipCooldown == 0 && target != null && target.sprite != null) {
            GLog.i(Messages.get(CowLegend.class, key));
            quipCooldown = 12;
        }
    }

    /** Keep the existing equipment objects; never reroll enchants or discard scroll upgrades. */
    private void refreshEquipment() {
        if (!(target instanceof Hero)) return;
        Hero hero = (Hero) target;
        int current = stage();
        if (hero.belongings.weapon instanceof OxHorn) {
            ((OxHorn)hero.belongings.weapon).evolveTo(current);
        }
        if (hero.belongings.armor instanceof CowhideArmor) {
            ((CowhideArmor)hero.belongings.armor).evolveTo(current);
        } else if (hero.belongings.armor instanceof CowArmor) {
            ((CowArmor)hero.belongings.armor).evolveTo(current);
        }
    }

    private void announceEvolution() {
        refreshEquipment();
        int current = stage();
        if (current > announcedStage) {
            if (target != null && target.sprite != null) {
                GLog.p(Messages.get(this, "evolved", Messages.get(this, "stage_" + current)));
            }
            announcedStage = current;
            Item.updateQuickslot();
            BuffIndicator.refreshHero();
        }
    }

    public String progress() {
        int current = stage();
        String name = Messages.get(this, "stage_" + current);
        if (current == 3) return Messages.get(this, "complete", name, points, MAX_POINTS);
        return Messages.get(this, "progress", name, points, PRACTICE[current + 1], LEVELS[current + 1]);
    }

    @Override public boolean act() {
        if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.COW) { detach(); return true; }
        if (!wearingApron((Hero) target)) blame = 0;
        blame = Math.min(blame, blameCap());
        if (guardCooldown > 0) guardCooldown--;
        if (quipCooldown > 0) quipCooldown--;
        announceEvolution();
        spend(TICK);
        return true;
    }

    @Override public int icon() { return BuffIndicator.FURY; }
    @Override public void tintIcon(Image icon) { icon.hardlight(0.65f, 0.85f, 1f); }
    @Override public String iconTextDisplay() { return Integer.toString(stage()); }
    @Override public String desc() {
        return progress() + "\n\n" + Messages.get(this, "desc", blame, blameCap(), guardCooldown);
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("cow_practice", points);
        bundle.put("cow_stage_announced", announcedStage);
        bundle.put("cow_blame", blame);
        bundle.put("cow_blame_guard_cd", guardCooldown);
        bundle.put("cow_quip_cd", quipCooldown);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        points = Math.max(0, Math.min(MAX_POINTS, bundle.getInt("cow_practice")));
        announcedStage = Math.max(0, Math.min(3, bundle.getInt("cow_stage_announced")));
        blame = Math.max(0, Math.min(4, bundle.getInt("cow_blame")));
        guardCooldown = Math.max(0, Math.min(8, bundle.getInt("cow_blame_guard_cd")));
        quipCooldown = Math.max(0, Math.min(12, bundle.getInt("cow_quip_cd")));
    }
}
