package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowCraft;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowJournal;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NightShift;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PastureField;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.MidnightSun;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.PastureWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.Stampede;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeadowCake;
import com.shatteredpixel.shatteredpixeldungeon.items.food.WarmMilk;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
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

/** Real armor casts, resolved attacks, bounded discoveries and compressed save round trips. */
public class CowCraftJournalTest {
    private Hero hero;
    private Arena level;
    private ClassArmor armor;

    @BeforeClass public static void initialize() throws Exception { CowTestSupport.init(); }

    @Before public void reset() {
        hero = CowTestSupport.freshHero(HeroClass.COW);
        hero.lvl = 20;
        Dungeon.depth = 1;
        Dungeon.branch = 0;
        level = new Arena();
        Dungeon.level = level;
        hero.pos = level.cell(3, 4);
        hero.fieldOfView = level.heroFOV;
        Arrays.fill(hero.fieldOfView, true);
        Actor.add(hero);
        armor = ClassArmor.upgrade(hero, hero.belongings.armor);
        hero.belongings.armor = armor;
        armor.charge = 100;
    }

    @Test public void stampedeCountsOncePerEffectiveCastAndEvolvesAtThreeAndSeven() {
        Dummy near = enemy(4, 4);
        Dummy far = enemy(6, 4);
        for (int cast = 1; cast <= 8; cast++) {
            int nearHealth = near.HP, farHealth = far.HP;
            float time = hero.cooldown();
            armor.charge = 100;
            new TestStampede().cast(armor, hero, near.pos);
            assertTrue(near.HP < nearHealth);
            assertTrue(far.HP < farHealth);
            assertEquals("贯穿多个敌人每次施放只记一次", Math.min(12, cast), CowCraft.uses(hero, CowCraft.STAMPEDE));
            assertEquals(cast >= 7 ? 2 : cast >= 3 ? 1 : 0, CowCraft.rank(hero, CowCraft.STAMPEDE));
            assertEquals("历练按施放计，不按命中目标数翻倍", 3 * cast, CowLegend.get(hero).points());
            assertEquals(65, armor.charge, 0f);
            assertEquals(time + 1, hero.cooldown(), 0f);
            assertEquals(0, CowCraft.uses(hero, CowCraft.PASTURE));
            assertEquals(0, CowCraft.uses(hero, CowCraft.NIGHT));
            if (cast >= 4) assertNotNull(near.buff(Weakness.class));
            else assertNull(near.buff(Weakness.class));
            assertNull("一阶只对首个有效目标施加虚弱", far.buff(Weakness.class));
            if (cast == 8) assertEquals(2, hero.buff(CowGuard.class).shielding());
        }
    }

    @Test public void emptyOrNegatedStampedeCannotTrainAndInvalidCastCannotSpend() {
        TestStampede skill = new TestStampede();
        skill.cast(armor, hero, level.cell(6, 4));
        assertEquals(65, armor.charge, 0f);
        assertEquals(0, CowCraft.uses(hero, CowCraft.STAMPEDE));
        assertEquals(0, CowLegend.get(hero).points());
        Dummy target = enemy(4, 4);
        target.ignoreDamage = true;
        armor.charge = 100;
        skill.cast(armor, hero, target.pos);
        assertEquals(1000, target.HP);
        assertEquals(0, CowCraft.uses(hero, CowCraft.STAMPEDE));
        assertEquals(0, CowLegend.get(hero).points());
        armor.charge = 34;
        float time = hero.cooldown();
        skill.cast(armor, hero, target.pos);
        assertEquals(34, armor.charge, 0f);
        assertEquals(time, hero.cooldown(), 0f);
        hero.paralysed = 1;
        armor.charge = 100;
        skill.cast(armor, hero, target.pos);
        assertEquals(100, armor.charge, 0f);
        assertEquals(time, hero.cooldown(), 0f);
        assertNull(hero.buff(CowCraft.class));
    }

    @Test public void pastureOnlyTrainsOnFirstRealEnemyAndKeepsItsReceiptAcrossSave() throws Exception {
        new TestPastureWard().cast(armor, hero);
        assertEquals(65, armor.charge, 0f);
        PastureField field = hero.buff(PastureField.class);
        field.act();
        assertEquals(0, CowCraft.uses(hero, CowCraft.PASTURE));
        Dummy target = enemy(7, 4);
        field.act();
        assertEquals(0, CowCraft.uses(hero, CowCraft.PASTURE));
        target.pos = level.cell(4, 4);
        field.act();
        assertNotNull(target.buff(Cripple.class));
        assertEquals(1, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(3, CowLegend.get(hero).points());
        field.act();
        field = reload(field, new PastureField());
        field.act();
        assertEquals(1, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(3, CowLegend.get(hero).points());
        armor.charge = 100;
        new TestPastureWard().cast(armor, hero);
        hero.buff(PastureField.class).act();
        assertEquals(2, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(6, CowLegend.get(hero).points());
        assertEquals(65, armor.charge, 0f);
    }

    @Test public void pastureMasteryAddsBoundedShieldAndOneFootingRecoveryPerField() {
        enemy(4, 4);
        for (int cast = 1; cast <= 7; cast++) {
            armor.charge = 100;
            new TestPastureWard().cast(armor, hero);
            assertEquals(65, armor.charge, 0f);
            int cap = 8 + hero.lvl / 2 + (cast >= 4 ? 2 : 0);
            assertEquals(cap, hero.buff(CowGuard.class).shielding());
            hero.buff(PastureField.class).act();
            assertEquals(cast, CowCraft.uses(hero, CowCraft.PASTURE));
            assertEquals(cast >= 7 ? 2 : cast >= 3 ? 1 : 0, CowCraft.rank(hero, CowCraft.PASTURE));
        }
        armor.charge = 100;
        new TestPastureWard().cast(armor, hero);
        PastureField field = hero.buff(PastureField.class);
        Buff.prolong(hero, Cripple.class, 100);
        field.act();
        field.act();
        assertNotNull("第三次脉冲之前不应清除残废", hero.buff(Cripple.class));
        field.act();
        assertNull(hero.buff(Cripple.class));
        Buff.prolong(hero, Cripple.class, 100);
        field.act();
        assertNotNull("每片牧场只能恢复一次脚步", hero.buff(Cripple.class));
        assertTrue(hero.buff(CowGuard.class).shielding() <= 10 + hero.lvl / 2);
        assertEquals(7, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(65, armor.charge, 0f);
    }

    @Test public void oldPastureAndOldNightShiftCannotGrantUnpaidUpgradePractice() throws Exception {
        Dummy target = enemy(4, 4);
        new TestPastureWard().cast(armor, hero);
        PastureField current = hero.buff(PastureField.class);
        Bundle oldPasture = state(current);
        oldPasture.remove("pasture_practiced");
        current.detach();
        PastureField legacyPasture = new PastureField();
        legacyPasture.restoreFromBundle(CowTestSupport.roundTrip(oldPasture));
        assertTrue(legacyPasture.attachTo(hero));
        legacyPasture.act();
        assertEquals(0, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(0, CowLegend.get(hero).points());
        armor.charge = 100;
        new TestMidnightSun().cast(armor, hero);
        NightShift shift = hero.buff(NightShift.class);
        Bundle oldNight = state(shift);
        oldNight.remove("night_practiced");
        shift.detach();
        NightShift legacyNight = new NightShift();
        legacyNight.restoreFromBundle(CowTestSupport.roundTrip(oldNight));
        assertTrue(legacyNight.attachTo(hero));
        assertTrue(melee(target));
        assertEquals(0, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals("旧夜班只产生普通真实命中的历练", 1, CowLegend.get(hero).points());
    }

    @Test public void nightShiftOnlyTrainsOnFirstResolvedHostileHitAndPersistsThatReceipt() throws Exception {
        Dummy target = enemy(4, 4);
        new TestMidnightSun().cast(armor, hero);
        assertEquals(60, armor.charge, 0f);
        assertEquals(0, CowCraft.uses(hero, CowCraft.NIGHT));
        target.armor = 10000;
        assertTrue(melee(target));
        assertEquals(1000, target.HP);
        assertEquals(0, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(0, CowLegend.get(hero).points());
        target.armor = 0;
        target.ignoreDamage = true;
        assertTrue(melee(target));
        assertEquals(1000, target.HP);
        assertEquals(0, CowCraft.uses(hero, CowCraft.NIGHT));
        target.ignoreDamage = false;
        assertTrue(melee(target));
        assertTrue(target.HP < 1000);
        assertEquals(1, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(4, CowLegend.get(hero).points());
        assertTrue(melee(target));
        assertEquals(1, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(5, CowLegend.get(hero).points());
        reload(hero.buff(NightShift.class), new NightShift());
        assertTrue(melee(target));
        assertEquals(1, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(6, CowLegend.get(hero).points());
        armor.charge = 100;
        new TestMidnightSun().cast(armor, hero);
        assertTrue(melee(target));
        assertEquals(2, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(10, CowLegend.get(hero).points());
        assertEquals(60, armor.charge, 0f);
    }

    @Test public void nightMasteryUnlocksCleanseAndTwoExtraTurnsWithoutFreeCharge() {
        Dummy target = enemy(4, 4);
        assertEquals(8, MidnightSun.duration(hero), 0f);
        for (int cast = 1; cast <= 7; cast++) {
            Buff.prolong(hero, Cripple.class, 100);
            armor.charge = 100;
            new TestMidnightSun().cast(armor, hero);
            assertEquals(60, armor.charge, 0f);
            if (cast >= 4) assertNull(hero.buff(Cripple.class));
            else assertNotNull(hero.buff(Cripple.class));
            assertTrue(melee(target));
            assertEquals(cast, CowCraft.uses(hero, CowCraft.NIGHT));
            assertEquals(cast >= 7 ? 2 : cast >= 3 ? 1 : 0, CowCraft.rank(hero, CowCraft.NIGHT));
        }
        assertEquals(10, MidnightSun.duration(hero), 0f);
        armor.charge = 100;
        new TestMidnightSun().cast(armor, hero);
        assertEquals(60, armor.charge, 0f);
        assertTrue(melee(target));
        assertEquals(7, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(10, MidnightSun.duration(hero), 0f);
    }

    @Test public void stationaryAbilitiesRejectInsufficientEnergyAndParalysis() {
        float time = hero.cooldown();
        armor.charge = 34;
        new TestPastureWard().cast(armor, hero);
        assertEquals(34, armor.charge, 0f);
        assertNull(hero.buff(PastureField.class));
        assertEquals(time, hero.cooldown(), 0f);
        armor.charge = 39;
        new TestMidnightSun().cast(armor, hero);
        assertEquals(39, armor.charge, 0f);
        assertNull(hero.buff(NightShift.class));
        assertEquals(time, hero.cooldown(), 0f);
        hero.paralysed = 1;
        armor.charge = 100;
        new TestPastureWard().cast(armor, hero);
        new TestMidnightSun().cast(armor, hero);
        assertEquals(100, armor.charge, 0f);
        assertEquals(time, hero.cooldown(), 0f);
        assertNull(hero.buff(PastureField.class));
        assertNull(hero.buff(NightShift.class));
        assertNull(hero.buff(CowCraft.class));
    }

    @Test public void skillPracticeIsIndependentBoundedSavedAndNeverGrantedToInvalidActors() throws Exception {
        assertNull(hero.buff(CowCraft.class));
        assertEquals(0, CowCraft.uses(hero, CowCraft.STAMPEDE));
        assertEquals(0, CowCraft.rank(hero, CowCraft.PASTURE));
        assertNull(hero.buff(CowCraft.class));
        CowCraft.record(hero, -1, true);
        CowCraft.record(hero, 3, true);
        CowCraft.record(hero, CowCraft.STAMPEDE, false);
        hero.heroClass = HeroClass.WARRIOR;
        CowCraft.record(hero, CowCraft.STAMPEDE, true);
        hero.heroClass = HeroClass.COW;
        hero.HP = 0;
        CowCraft.record(hero, CowCraft.STAMPEDE, true);
        assertNull(hero.buff(CowCraft.class));
        hero.HP = hero.HT;
        for (int i = 0; i < 9; i++) CowCraft.record(hero, CowCraft.STAMPEDE, true);
        for (int i = 0; i < 3; i++) CowCraft.record(hero, CowCraft.PASTURE, true);
        CowCraft.record(hero, CowCraft.NIGHT, true);
        assertEquals(12, CowCraft.uses(hero, CowCraft.STAMPEDE));
        assertEquals(3, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(1, CowCraft.uses(hero, CowCraft.NIGHT));
        CowCraft craft = reload(hero.buff(CowCraft.class), new CowCraft());
        assertSame(craft, hero.buff(CowCraft.class));
        assertEquals(7, CowCraft.uses(hero, CowCraft.STAMPEDE));
        assertEquals(3, CowCraft.uses(hero, CowCraft.PASTURE));
        assertEquals(1, CowCraft.uses(hero, CowCraft.NIGHT));
        assertEquals(2, CowCraft.rank(hero, CowCraft.STAMPEDE));
        assertEquals(1, CowCraft.rank(hero, CowCraft.PASTURE));
        assertEquals(0, CowCraft.rank(hero, CowCraft.NIGHT));
    }

    @Test public void everyDiscoveryIsOneTimeAndOnlyTwoOfferAOneTimeTwoPointShield() throws Exception {
        CowJournal journal = CowJournal.get(hero);
        assertEquals(0, journal.count());
        for (int entry = 0; entry < CowJournal.TOTAL; entry++) {
            assertTrue(journal.discover(hero, entry));
            assertTrue(journal.found(entry));
            assertEquals(entry + 1, journal.count());
            assertFalse(journal.discover(hero, entry));
            if (entry == 3 || entry == 17) {
                CowGuard guard = hero.buff(CowGuard.class);
                assertNotNull(guard);
                assertEquals(2, guard.shielding());
                guard.absorbDamage(2);
                guard.detach();
                assertFalse("吃掉奖励盾后不能重复刷取", journal.discover(hero, entry));
                assertNull(hero.buff(CowGuard.class));
            } else assertNull(hero.buff(CowGuard.class));
        }
        assertEquals(18, journal.count());
        journal = reload(journal, new CowJournal());
        assertEquals(18, journal.count());
        for (int entry = 0; entry < CowJournal.TOTAL; entry++) {
            assertTrue(journal.found(entry));
            assertFalse(journal.discover(hero, entry));
        }
        assertNull(hero.buff(CowGuard.class));
    }

    @Test public void partialDiscoveryCountersSurviveSaveAndContinueExactlyOnceAtTheirThresholds() throws Exception {
        Dummy target = enemy(4, 4);
        for (int i = 0; i < 5; i++) {
            CowJournal.onMove(hero, Terrain.WATER);
            CowJournal.onMove(hero, Terrain.OPEN_DOOR);
        }
        for (int i = 0; i < 4; i++) CowJournal.onFood(hero, new Food());
        for (int i = 0; i < 99; i++) CowJournal.onHit(hero, target, 1);
        CowJournal journal = CowJournal.get(hero);
        assertEquals(0, journal.count());
        journal = reload(journal, new CowJournal());
        CowJournal.onMove(hero, Terrain.WATER);
        CowJournal.onMove(hero, Terrain.DOOR);
        CowJournal.onFood(hero, new Food());
        CowJournal.onHit(hero, target, 1);
        assertEquals(4, journal.count());
        assertTrue(journal.found(0));
        assertTrue(journal.found(1));
        assertTrue(journal.found(5));
        assertTrue(journal.found(10));
        for (int i = 0; i < 200; i++) {
            CowJournal.onMove(hero, Terrain.WATER);
            CowJournal.onMove(hero, Terrain.DOOR);
            CowJournal.onFood(hero, new Food());
            CowJournal.onHit(hero, target, 1);
        }
        assertEquals(4, journal.count());
        Bundle bounded = state(journal);
        assertEquals(6, bounded.getInt("cow_water"));
        assertEquals(6, bounded.getInt("cow_doors"));
        assertEquals(5, bounded.getInt("cow_meals"));
        assertEquals(100, bounded.getInt("cow_journal_hits"));
    }

    @Test public void allEighteenDiscoveryEventsAreReachableThroughTheirGameplayHooks() {
        for (int i = 0; i < 6; i++) {
            CowJournal.onMove(hero, Terrain.WATER);
            CowJournal.onMove(hero, Terrain.OPEN_DOOR);
        }
        CowJournal.onFood(hero, new WarmMilk());
        CowJournal.onFood(hero, new MeadowCake());
        CowJournal.onFood(hero, new FrozenCarpaccio());
        CowJournal.onFood(hero, new Food());
        CowJournal.onFood(hero, new Food());
        CowJournal.onHit(hero, new Crab(), 1);
        CowJournal.onHit(hero, new Skeleton(), 1);
        Mimic mimic = new Mimic();
        CowJournal.onHit(hero, mimic, 1);
        assertFalse("尚未显形的中立拟态怪不能计入战斗发现", CowJournal.get(hero).found(8));
        assertEquals(2, state(CowJournal.get(hero)).getInt("cow_journal_hits"));
        mimic.alignment = Char.Alignment.ENEMY;
        mimic.state = mimic.HUNTING;
        mimic.HP = mimic.HT = 30;
        CowJournal.onHit(hero, mimic, 1);
        CowJournal.onHit(hero, new DM100(), 1);
        Dummy target = enemy(4, 4);
        for (int i = 0; i < 96; i++) CowJournal.onHit(hero, target, 1);
        Dungeon.depth = 6;
        CowJournal.onMove(hero, Terrain.EMPTY);
        Dungeon.depth = 16;
        CowJournal.onMove(hero, Terrain.EMPTY);
        hero.belongings.armor = null;
        CowJournal.onMove(hero, Terrain.EMPTY);
        for (int skill = 0; skill < 3; skill++) CowJournal.onPractice(hero, skill, 3);
        hero.HP = 1;
        CowJournal.onDamage(hero, target, 1);
        CowJournal journal = CowJournal.get(hero);
        assertEquals(18, journal.count());
        for (int entry = 0; entry < CowJournal.TOTAL; entry++) assertTrue("无法通过事件到达彩蛋" + entry, journal.found(entry));
    }

    @Test public void deadOrForeignHeroesCannotCreateOrMutateDiscoveryStateAndAlbumIsReadOnly() {
        assertNull(hero.buff(CowJournal.class));
        assertChinese(CowJournal.album(hero));
        assertChinese(CowJournal.album(null));
        assertNull("查看空相册不能创建记录Buff", hero.buff(CowJournal.class));
        Dummy target = enemy(4, 4);
        hero.heroClass = HeroClass.WARRIOR;
        triggerJournalEvents(target);
        assertNull(hero.buff(CowJournal.class));
        hero.heroClass = HeroClass.COW;
        hero.HP = 0;
        triggerJournalEvents(target);
        assertNull(hero.buff(CowJournal.class));
        hero.HP = hero.HT;
        CowJournal journal = CowJournal.get(hero);
        Bundle before = state(journal);
        hero.HP = 0;
        triggerJournalEvents(target);
        assertFalse(journal.discover(hero, 3));
        assertEquals(before.toString(), state(journal).toString());
        assertNull(hero.buff(CowGuard.class));
        hero.HP = hero.HT;
        hero.heroClass = HeroClass.WARRIOR;
        assertFalse(journal.discover(hero, 17));
        assertEquals(0, journal.count());
        hero.heroClass = HeroClass.COW;
        assertFalse(journal.discover(hero, Integer.MAX_VALUE));
        assertFalse(journal.found(Integer.MIN_VALUE));
    }

    private void triggerJournalEvents(Dummy target) {
        CowJournal.onMove(hero, Terrain.WATER);
        CowJournal.onFood(hero, new MeadowCake());
        CowJournal.onHit(hero, target, 1);
        CowJournal.onPractice(hero, CowCraft.STAMPEDE, 3);
        CowJournal.onDamage(hero, target, 1);
    }

    private boolean melee(Dummy target) {
        // The headless test uses the real attack/damage pipeline, with cosmetic audio/rendering disabled.
        Arrays.fill(level.heroFOV, false);
        try { return hero.attack(target, 1f, 0f, Float.POSITIVE_INFINITY); }
        finally { Arrays.fill(level.heroFOV, true); }
    }

    private Dummy enemy(int x, int y) {
        Dummy dummy = new Dummy();
        dummy.HP = dummy.HT = 1000;
        dummy.pos = level.cell(x, y);
        dummy.alignment = Char.Alignment.ENEMY;
        Actor.add(dummy);
        return dummy;
    }

    private static Bundle state(Buff buff) {
        Bundle bundle = new Bundle();
        buff.storeInBundle(bundle);
        return bundle;
    }

    private <T extends Buff> T reload(T original, T restored) throws Exception {
        Bundle bundle = CowTestSupport.roundTrip(state(original));
        original.detach();
        restored.restoreFromBundle(bundle);
        assertTrue(restored.attachTo(hero));
        return restored;
    }

    private static void assertChinese(String text) {
        assertNotNull(text);
        assertFalse(text.contains(Messages.NO_TEXT_FOUND));
        assertTrue("成长与发现说明必须显示中文", text.matches("(?s).*\\p{IsHan}.*"));
    }

    public static class Dummy extends Char {
        int armor;
        boolean ignoreDamage;
        @Override public int drRoll() { return armor; }
        @Override public void damage(int damage, Object source) { if (!ignoreDamage) super.damage(damage, source); }
        @Override protected boolean act() { return false; }
    }

    private static class TestStampede extends Stampede {
        void cast(ClassArmor armor, Hero hero, int target) { activate(armor, hero, target); }
    }
    private static class TestPastureWard extends PastureWard {
        void cast(ClassArmor armor, Hero hero) { activate(armor, hero, hero.pos); }
    }
    private static class TestMidnightSun extends MidnightSun {
        void cast(ClassArmor armor, Hero hero) { activate(armor, hero, hero.pos); }
    }

    private static class Arena extends Level {
        Arena() {
            setSize(13, 10);
            transitions = new ArrayList<>(); mobs = new HashSet<>(); heaps = new SparseArray<>();
            blobs = new HashMap<>(); plants = new SparseArray<>(); traps = new SparseArray<>();
            customTiles = new ArrayList<>(); customTerrain = new ArrayList<>(); customWalls = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) map[cell(x, y)] = Terrain.EMPTY;
            discoverable = new boolean[length]; Arrays.fill(discoverable, true);
            buildFlagMaps();
        }
        int cell(int x, int y) { return y * width + x; }
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
        @Override public String tilesTex() { return Assets.Environment.TILES_SEWERS; }
        @Override public String waterTex() { return Assets.Environment.WATER_SEWERS; }
    }
}
