/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeadowCake;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** The cow's persistent, finite combat resource. Waiting never generates charge. */
public class Stubbornness extends Buff {

    private int charge = 3;
    private int resonantHits;
    private int recoveryCooldown;
    private int grassSteps;
    private int successfulHits;
    private boolean greeted, meadowFound, convergenceFound, legendMigrated;

    {
        type = buffType.POSITIVE;
        revivePersists = true;
    }

    public static int capacity(Hero hero) {
        return 6 + hero.pointsInTalent(Talent.COW_STUBBORN_SPARK)
                + hero.pointsInTalent(Talent.COW_STAMINA)
                + CowJournal.collectionBonus(hero);
    }

    public int charge() { return charge; }

    /** The migration flag lives with its source counter so a load never credits old hits twice. */
    void migrateLegend(CowLegend legend) {
        if (!legendMigrated) {
            legendMigrated = true;
            legend.importPractice(successfulHits);
        }
    }

    public void gain(int amount) {
        if (!(target instanceof Hero) || amount <= 0) return;
        charge = Math.min(capacity((Hero) target), charge + Math.min(amount, capacity((Hero) target)));
        Item.updateQuickslot();
        BuffIndicator.refreshHero();
    }

    public boolean spendCharge(int amount) {
        if (amount < 0 || charge < amount) return false;
        charge -= amount;
        Item.updateQuickslot();
        BuffIndicator.refreshHero();
        return true;
    }

    public static int onHit(Hero hero, Char enemy, int damage) {
        if (hero.heroClass != HeroClass.COW || enemy == null
                || enemy.alignment != Char.Alignment.ENEMY || damage <= 0) return damage;
        Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
        // Test the reserve before this hit earns new charge.
        if (stubborn.charge >= (capacity(hero) + 1)/2) {
            damage += hero.pointsInTalent(Talent.COW_RESOLVE);
        }
        CowRush rush = hero.buff(CowRush.class);
        if (rush != null) damage += rush.consume(hero);
        NightShift night = hero.buff(NightShift.class);
        if (night != null) damage += 1 + hero.pointsInTalent(Talent.COW_SOLAR_FLARE);
        // Import an older hit counter before this attack changes it. Actual new practice
        // is credited by Hero.attack only after damage has really reduced HP or shielding.
        CowLegend.get(hero);
        damage = CowLegend.unloadBlame(hero, enemy, damage);
        stubborn.gain(1);
        int resonance = hero.pointsInTalent(Talent.COW_BELL_RESONANCE);
        if (resonance > 0 && ++stubborn.resonantHits >= 4 - resonance) {
            stubborn.resonantHits = 0;
            stubborn.gain(1);
        }
        if (++stubborn.successfulHits >= 64 && !stubborn.convergenceFound) {
            stubborn.convergenceFound = true;
            if (hero.sprite != null) GLog.p(Messages.get(Stubbornness.class, "convergence"));
            Buff.affect(hero, CowGuard.class).set(6, 6);
        }
        return damage;
    }

    public static int defend(Hero hero, Char enemy, int damage) {
        if (hero.heroClass != HeroClass.COW || enemy == null
                || enemy.alignment != Char.Alignment.ENEMY || damage <= 0) return damage;
        return (int)Math.ceil(damage * (1f - 0.05f*hero.pointsInTalent(Talent.COW_THICK_HIDE)));
    }

    public static void onDamage(Hero hero, Object source, int actualDamage) {
        // Hunger, poison, reflected damage and friendly attacks cannot farm charge.
        if (hero.heroClass == HeroClass.COW && hero.isAlive() && actualDamage > 0
                && source instanceof Char && ((Char)source).alignment == Char.Alignment.ENEMY) {
            Buff.affect(hero, Stubbornness.class).gain(1);
            CowLegend.recordDamage(hero, source, actualDamage);
        }
    }

    public static void afterBell(Hero hero) {
        Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
        if (hero.subClass == HeroSubClass.BULLDOZER) {
            int shield = 2*hero.pointsInTalent(Talent.COW_UNSTOPPABLE);
            if (shield > 0) Buff.affect(hero, CowGuard.class).set(shield, 5);
        } else if (hero.subClass == HeroSubClass.HERDGUARD) {
            int watch = hero.pointsInTalent(Talent.COW_PATIENT_WATCH);
            Buff.affect(hero, CowGuard.class).set(3 + hero.lvl/3 + 2*watch, 6 + 2*watch);
            int shelter = hero.pointsInTalent(Talent.COW_SHARED_SHELTER);
            if (shelter > 0) {
                for (Char ally : com.shatteredpixel.shatteredpixeldungeon.actors.Actor.chars()) {
                    if (ally != hero && ally.alignment == Char.Alignment.ALLY
                            && Dungeon.level.distance(hero.pos, ally.pos) <= 2) {
                        Buff.affect(ally, CowGuard.class).set(2*shelter, 6 + 2*watch);
                    }
                }
            }
            int recovery = hero.pointsInTalent(Talent.COW_GENTLE_RECOVERY);
            if (recovery > 0 && stubborn.recoveryCooldown == 0 && hero.HP < hero.HT) {
                Buff.affect(hero, Rumination.class).setHeal(recovery);
                stubborn.recoveryCooldown = 12;
            }
        }
    }

    public static void onStep(Hero hero, boolean wasGrass) {
        if (hero.heroClass != HeroClass.COW || !wasGrass || hero.flying) return;
        Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
        if (!stubborn.meadowFound && ++stubborn.grassSteps >= 8) {
            stubborn.meadowFound = true;
            if (hero.sprite != null) GLog.p(Messages.get(Stubbornness.class, "meadow"));
            MeadowCake cake = new MeadowCake();
            if (!cake.collect()) Dungeon.level.drop(cake, hero.pos).sprite.drop();
        }
    }

    @Override public boolean act() {
        if (!(target instanceof Hero)) { detach(); return true; }
        Hero hero = (Hero)target;
        CowLegend.get(hero);
        charge = Math.min(charge, capacity(hero));
        if (recoveryCooldown > 0) recoveryCooldown--;
        if (!greeted && hero.sprite != null && Dungeon.depth > 0) {
            greeted = true;
            GLog.p(Messages.get(this, "welcome"));
        }
        spend(TICK);
        return true;
    }

    @Override public int icon() { return BuffIndicator.FURY; }
    @Override public void tintIcon(Image icon) { icon.hardlight(0.92f, 0.73f, 0.40f); }
    @Override public String iconTextDisplay() { return Integer.toString(charge); }
    @Override public String desc() {
        int cap = target instanceof Hero ? capacity((Hero)target) : 6;
        return Messages.get(this, "desc", charge, cap);
    }

    @Override public void storeInBundle(Bundle b) {
        super.storeInBundle(b);
        b.put("cow_charge", charge);
        b.put("cow_resonance", resonantHits);
        b.put("cow_recovery", recoveryCooldown);
        b.put("cow_grass", grassSteps);
        b.put("cow_hits", successfulHits);
        b.put("cow_greeted", greeted);
        b.put("cow_meadow", meadowFound);
        b.put("cow_convergence", convergenceFound);
        b.put("cow_legend_migrated", legendMigrated);
    }

    @Override public void restoreFromBundle(Bundle b) {
        super.restoreFromBundle(b);
        charge = Math.max(0, Math.min(11, b.getInt("cow_charge")));
        resonantHits = Math.max(0, b.getInt("cow_resonance"));
        recoveryCooldown = Math.max(0, b.getInt("cow_recovery"));
        grassSteps = b.getInt("cow_grass");
        successfulHits = b.getInt("cow_hits");
        greeted = b.getBoolean("cow_greeted");
        meadowFound = b.getBoolean("cow_meadow");
        convergenceFound = b.getBoolean("cow_convergence");
        legendMigrated = b.getBoolean("cow_legend_migrated");
    }
}
