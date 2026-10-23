package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.MidnightSun;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.PastureWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.Stampede;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.items.food.WarmMilk;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.CrackedBell;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.RemainsItem;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentIcon;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.DeviceCompat;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** 覆盖英雄注册、真实开局、界面素材、转职和实际压缩存档往返。 */
public class CowIntegrationTest {
    @BeforeClass public static void startRuntime() throws Exception {
        CowTestSupport.init();
    }

    @Test public void originalClassOrderAndCowUnlockRemainStable() {
        HeroClass[] classes = HeroClass.values();
        assertEquals(7, classes.length);
        String[] original = {"WARRIOR", "MAGE", "ROGUE", "HUNTRESS", "DUELIST", "CLERIC"};
        for (int i = 0; i < original.length; i++) assertEquals(original[i], classes[i].name());
        assertEquals(HeroClass.COW, classes[6]);
        assertFalse("解锁检查必须在正式版本条件下进行", DeviceCompat.isDebug());
        assertTrue("首次游玩即可选择犟牛", HeroClass.COW.isUnlocked());
        assertEquals(HeroSubClass.PALADIN, HeroSubClass.values()[12]);
        assertEquals(HeroSubClass.BULLDOZER, HeroSubClass.values()[13]);
        assertEquals(HeroSubClass.HERDGUARD, HeroSubClass.values()[14]);
    }

    @Test public void allSevenHeroesCanInitializeRealStartingLoadouts() {
        for (HeroClass heroClass : HeroClass.values()) {
            Hero hero = CowTestSupport.freshHero(heroClass);
            assertSame(heroClass, hero.heroClass);
            assertNotNull(heroClass.name(), hero.belongings.weapon);
            assertNotNull(heroClass.name(), hero.belongings.armor);
            assertEquals(20, hero.HT);
            assertEquals(20, hero.HP);
            assertEquals(4, hero.talents.get(0).size());
            assertEquals(5, hero.talents.get(1).size());
            assertEquals(2, hero.talents.get(2).size());
            assertNotNull(heroClass.masteryBadge());
            assertEquals(2, heroClass.subClasses().length);
            assertEquals(3, heroClass.armorAbilities().length);
        }
    }

    @Test public void cowStartsWithItsOwnItemsAndFiniteResource() {
        Hero hero = CowTestSupport.freshHero(HeroClass.COW);
        assertTrue(hero.belongings.weapon instanceof OxHorn);
        assertTrue(hero.belongings.armor instanceof CowhideArmor);
        assertNotNull(hero.belongings.getItem(CowBell.class));
        assertNotNull(hero.belongings.getItem(WarmMilk.class));
        assertTrue(Dungeon.quickslot.getItem(0) instanceof CowBell);
        assertNotNull(hero.buff(Stubbornness.class));
        assertEquals(3, hero.buff(Stubbornness.class).charge());
        assertEquals(6, Stubbornness.capacity(hero));
        assertTrue(RemainsItem.get(HeroClass.COW) instanceof CrackedBell);
        assertTrue(Catalog.MELEE_WEAPONS.items().contains(OxHorn.class));
        assertTrue(Catalog.ARMOR.items().contains(CowhideArmor.class));
        assertTrue(Catalog.MISC_EQUIPMENT.items().contains(CowBell.class));
        assertTrue(Catalog.FOOD.items().contains(WarmMilk.class));
        assertTrue(Catalog.MISC_CONSUMABLES.items().contains(CrackedBell.class));
    }

    @Test public void allLocalesRetainChineseCowTextAndCompleteTalentDescriptions() {
        CowTestSupport.freshHero(HeroClass.COW);
        try {
            for (Languages language : Languages.values()) {
                Messages.setup(language);
                assertChinese(HeroClass.COW.title());
                assertChinese(HeroClass.COW.desc());
                assertEquals("人物介绍必须与四个说明图标逐项对应", 4, HeroClass.COW.desc().split("\n\n").length);
                for (HeroSubClass subClass : HeroClass.COW.subClasses()) {
                    assertChinese(subClass.title());
                    assertChinese(subClass.desc());
                }
                int cowTalents = 0;
                for (Talent talent : Talent.values()) {
                    if (talent.name().startsWith("COW_")) {
                        cowTalents++;
                        assertChinese(talent.title());
                        assertChinese(talent.desc());
                    }
                }
                assertEquals(26, cowTalents);
                for (ArmorAbility ability : HeroClass.COW.armorAbilities()) {
                    assertChinese(ability.name());
                    assertChinese(ability.shortDesc());
                    assertChinese(ability.desc());
                }
                for (Item item : new Item[]{new OxHorn(), new CowBell(), new CowhideArmor(),
                        new WarmMilk(), new CowArmor(), new CrackedBell()}) {
                    assertChinese(item.name());
                    assertChinese(item.desc());
                }
            }
        } finally {
            Messages.setup(Languages.CHI_SMPL);
        }
    }

    @Test public void masteryAndBothBranchesProvideDistinctCompleteTrees() {
        Set<Talent> firstBranch = null;
        for (HeroSubClass subClass : HeroClass.COW.subClasses()) {
            Hero hero = CowTestSupport.freshHero(HeroClass.COW);
            Badges.validateMastery();
            assertTrue(Badges.isUnlocked(Badges.Badge.MASTERY_COW));
            hero.subClass = subClass;
            Talent.initSubclassTalents(hero);
            assertEquals(5, hero.talents.get(2).size());
            Set<Talent> talents = new HashSet<>(hero.talents.get(2).keySet());
            if (firstBranch == null) firstBranch = talents;
            else {
                talents.retainAll(firstBranch);
                assertEquals("两种转职只有通用的三阶天赋相同", 2, talents.size());
            }
        }
    }

    @Test public void crownCreatesCowArmorAndAllThreeAbilityTrees() {
        Set<Class<?>> abilities = new HashSet<>();
        for (ArmorAbility ability : HeroClass.COW.armorAbilities()) {
            Hero hero = CowTestSupport.freshHero(HeroClass.COW);
            hero.belongings.armor.upgrade(2);
            ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
            assertTrue(armor instanceof CowArmor);
            assertEquals(2, armor.trueLevel());
            assertEquals(hero.belongings.armor.tier, armor.tier);
            assertEquals(50f, armor.charge, 0f);
            hero.armorAbility = ability;
            Talent.initArmorTalents(hero);
            assertEquals(4, hero.talents.get(3).size());
            assertTrue(hero.talents.get(3).containsKey(Talent.HEROIC_ENERGY));
            for (Talent talent : hero.talents.get(3).keySet()) assertEquals(4, talent.maxPoints());
            abilities.add(ability.getClass());
        }
        assertEquals(new HashSet<>(Arrays.asList(Stampede.class, PastureWard.class, MidnightSun.class)), abilities);
    }

    @Test public void cowSaveRoundTripKeepsClassBranchesEquipmentTalentsAndResource() throws Exception {
        for (HeroSubClass subClass : HeroClass.COW.subClasses()) {
            for (ArmorAbility ability : HeroClass.COW.armorAbilities()) {
                Hero original = CowTestSupport.freshHero(HeroClass.COW);
                original.subClass = subClass;
                Talent.initSubclassTalents(original);
                original.armorAbility = ability;
                Talent.initArmorTalents(original);
                original.upgradeTalent(Talent.COW_STUBBORN_SPARK);
                original.upgradeTalent(ability.talents()[0]);
                original.belongings.armor = ClassArmor.upgrade(original, original.belongings.armor);
                original.buff(Stubbornness.class).gain(2);
                Bundle wrapper = new Bundle();
                wrapper.put("hero", original);
                Bundle restoredBundle = CowTestSupport.roundTrip(wrapper);
                Hero restored = (Hero) restoredBundle.get("hero");
                Dungeon.hero = restored;
                assertNotNull(restored);
                assertEquals(HeroClass.COW, restored.heroClass);
                assertEquals(subClass, restored.subClass);
                assertEquals(ability.getClass(), restored.armorAbility.getClass());
                assertTrue(restored.belongings.weapon instanceof OxHorn);
                assertTrue(restored.belongings.armor instanceof CowArmor);
                assertNotNull(restored.belongings.getItem(CowBell.class));
                assertNotNull(restored.belongings.getItem(WarmMilk.class));
                assertEquals(1, restored.pointsInTalent(Talent.COW_STUBBORN_SPARK));
                assertEquals(1, restored.pointsInTalent(ability.talents()[0]));
                assertNotNull(restored.buff(Stubbornness.class));
                assertEquals(5, restored.buff(Stubbornness.class).charge());
            }
        }
    }

    @Test public void nativeSpriteFramesAndIconsLoadWithinTheirAtlases() {
        CowTestSupport.freshHero(HeroClass.COW);
        for (int tier = 0; tier < 8; tier++) {
            Image avatar = HeroSprite.avatar(HeroClass.COW, tier);
            assertEquals(11f, avatar.width(), 0f); // 原版头像裁去首列透明像素。
            assertEquals(15f, avatar.height(), 0f);
            assertTrue(avatar.frame().bottom <= 1f);
        }
        assertEquals(16f, Icons.get(HeroClass.COW).width(), 0f);
        for (HeroSubClass subClass : HeroClass.COW.subClasses()) {
            assertEquals(16f, new HeroIcon(subClass).width(), 0f);
        }
        for (ArmorAbility ability : HeroClass.COW.armorAbilities()) {
            assertEquals(16f, new HeroIcon(ability).width(), 0f);
        }
        for (Talent talent : Talent.values()) {
            if (talent.name().startsWith("COW_")) {
                Image icon = new TalentIcon(talent);
                assertEquals(16f, icon.width(), 0f);
                assertTrue(icon.frame().right <= 1f);
            }
        }
    }

    private static void assertChinese(String text) {
        assertNotNull(text);
        assertFalse(text, text.contains(Messages.NO_TEXT_FOUND));
        assertTrue("新增文本应为中文：" + text, text.matches("(?s).*\\p{IsHan}.*"));
    }
}
