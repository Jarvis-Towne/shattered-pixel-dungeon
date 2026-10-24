package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowBellMastery;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowCraft;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfBullishResolve;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfCowCouncil;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.OvertimeInspector;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/** V1.3: smooth training, exact exclusive sprite identity, usability, and safe new item/monster paths. */
public class CowV13ProgressionTest {
    private Hero hero;
    @BeforeClass public static void boot() throws Exception { CowTestSupport.init(); }
    @Before public void init() { hero = CowTestSupport.freshHero(HeroClass.COW); }

    @Test public void smoothFunctionNeverRegressesAndContinuesBeyondAllTierThresholds() {
        int previous = 0;
        for (int points = 0; points <= 10000; points += 7) {
            int current = CowLegend.growth(points);
            assertTrue("练习不应降低战斗属性", current >= previous);
            previous = current;
        }
        assertEquals(0, CowLegend.growth(0));
        assertTrue(CowLegend.growth(1000) > CowLegend.growth(90));
        assertTrue(CowLegend.growth(10000) > CowLegend.growth(1000));
    }

    @Test public void legacyAppearanceAndTenStrengthRemainAcrossAllStagesAndCrown() {
        OxHorn weapon = (OxHorn)hero.belongings.weapon;
        CowhideArmor armor = (CowhideArmor)hero.belongings.armor;
        assertEquals(10, weapon.STRReq(0));
        assertEquals(10, armor.STRReq(0));
        assertEquals(ItemSpriteSheet.COW_HORN, weapon.image());
        assertEquals(ItemSpriteSheet.COW_HIDE, armor.image());

        CowLegend legend = CowLegend.get(hero);
        hero.lvl = 3;
        legend.addPoints(12);
        assertEquals(2, weapon.tier);
        assertEquals(2, armor.tier);
        assertEquals(10, weapon.STRReq(0));
        assertEquals(10, armor.STRReq(0));
        assertEquals(ItemSpriteSheet.COW_HORN, weapon.image());
        assertEquals(ItemSpriteSheet.COW_HIDE, armor.image());

        hero.lvl = 8;
        legend.addPoints(28);
        assertEquals(3, weapon.tier);
        assertEquals(3, armor.tier);
        hero.lvl = 14;
        legend.addPoints(50);
        assertEquals(4, weapon.tier);
        assertEquals(4, armor.tier);
        assertEquals(10, weapon.STRReq(0));
        assertEquals(10, armor.STRReq(0));
        assertEquals(ItemSpriteSheet.COW_HORN, weapon.image());
        assertEquals(ItemSpriteSheet.COW_HIDE, armor.image());

        int maxAt90 = weapon.max(0);
        int guardAt90 = armor.DRMax(0);
        legend.addPoints(10000);
        assertTrue(weapon.max(0) > maxAt90);
        assertTrue(armor.DRMax(0) > guardAt90);
        CowArmor classArmor = (CowArmor)ClassArmor.upgrade(hero, armor);
        hero.belongings.armor = classArmor;
        assertEquals(ItemSpriteSheet.COW_ARMOR, classArmor.image());
        assertEquals(10, classArmor.STRReq(0));
        assertTrue(classArmor.DRMax(0) >= guardAt90);
    }

    @Test public void oldIntPracticeSavedStateMigratesToLongWithoutResetting() throws Exception {
        CowLegend legend = CowLegend.get(hero);
        Bundle data = new Bundle();
        legend.storeInBundle(data);
        data.put("cow_practice", 999);
        legend.restoreFromBundle(CowTestSupport.roundTrip(data));
        assertEquals(999L, legend.points());
        legend.addPoints(5000);
        assertEquals(5999L, legend.points());
        Bundle persisted = new Bundle();
        legend.storeInBundle(persisted);
        CowLegend restored = new CowLegend();
        restored.restoreFromBundle(CowTestSupport.roundTrip(persisted));
        assertEquals(5999L, restored.points());
    }

    @Test public void masteryContinuesAfterFinalRanksAndPersistsCounts() throws Exception {
        for (int i = 0; i < 23; i++) CowBellMastery.record(hero, 1);
        assertEquals(3, CowBellMastery.rank(hero));
        assertEquals(23, CowBellMastery.uses(hero));
        assertTrue(CowBellMastery.growth(hero) > 0);
        for (int i = 0; i < 19; i++) CowCraft.record(hero, CowCraft.STAMPEDE, true);
        assertEquals(3, CowCraft.rank(hero, CowCraft.STAMPEDE));
        assertEquals(19, CowCraft.uses(hero, CowCraft.STAMPEDE));
        Bundle bundle = new Bundle();
        bundle.put("hero", hero);
        Hero restored = (Hero)CowTestSupport.roundTrip(bundle).get("hero");
        Dungeon.hero = restored;
        assertEquals(23, CowBellMastery.uses(restored));
        assertEquals(19, CowCraft.uses(restored, CowCraft.STAMPEDE));
    }

    @Test public void newDropsAreIdentifiedAndTonicDoesNotChangePermanentPractice() {
        PotionOfBullishResolve tonic = new PotionOfBullishResolve();
        ScrollOfCowCouncil council = new ScrollOfCowCouncil();
        assertTrue(tonic.isKnown());
        assertTrue(council.isKnown());
        assertEquals(ItemSpriteSheet.POTION_AMBER, tonic.image());
        assertEquals(ItemSpriteSheet.SCROLL_ODAL, council.image());
        CowLegend legend = CowLegend.get(hero);
        long practiceBefore = legend.points();
        tonic.apply(hero);
        assertNotNull(hero.buff(Bless.class));
        assertNotNull(hero.buff(CowGuard.class));
        assertEquals(Stubbornness.capacity(hero), hero.buff(Stubbornness.class).charge());
        assertEquals(practiceBefore, legend.points());
        assertEquals(0, ScrollOfCowCouncil.council(hero)); // no dungeon scene; no free time/use
        OvertimeInspector inspector = new OvertimeInspector();
        Item loot = inspector.createLoot();
        assertTrue(loot instanceof PotionOfBullishResolve || loot instanceof ScrollOfCowCouncil);
        assertFalse(OvertimeInspector.summon(hero));
    }
}
