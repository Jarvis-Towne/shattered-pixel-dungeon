package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowRush;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NightShift;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PastureField;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Rumination;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.MidnightSun;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.PastureWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.Stampede;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.*;

/** Execute real resource, movement, combat, shield and save logic on a deterministic floor. */
public class CowMechanicsTest {
    private Hero hero;
    private Arena level;
    private Stubbornness stubborn;

    @BeforeClass public static void initialize() throws Exception { CowTestSupport.init(); }

    @Before public void reset() {
        hero = CowTestSupport.freshHero(HeroClass.COW);
        Dungeon.depth = 1;
        Dungeon.branch = 0;
        level = new Arena();
        Dungeon.level = level;
        hero.pos = level.cell(3, 4);
        hero.fieldOfView = level.heroFOV;
        Arrays.fill(hero.fieldOfView, true);
        Actor.add(hero);
        stubborn = hero.buff(Stubbornness.class);
    }

    @Test public void resourceRejectsNegativeInputsCapsHugeGainsAndNeverChargesByWaiting() {
        assertEquals(3, stubborn.charge());
        stubborn.gain(-9);
        assertFalse(stubborn.spendCharge(-1));
        assertFalse(stubborn.spendCharge(4));
        assertEquals(3, stubborn.charge());
        stubborn.gain(Integer.MAX_VALUE);
        assertEquals(6, stubborn.charge());
        hero.upgradeTalent(Talent.COW_STUBBORN_SPARK);
        stubborn.gain(2);
        assertEquals(7, stubborn.charge());
        assertTrue(stubborn.spendCharge(7));
        for (int i = 0; i < 20; i++) stubborn.act();
        assertEquals(0, stubborn.charge());
    }

    @Test public void chargeReserveAndOneTimeDiscoveriesSurviveActualCompressedSave() throws Exception {
        stubborn.gain(2);
        Bundle state = new Bundle();
        stubborn.storeInBundle(state);
        state.put("cow_meadow", true);
        state.put("cow_convergence", true);
        Stubbornness restored = new Stubbornness();
        restored.restoreFromBundle(CowTestSupport.roundTrip(state));
        Bundle after = new Bundle();
        restored.storeInBundle(after);
        assertEquals(5, restored.charge());
        assertTrue(after.getBoolean("cow_meadow"));
        assertTrue(after.getBoolean("cow_convergence"));
    }

    @Test public void onlyRealHostileCombatBuildsResolve() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        Dummy ally = addDummy(3, 5, Char.Alignment.ALLY);
        assertTrue(stubborn.spendCharge(3));
        Stubbornness.onDamage(hero, new Hunger(), 3);
        Stubbornness.onDamage(hero, ally, 3);
        Stubbornness.onDamage(hero, enemy, 0);
        assertEquals(0, stubborn.charge());
        Stubbornness.onDamage(hero, enemy, 2);
        assertEquals(1, stubborn.charge());
        Stubbornness.onHit(hero, ally, 4);
        Stubbornness.onHit(hero, enemy, 0);
        assertEquals(1, stubborn.charge());
        Stubbornness.onHit(hero, enemy, 4);
        assertEquals(2, stubborn.charge());
    }

    @Test public void invalidTargetsWallsPitsAndAlliesNeverSpendChargeOrTime() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        int start = hero.pos;
        float time = hero.cooldown();
        assertFalse(CowBell.charge(hero, hero.pos));
        assertFalse(CowBell.charge(hero, level.cell(4, 3)));
        assertFalse(CowBell.charge(hero, -1));
        level.terrain(4, 4, Terrain.WALL);
        assertFalse(CowBell.charge(hero, enemy.pos));
        level.terrain(4, 4, Terrain.CHASM);
        assertFalse(CowBell.charge(hero, enemy.pos));
        level.terrain(4, 4, Terrain.EMPTY);
        Dummy ally = addDummy(4, 4, Char.Alignment.ALLY);
        assertFalse(CowBell.charge(hero, enemy.pos));
        assertFalse(CowBell.charge(hero, ally.pos));
        assertEquals(start, hero.pos);
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(3, stubborn.charge());
        assertEquals(100, enemy.HP);
    }

    @Test public void hiddenDistantAndRootedTargetsCannotBeCharged() {
        Dummy enemy = addDummy(7, 4, Char.Alignment.ENEMY);
        assertFalse(CowBell.charge(hero, enemy.pos));
        enemy.pos = level.cell(6, 4);
        hero.fieldOfView[enemy.pos] = false;
        assertFalse(CowBell.charge(hero, enemy.pos));
        hero.fieldOfView[enemy.pos] = true;
        Buff.prolong(hero, Roots.class, 3);
        assertFalse(CowBell.charge(hero, enemy.pos));
        assertEquals(3, stubborn.charge());
    }

    @Test public void nonCowAndParalyzedHeroesCannotUseBellStaticEntrypoints() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        hero.heroClass = HeroClass.WARRIOR;
        assertFalse(CowBell.charge(hero, enemy.pos));
        assertFalse(CowBell.stomp(hero));
        hero.heroClass = HeroClass.COW;
        hero.paralysed = 1;
        assertFalse(CowBell.charge(hero, enemy.pos));
        assertFalse(CowBell.stomp(hero));
        assertEquals(3, stubborn.charge());
        assertEquals(100, enemy.HP);
    }

    @Test public void validChargeMovesAdjacentPressesEveryTileDealsDamageAndSpendsOneTurn() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        float time = hero.cooldown();
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertEquals(level.cell(5, 4), hero.pos);
        assertEquals(Arrays.asList(level.cell(4, 4), level.cell(5, 4)), level.pressed);
        assertEquals(100 - CowBell.impactDamage(hero, 2), enemy.HP);
        assertNotNull(enemy.buff(Cripple.class));
        assertEquals(0, stubborn.charge());
        assertEquals(time + 1, hero.cooldown(), 0f);
        assertFalse(hero.ready);
        assertFalse(CowBell.charge(hero, enemy.pos));
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    @Test public void terrainControlInterruptsChargeWithoutHittingDistantEnemy() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        level.rootAt = level.cell(4, 4);
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertEquals(level.rootAt, hero.pos);
        assertTrue(hero.rooted);
        assertEquals(100, enemy.HP);
        assertEquals(0, stubborn.charge());
        assertEquals(1, level.pressed.size());
    }

    @Test public void newlySummonedOccupantStopsChargeBeforeActorOverlap() {
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        level.summonAt = level.cell(4, 4);
        level.summonDestination = level.cell(5, 4);
        float time = hero.cooldown();
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertEquals(level.summonAt, hero.pos);
        assertNotNull(Actor.findChar(level.summonDestination));
        assertNotSame(hero, Actor.findChar(level.summonDestination));
        assertEquals(1, level.pressed.size());
        assertEquals(100, enemy.HP);
        assertEquals(0, stubborn.charge());
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    @Test public void stompDamagesEnemiesProtectsAlliesConsumesChargeAndClearsTalentedRoots() {
        Dummy enemy = addDummy(4, 4, Char.Alignment.ENEMY);
        Dummy ally = addDummy(3, 5, Char.Alignment.ALLY);
        hero.upgradeTalent(Talent.COW_STEADY_HOOVES);
        hero.upgradeTalent(Talent.COW_STEADY_HOOVES);
        Buff.prolong(hero, Roots.class, 3);
        Buff.prolong(hero, Cripple.class, 3);
        float time = hero.cooldown();
        assertTrue(CowBell.stomp(hero));
        assertTrue(enemy.HP < 100);
        assertNotNull(enemy.buff(Cripple.class));
        assertEquals(100, ally.HP);
        assertNotNull(hero.buff(CowGuard.class));
        assertFalse(hero.rooted);
        assertNull(hero.buff(Cripple.class));
        assertEquals(0, stubborn.charge());
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    @Test public void bulldozerGetsOneConsumableFollowUpAndHerdguardGetsShelter() {
        hero.subClass = HeroSubClass.BULLDOZER;
        Talent.initSubclassTalents(hero);
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertNotNull(hero.buff(CowRush.class));
        assertTrue(Stubbornness.onHit(hero, enemy, 3) > 3);
        assertNull(hero.buff(CowRush.class));
        hero.subClass = HeroSubClass.HERDGUARD;
        Talent.initSubclassTalents(hero);
        hero.upgradeTalent(Talent.COW_SHARED_SHELTER);
        Dummy ally = addDummy(5, 5, Char.Alignment.ALLY);
        stubborn.gain(3);
        assertTrue(CowBell.stomp(hero));
        assertNotNull(hero.buff(CowGuard.class));
        assertNotNull(ally.buff(CowGuard.class));
        assertEquals(2, ally.buff(CowGuard.class).shielding());
    }

    @Test public void guardDoesNotStackAndItsRemainingLifetimeSurvivesSave() throws Exception {
        CowGuard guard = Buff.affect(hero, CowGuard.class);
        guard.set(7, 5);
        guard.set(4, 3);
        assertEquals(7, guard.shielding());
        assertEquals(0, guard.absorbDamage(3));
        assertEquals(4, guard.shielding());
        Bundle state = new Bundle();
        guard.storeInBundle(state);
        CowGuard restored = new CowGuard();
        restored.restoreFromBundle(CowTestSupport.roundTrip(state));
        guard.detach();
        restored.attachTo(hero);
        for (int i = 0; i < 4; i++) restored.act();
        assertSame(restored, hero.buff(CowGuard.class));
        restored.act();
        assertNull(hero.buff(CowGuard.class));
    }

    @Test public void ruminationIsFiniteNonstackingAndRespectsPharmacophobia() {
        hero.HP = 10;
        Rumination healing = Buff.affect(hero, Rumination.class);
        healing.setHeal(5);
        healing.setHeal(3);
        assertEquals(5, healing.remaining());
        for (int i = 0; i < 5; i++) healing.act();
        assertEquals(15, hero.HP);
        assertNull(hero.buff(Rumination.class));
        Dungeon.challenges = Challenges.NO_HEALING;
        healing = Buff.affect(hero, Rumination.class);
        healing.setHeal(8);
        healing.act();
        assertEquals(15, hero.HP);
        assertNull(hero.buff(Rumination.class));
        Dungeon.challenges = 0;
    }

    @Test public void stampedePiercesEnemiesStopsAtWallsAndDebitsRealArmorCharge() {
        hero.lvl = 20;
        Dummy near = addDummy(4, 4, Char.Alignment.ENEMY);
        Dummy far = addDummy(6, 4, Char.Alignment.ENEMY);
        Dummy blocked = addDummy(8, 4, Char.Alignment.ENEMY);
        level.terrain(7, 4, Terrain.WALL);
        ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
        armor.charge = 100;
        float time = hero.cooldown();
        new TestStampede().cast(armor, hero, near.pos);
        assertTrue(near.HP < 100);
        assertTrue(far.HP < 100);
        assertEquals(100, blocked.HP);
        assertEquals(65, armor.charge, 0f);
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    @Test public void pastureAnchorsAtCastPositionRespectsWallsAndExpiresAcrossFloors() {
        ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
        armor.charge = 100;
        new TestPastureWard().cast(armor, hero);
        PastureField field = hero.buff(PastureField.class);
        assertNotNull(field);
        assertTrue(field.contains(hero.pos));
        assertEquals(65, armor.charge, 0f);
        Dummy enemy = addDummy(4, 4, Char.Alignment.ENEMY);
        field.act();
        assertNotNull(enemy.buff(Cripple.class));
        level.terrain(4, 4, Terrain.WALL);
        assertFalse(field.contains(level.cell(5, 4)));
        int original = hero.pos;
        hero.pos = level.cell(8, 4);
        assertTrue(field.contains(original));
        assertFalse(field.contains(hero.pos));
        Dungeon.depth++;
        field.act();
        assertNull(hero.buff(PastureField.class));
    }

    @Test public void midnightShiftSpendsArmorChargeAddsFiniteResolveAndDamageBuff() {
        ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
        armor.charge = 100;
        float time = hero.cooldown();
        new TestMidnightSun().cast(armor, hero);
        assertEquals(60, armor.charge, 0f);
        assertEquals(5, stubborn.charge());
        assertNotNull(hero.buff(NightShift.class));
        Dummy enemy = addDummy(4, 4, Char.Alignment.ENEMY);
        assertEquals(6, Stubbornness.onHit(hero, enemy, 5));
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    private Dummy addDummy(int x, int y, Char.Alignment alignment) {
        Dummy dummy = new Dummy();
        dummy.alignment = alignment;
        dummy.HP = dummy.HT = 100;
        dummy.pos = level.cell(x, y);
        Actor.add(dummy);
        return dummy;
    }

    public static class Dummy extends Char { @Override protected boolean act() { return false; } }
    private static class TestStampede extends Stampede { void cast(ClassArmor armor, Hero hero, int target) { activate(armor, hero, target); } }
    private static class TestPastureWard extends PastureWard { void cast(ClassArmor armor, Hero hero) { activate(armor, hero, hero.pos); } }
    private static class TestMidnightSun extends MidnightSun { void cast(ClassArmor armor, Hero hero) { activate(armor, hero, hero.pos); } }

    private static class Arena extends Level {
        final ArrayList<Integer> pressed = new ArrayList<>();
        int rootAt = -1;
        int summonAt = -1, summonDestination = -1;
        Arena() {
            setSize(13, 10);
            transitions = new ArrayList<>(); mobs = new HashSet<>(); heaps = new SparseArray<>();
            blobs = new HashMap<>(); plants = new SparseArray<>(); traps = new SparseArray<>();
            customTiles = new ArrayList<>(); customTerrain = new ArrayList<>(); customWalls = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) map[cell(x, y)] = Terrain.EMPTY;
            discoverable = new boolean[length]; Arrays.fill(discoverable, true);
            buildFlagMaps();
        }
        int cell(int x, int y) { return y*width + x; }
        void terrain(int x, int y, int tile) { map[cell(x, y)] = tile; buildFlagMaps(); }
        @Override public void occupyCell(Char ch) {
            pressed.add(ch.pos);
            super.occupyCell(ch);
            if (ch.pos == rootAt) Buff.prolong(ch, Roots.class, 3);
            if (ch.pos == summonAt && Actor.findChar(summonDestination) == null) {
                Dummy summoned = new Dummy();
                summoned.alignment = Char.Alignment.ENEMY;
                summoned.HP = summoned.HT = 100;
                summoned.pos = summonDestination;
                Actor.add(summoned);
            }
        }
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
        @Override public String tilesTex() { return Assets.Environment.TILES_SEWERS; }
        @Override public String waterTex() { return Assets.Environment.WATER_SEWERS; }
    }
}
