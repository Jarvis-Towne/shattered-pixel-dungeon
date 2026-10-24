package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/** Verifies progression gates, old-save migration and bounded equipment synergy on real game objects. */
public class CowGearEvolutionTest {
    private Hero hero;
    private Dummy enemy;
    private Dummy ally;

    @BeforeClass public static void initialize() throws Exception { CowTestSupport.init(); }

    @Before public void reset() {
        hero = CowTestSupport.freshHero(HeroClass.COW);
        enemy = new Dummy();
        ally = new Dummy();
        ally.alignment = Char.Alignment.ALLY;
    }

    @Test public void stagePreviewIsReadOnlyAndEveryEvolutionRequiresBothPracticeAndLevel() {
        assertNull(hero.buff(CowLegend.class));
        assertEquals(0, CowLegend.stage(hero));
        assertNull(hero.buff(CowLegend.class));
        CowLegend legend = CowLegend.get(hero);
        legend.addPoints(12);
        assertEquals(0, legend.stage());
        hero.lvl = 3;
        assertEquals(1, legend.stage());
        legend.addPoints(28);
        assertEquals(1, legend.stage());
        hero.lvl = 8;
        assertEquals(2, legend.stage());
        legend.addPoints(50);
        assertEquals(2, legend.stage());
        hero.lvl = 14;
        legend.act(); // real game refreshes equipment in the buff tick after leveling
        assertEquals(3, legend.stage());
        // Evolution now upgrades the actual equipment tier, not just its display name.
        assertEquals(4, ((OxHorn) hero.belongings.weapon).tier);
        assertEquals(4, hero.belongings.armor.tier);
        assertEquals(ItemSpriteSheet.WAR_HAMMER, hero.belongings.weapon.image());
        assertEquals(ItemSpriteSheet.COW_ARMOR, hero.belongings.armor.image());
        assertEquals(0, CowLegend.stage(null));
        hero.heroClass = HeroClass.WARRIOR;
        assertEquals(0, CowLegend.stage(hero));
        assertNull(CowLegend.get(hero));
    }

    @Test public void onlyRealCombatEarnsPracticeAndMultiTargetSkillsCreditOnce() {
        CowLegend legend = CowLegend.get(hero);
        CowLegend.recordHit(hero, ally, 4);
        CowLegend.recordHit(hero, enemy, 0);
        CowLegend.recordDamage(hero, new Hunger(), 5);
        CowLegend.recordDamage(hero, ally, 5);
        CowLegend.recordDamage(hero, enemy, 0);
        CowLegend.recordSkill(hero, 0);
        for (int i = 0; i < 30; i++) legend.act();
        assertEquals(0, legend.points());
        CowLegend.recordHit(hero, enemy, 4);
        CowLegend.recordDamage(hero, enemy, 1);
        CowLegend.recordSkill(hero, 9);
        assertEquals(4, legend.points());
        CowLegend.recordArmorAbility(hero);
        assertEquals(7, legend.points());
        enemy.HP = 0;
        CowLegend.recordHit(hero, enemy, 8);
        assertEquals(7, legend.points());
        legend.addPoints(-1);
        legend.addPoints(Integer.MAX_VALUE);
        assertEquals(CowLegend.MAX_POINTS, legend.points());
        legend.addPoints(Integer.MAX_VALUE);
        assertEquals(CowLegend.MAX_POINTS, legend.points());
    }

    @Test public void legacySuccessfulHitsMigrateOnceAcrossActualCompressedHeroSave() throws Exception {
        hero.lvl = 8;
        Stubbornness stubborn = hero.buff(Stubbornness.class);
        Bundle old = new Bundle();
        stubborn.storeInBundle(old);
        old.put("cow_hits", 37);
        old.put("cow_legend_migrated", false);
        stubborn.restoreFromBundle(CowTestSupport.roundTrip(old));
        CowLegend legend = CowLegend.get(hero);
        assertEquals(37, legend.points());
        assertEquals(1, legend.stage());
        Stubbornness.onHit(hero, enemy, 4);
        CowLegend.recordResolvedHit(hero, enemy, 4);
        assertEquals(38, legend.points());
        Bundle save = new Bundle();
        save.put("hero", hero);
        Hero restored = (Hero) CowTestSupport.roundTrip(save).get("hero");
        Dungeon.hero = restored;
        assertEquals(38, CowLegend.get(restored).points());
        for (int i = 0; i < 10; i++) {
            restored.buff(Stubbornness.class).act();
            CowLegend.get(restored);
        }
        assertEquals(38, restored.buff(CowLegend.class).points());
    }

    @Test public void resolvedDamageCreditsLethalHitsButRejectsZeroActualDamageAndFriendlies() {
        CowLegend legend = CowLegend.get(hero);
        CowLegend.recordResolvedHit(hero, enemy, 0);
        CowLegend.recordResolvedHit(hero, enemy, -1);
        CowLegend.recordResolvedHit(hero, ally, 7);
        assertEquals(0, legend.points());
        enemy.HP = 0;
        CowLegend.recordHit(hero, enemy, 7);
        assertEquals(0, legend.points());
        CowLegend.recordResolvedHit(hero, enemy, 7);
        assertEquals(1, legend.points());
        hero.HP = 0;
        CowLegend.recordResolvedHit(hero, enemy, 7);
        assertEquals(1, legend.points());
    }

    @Test public void firstAttackInitializesLegacyMigrationBeforeIncrementingItsCounter() {
        assertNull(hero.buff(CowLegend.class));
        Stubbornness.onHit(hero, enemy, 4);
        assertEquals(0, CowLegend.get(hero).points());
        CowLegend.recordResolvedHit(hero, enemy, 4);
        assertEquals(1, CowLegend.get(hero).points());
        Stubbornness.onHit(hero, enemy, 4);
        // A positive pre-damage roll can still be absorbed by a later effect.
        CowLegend.recordResolvedHit(hero, enemy, 0);
        assertEquals(1, CowLegend.get(hero).points());
    }

    @Test public void apronStoresOnlyBoundedRealDamageAndCannotBeUsedFromBackpack() {
        CowLegend legend = mature();
        for (int i = 0; i < 20; i++) CowLegend.recordDamage(hero, enemy, 1);
        assertEquals(4, legend.blame());
        CowLegend.recordDamage(hero, new Hunger(), 10);
        assertEquals(4, legend.blame());
        assertEquals(5, CowLegend.unloadBlame(hero, ally, 5));
        assertEquals(0, CowLegend.unloadBlame(hero, enemy, 0));
        assertEquals(4, legend.blame());
        hero.belongings.armor = new ClothArmor();
        assertEquals(5, CowLegend.unloadBlame(hero, enemy, 5));
        CowLegend.recordDamage(hero, enemy, 1);
        assertEquals(4, legend.blame());
        legend.act();
        assertEquals(0, legend.blame());
        assertEquals(5, CowhideArmor.apronDefense(enemy, hero, 5));
    }

    @Test public void compensationShieldHasRealCooldownAndCannotStackByRapidHits() {
        CowLegend legend = mature();
        for (int i = 0; i < 4; i++) CowLegend.recordDamage(hero, enemy, 1);
        assertEquals(7, CowLegend.unloadBlame(hero, enemy, 5));
        assertEquals(2, legend.blame());
        CowGuard guard = hero.buff(CowGuard.class);
        assertNotNull(guard);
        assertEquals(2, guard.shielding());
        assertEquals(8, legend.guardCooldown());
        guard.absorbDamage(1);
        assertEquals(7, CowLegend.unloadBlame(hero, enemy, 5));
        assertEquals(1, guard.shielding());
        assertEquals(0, legend.blame());
        for (int i = 0; i < 8; i++) legend.act();
        CowLegend.recordDamage(hero, enemy, 1);
        CowLegend.recordDamage(hero, enemy, 1);
        CowLegend.unloadBlame(hero, enemy, 5);
        assertEquals(2, hero.buff(CowGuard.class).shielding());
        assertEquals(8, legend.guardCooldown());
    }

    @Test public void thirdHornHitPersistsAndInvalidHitsNeverAdvanceIt() throws Exception {
        mature();
        OxHorn horn = (OxHorn) hero.belongings.weapon;
        assertEquals(5, horn.min(0));
        assertEquals(20, horn.max(0));
        int ordinary = horn.proc(hero, enemy, 5);
        assertEquals(ordinary, horn.proc(hero, enemy, 5));
        assertEquals(2, horn.argumentHits());
        horn.proc(hero, ally, 5);
        horn.proc(hero, enemy, 0);
        assertEquals(2, horn.argumentHits());
        Bundle save = new Bundle();
        save.put("horn", horn);
        OxHorn restored = (OxHorn) CowTestSupport.roundTrip(save).get("horn");
        hero.belongings.weapon = restored;
        assertEquals(ordinary + 8, restored.proc(hero, enemy, 5));
        assertEquals(0, restored.argumentHits());
        assertNotNull(enemy.buff(Cripple.class));
        assertNotNull(enemy.buff(Vulnerable.class));
        enemy.HP = 0;
        restored.proc(hero, enemy, 5);
        assertEquals(0, restored.argumentHits());
    }

    @Test public void crownPreservesSourceArmorStatsAndInheritsApronCompensation() {
        CowLegend legend = mature();
        hero.belongings.armor.upgrade(2);
        int tier = hero.belongings.armor.tier;
        ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
        assertTrue(armor instanceof CowArmor);
        assertEquals(tier, armor.tier);
        assertEquals(2, armor.trueLevel());
        hero.belongings.armor = armor;
        assertEquals(4, CowhideArmor.apronDefense(enemy, hero, 5));
        CowLegend.recordDamage(hero, enemy, 2);
        assertEquals(1, legend.blame());
        assertEquals(6, CowLegend.unloadBlame(hero, enemy, 5));
        assertEquals(0, legend.blame());
    }

    @Test public void fullSaveKeepsPracticeWeaponRhythmCompensationAndCooldown() throws Exception {
        CowLegend legend = mature();
        for (int i = 0; i < 4; i++) CowLegend.recordDamage(hero, enemy, 1);
        CowLegend.unloadBlame(hero, enemy, 5);
        OxHorn horn = (OxHorn) hero.belongings.weapon;
        horn.upgrade(2);
        horn.proc(hero, enemy, 5);
        Bundle save = new Bundle();
        save.put("hero", hero);
        Hero restored = (Hero) CowTestSupport.roundTrip(save).get("hero");
        Dungeon.hero = restored;
        CowLegend after = CowLegend.get(restored);
        assertEquals(94, after.points());
        assertEquals(3, after.stage());
        assertEquals(2, after.blame());
        assertEquals(8, after.guardCooldown());
        assertEquals(1, ((OxHorn) restored.belongings.weapon).argumentHits());
        assertEquals(2, restored.belongings.weapon.trueLevel());
        assertEquals(2, restored.buff(CowGuard.class).shielding());
    }

    private CowLegend mature() {
        hero.lvl = 14;
        CowLegend legend = CowLegend.get(hero);
        legend.addPoints(90);
        return legend;
    }

    public static class Dummy extends Char {
        { HP = HT = 1000; alignment = Alignment.ENEMY; }
        @Override protected boolean act() { return false; }
    }
}
