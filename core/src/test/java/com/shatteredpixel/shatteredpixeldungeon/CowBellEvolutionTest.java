package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowBellCombo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowBellMastery;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
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

/** Exercise evolved actions through actual damage, resolve, actor time and compressed saves. */
public class CowBellEvolutionTest {
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

    @Test public void menuRequiresBothStageGatesAndGuideDoesNotCreateOrAdvanceProgress() {
        Buff.detach(hero, CowLegend.class);
        CowBell bell = new CowBell();
        float time = hero.cooldown();
        int charge = stubborn.charge();
        assertTrue(CowBell.guide(hero).contains("成长"));
        assertNull(hero.buff(CowLegend.class));
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(charge, stubborn.charge());
        assertTrue(bell.actions(hero).contains(CowBell.AC_GUIDE));
        assertTrue(bell.actions(hero).contains(CowBell.AC_ALBUM));
        assertFalse(bell.actions(hero).contains(CowBell.AC_DEBATE));

        CowLegend legend = CowLegend.get(hero);
        legend.addPoints(90);
        hero.lvl = 13;
        assertEquals(2, CowLegend.stage(hero));
        assertFalse(bell.actions(hero).contains(CowBell.AC_DEBATE));
        hero.lvl = 14;
        assertTrue(bell.actions(hero).contains(CowBell.AC_DEBATE));
        assertEquals(5, bell.actions(hero).size());
        assertTrue(CowBell.guide(hero).contains("90"));
        assertEquals(90, legend.points());
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(charge, stubborn.charge());
    }

    @Test public void firstEvolutionNeedsTravelAndNeverDisplacesBossIntoHazards() {
        evolve(3, 12);
        Dummy boss = addDummy(6, 4, Char.Alignment.ENEMY);
        boss.addProperty(Char.Property.BOSS);
        boss.addProperty(Char.Property.IMMOVABLE);
        level.terrain(7, 4, Terrain.CHASM);
        int originalBossPosition = boss.pos;
        assertTrue(CowBell.charge(hero, boss.pos));
        assertEquals(originalBossPosition, boss.pos);
        assertEquals(level.cell(5, 4), hero.pos);
        assertEquals(Arrays.asList(level.cell(4, 4), level.cell(5, 4)), level.pressed);
        assertNotNull(boss.buff(Weakness.class));
        assertEquals(1f, boss.buff(Weakness.class).cooldown(), 0f);
        assertEquals(14, CowLegend.get(hero).points());

        Dummy adjacent = addDummy(5, 5, Char.Alignment.ENEMY);
        stubborn.gain(3);
        assertTrue(CowBell.charge(hero, adjacent.pos));
        assertNull(adjacent.buff(Weakness.class));
        assertEquals(level.cell(5, 5), adjacent.pos);
    }

    @Test public void chargeThenStompAddsOnlyTwoShieldAndStillPaysForBothActions() {
        evolve(8, 40);
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        float time = hero.cooldown();
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertNotNull(hero.buff(CowBellCombo.class));
        assertEquals(3, stubborn.charge());
        assertTrue(CowBell.stomp(hero));
        assertEquals(2 + hero.lvl/5 + 2, hero.buff(CowGuard.class).shielding());
        assertNull(hero.buff(CowBellCombo.class));
        assertEquals(0, stubborn.charge());
        assertEquals(time + 2, hero.cooldown(), 0f);
        assertEquals(44, CowLegend.get(hero).points());
    }

    @Test public void stompThenChargeAddsOnlyTwoDamageAndConsumesTheWindow() {
        evolve(8, 40);
        Dummy enemy = addDummy(4, 4, Char.Alignment.ENEMY);
        float time = hero.cooldown();
        assertTrue(CowBell.stomp(hero));
        int before = enemy.HP;
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertEquals(before - CowBell.impactDamage(hero, 0) - 2, enemy.HP);
        assertEquals(2 + hero.lvl/5, hero.buff(CowGuard.class).shielding());
        assertNull(hero.buff(CowBellCombo.class));
        assertEquals(0, stubborn.charge());
        assertEquals(time + 2, hero.cooldown(), 0f);
        assertEquals(44, CowLegend.get(hero).points());
    }

    @Test public void emptyStompAndRepeatedActionsCannotFarmPracticeOrComboBonuses() {
        evolve(8, 40);
        assertTrue(CowBell.stomp(hero));
        assertEquals(40, CowLegend.get(hero).points());
        assertNull(hero.buff(CowBellCombo.class));
        assertEquals(3, stubborn.charge());
        Dummy enemy = addDummy(4, 4, Char.Alignment.ENEMY);
        stubborn.gain(3);
        assertTrue(CowBell.stomp(hero));
        assertTrue(CowBell.stomp(hero));
        assertTrue(enemy.HP < 100);
        assertEquals(2 + hero.lvl/5, hero.buff(CowGuard.class).shielding());
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        assertTrue(CowBellCombo.canFinish(hero, CowBellCombo.CHARGE));
        assertEquals(44, CowLegend.get(hero).points());
        assertEquals(0, stubborn.charge());
    }

    @Test public void comboLifetimeAndActionSurviveCompressedSaveButNotExpiryOrOtherFloors() throws Exception {
        evolve(8, 40);
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        assertTrue(CowBell.charge(hero, enemy.pos));
        CowBellCombo combo = hero.buff(CowBellCombo.class);
        Bundle state = new Bundle();
        combo.storeInBundle(state);
        CowBellCombo restored = new CowBellCombo();
        restored.restoreFromBundle(CowTestSupport.roundTrip(state));
        combo.detach();
        assertTrue(restored.attachTo(hero));
        assertEquals(CowBellCombo.WINDOW, restored.cooldown(), 0f);
        assertTrue(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.CHARGE));
        Dungeon.branch = 1;
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        Dungeon.branch = 0;
        Dungeon.depth = 2;
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        Dungeon.depth = 1;
        restored.act();
        assertNull(hero.buff(CowBellCombo.class));
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));

        // Stairs need not advance a whole turn. The actual shared transition hook
        // must end the sequence rather than merely hiding it on the other floor.
        stubborn.gain(CowBell.COST);
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertTrue(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        Level.beforeTransition();
        Dungeon.depth = 2;
        Dungeon.branch = 1;
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
        Dungeon.depth = 1;
        Dungeon.branch = 0;
        assertNull("立即返回原楼层也不能续上旧连招", hero.buff(CowBellCombo.class));
        assertFalse(CowBellCombo.canFinish(hero, CowBellCombo.STOMP));
    }

    @Test public void debateRespectsRangeSightWallsAlliesAndBossDurationWithoutRefundingResolve() {
        evolve(14, 90);
        Dummy ordinary = addDummy(5, 4, Char.Alignment.ENEMY);
        Dummy ally = addDummy(4, 4, Char.Alignment.ALLY);
        Dummy hidden = addDummy(3, 2, Char.Alignment.ENEMY);
        hero.fieldOfView[hidden.pos] = false;
        Dummy blocked = addDummy(1, 4, Char.Alignment.ENEMY);
        level.terrain(2, 4, Terrain.WALL);
        Dummy far = addDummy(6, 4, Char.Alignment.ENEMY);
        Dummy boss = addDummy(3, 6, Char.Alignment.ENEMY);
        boss.addProperty(Char.Property.BOSS);
        Dummy miniboss = addDummy(4, 6, Char.Alignment.ENEMY);
        miniboss.addProperty(Char.Property.MINIBOSS);
        float time = hero.cooldown();
        assertTrue(CowBell.debate(hero));
        assertEquals(100 - (2 + hero.lvl/4), ordinary.HP);
        assertEquals(3f, ordinary.buff(Weakness.class).cooldown(), 0f);
        assertEquals(1f, boss.buff(Weakness.class).cooldown(), 0f);
        assertEquals(1f, miniboss.buff(Weakness.class).cooldown(), 0f);
        for (Dummy untouched : Arrays.asList(ally, hidden, blocked, far)) {
            assertEquals(100, untouched.HP);
            assertNull(untouched.buff(Weakness.class));
        }
        assertEquals(2, stubborn.charge());
        assertEquals(time + 1, hero.cooldown(), 0f);
        assertEquals(92, CowLegend.get(hero).points());
        assertNull(hero.buff(CowBellCombo.class));
    }

    @Test public void absentAudienceLockedStageDisabledHeroAndInsufficientResolveDoNotSpendTime() {
        evolve(8, 40);
        float time = hero.cooldown();
        Dummy enemy = addDummy(5, 4, Char.Alignment.ENEMY);
        assertFalse(CowBell.debate(hero));
        hero.lvl = 14;
        CowLegend.get(hero).addPoints(50);
        hero.fieldOfView[enemy.pos] = false;
        assertFalse(CowBell.debate(hero));
        hero.fieldOfView[enemy.pos] = true;
        hero.heroClass = HeroClass.WARRIOR;
        assertFalse(CowBell.debate(hero));
        hero.heroClass = HeroClass.COW;
        hero.paralysed = 1;
        assertFalse(CowBell.debate(hero));
        hero.paralysed = 0;
        assertEquals(6, stubborn.charge());
        assertTrue(stubborn.spendCharge(6));
        assertFalse(CowBell.debate(hero));
        assertEquals(0, stubborn.charge());
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(100, enemy.HP);
        assertEquals(90, CowLegend.get(hero).points());
    }

    @Test public void invulnerableTargetsDoNotFarmPracticeOrReceiveShoutWeakness() {
        evolve(14, 90);
        Dummy immune = addDummy(5, 4, Char.Alignment.ENEMY);
        immune.invulnerable = true;
        StatusOnlySprite sprite = new StatusOnlySprite();
        immune.sprite = sprite;
        assertTrue(CowBell.debate(hero));
        assertEquals(100, immune.HP);
        assertNull(immune.buff(Weakness.class));
        assertEquals(90, CowLegend.get(hero).points());
        assertNull(hero.buff(CowBellCombo.class));
        assertEquals(1, sprite.announcements);
        assertEquals(2, stubborn.charge());
    }

    @Test public void evolvedChargeStillRejectsWallsPitsAndAlliesAndCannotPracticeOnInterruptedDash() {
        evolve(14, 90);
        Dummy enemy = addDummy(6, 4, Char.Alignment.ENEMY);
        float time = hero.cooldown();
        int start = hero.pos;
        level.terrain(4, 4, Terrain.WALL);
        assertFalse(CowBell.charge(hero, enemy.pos));
        level.terrain(4, 4, Terrain.CHASM);
        assertFalse(CowBell.charge(hero, enemy.pos));
        level.terrain(4, 4, Terrain.EMPTY);
        Dummy ally = addDummy(4, 4, Char.Alignment.ALLY);
        assertFalse(CowBell.charge(hero, ally.pos));
        assertFalse(CowBell.charge(hero, enemy.pos));
        Actor.remove(ally);
        assertEquals(start, hero.pos);
        assertEquals(time, hero.cooldown(), 0f);
        assertEquals(6, stubborn.charge());
        level.rootAt = level.cell(4, 4);
        assertTrue(CowBell.charge(hero, enemy.pos));
        assertEquals(level.rootAt, hero.pos);
        assertTrue(hero.rooted);
        assertEquals(100, enemy.HP);
        assertEquals(90, CowLegend.get(hero).points());
        assertNull(hero.buff(CowBellCombo.class));
        assertEquals(3, stubborn.charge());
        assertEquals(time + 1, hero.cooldown(), 0f);
    }

    @Test public void bellMasteryUnlocksRangeAndOneDrawPerVisitedDepth() {
        evolve(8, 40);
        CowBell bell = new CowBell();
        assertEquals(3, CowBell.range(hero));
        assertFalse(bell.actions(hero).contains(CowBell.AC_DRAW));
        for (int i = 0; i < 3; i++) CowBellMastery.record(hero, 1);
        assertEquals(1, CowBellMastery.rank(hero));
        assertEquals(4, CowBell.range(hero));
        for (int i = 3; i < 8; i++) CowBellMastery.record(hero, 1);
        assertEquals(2, CowBellMastery.rank(hero));
        assertTrue(bell.actions(hero).contains(CowBell.AC_DRAW));
        assertTrue(CowBellMastery.canDraw(hero));
        assertTrue(CowBellMastery.markDraw(hero));
        assertFalse("不能在同一层反复开盲盒", CowBellMastery.canDraw(hero));
        int oldDepth = Dungeon.depth;
        Dungeon.depth = oldDepth + 1;
        assertTrue("新深度可继续探索", CowBellMastery.canDraw(hero));
        Dungeon.depth = oldDepth;
        assertFalse("返回旧层仍不能重复领取", CowBellMastery.canDraw(hero));
        for (int i = 8; i < 18; i++) CowBellMastery.record(hero, 1);
        assertEquals(15, CowBellMastery.uses(hero));
        assertEquals(3, CowBellMastery.rank(hero));
    }

    private void evolve(int level, int points) {
        hero.lvl = level;
        Buff.detach(hero, CowLegend.class);
        CowLegend.get(hero).addPoints(points);
        stubborn.gain(6);
    }

    private Dummy addDummy(int x, int y, Char.Alignment alignment) {
        Dummy dummy = new Dummy();
        dummy.alignment = alignment;
        dummy.HP = dummy.HT = 100;
        dummy.pos = level.cell(x, y);
        Actor.add(dummy);
        return dummy;
    }

    public static class Dummy extends Char {
        boolean invulnerable;
        void addProperty(Property property) { properties.add(property); }
        @Override protected boolean act() { return false; }
        @Override public boolean isInvulnerable(Class effect) { return invulnerable || super.isInvulnerable(effect); }
    }

    /** Native immunity announces a status unconditionally; this fixture needs no renderer. */
    private static class StatusOnlySprite extends CharSprite {
        int announcements;
        @Override public void showStatus(int color, String text, Object... args) { announcements++; }
    }

    private static class Arena extends Level {
        final ArrayList<Integer> pressed = new ArrayList<>();
        int rootAt = -1;
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
        }
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
        @Override public String tilesTex() { return Assets.Environment.TILES_SEWERS; }
        @Override public String waterTex() { return Assets.Environment.WATER_SEWERS; }
    }
}
