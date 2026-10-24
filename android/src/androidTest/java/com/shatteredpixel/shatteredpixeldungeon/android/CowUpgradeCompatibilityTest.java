package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NightShift;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.MidnightSun;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest.assertChinese;
import static com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest.onGame;
import static com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest.screenshot;
import static com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest.waitFor;
import static org.junit.Assert.*;

/**
 * 必须先用已发布的1.0.0 APK和其原测试APK生成slot99，再覆盖安装新版运行本类。
 * 不写入或伪造旧格式；版本及旧字段断言可阻止误把新版存档当作升级测试。
 */
@RunWith(AndroidJUnit4.class)
public class CowUpgradeCompatibilityTest {

    @Test public void publishedV100SaveLoadsAfterInPlaceApkUpgrade() throws Exception {
        try (ActivityScenario<AndroidLauncher> ignored = ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(() -> Gdx.app != null && Game.instance != null, 30);
            waitFor(() -> onGame(() -> Game.scene() != null), 30);

            final LegacySnapshot legacy = onGame(() -> {
                assertTrue("当前安装的必须是新版APK", Game.versionCode >= 1100);
                assertTrue("缺少旧版真实生成的slot99存档，请先运行1.0.0原始冒烟测试",
                        GamesInProgress.gameExists(99));
                Bundle save = FileUtils.bundleFromFile(GamesInProgress.gameFile(99));
                assertEquals("升级测试必须读取已发布1.0.0版本写出的存档", 1001, save.getInt("version"));
                Bundle oldHero = save.getBundle("hero");
                assertEquals("COW", oldHero.getString("class"));
                assertEquals("HERDGUARD", oldHero.getString("subClass"));

                int oldResolve = -1;
                int oldHits = 0;
                boolean oldNightShift = false;
                Bundle[] buffs = oldHero.getBundleArray("buffs");
                assertNotNull(buffs);
                for (Bundle buff : buffs) {
                    String name = buff.getString("__className");
                    assertFalse("旧版存档不应包含新版成长Buff", name.endsWith(".CowLegend"));
                    if (name.endsWith(".Stubbornness")) {
                        oldResolve = buff.getInt("cow_charge");
                        oldHits = buff.getInt("cow_hits");
                    }
                    if (name.endsWith(".NightShift")) oldNightShift = true;
                }
                assertTrue("旧档缺少倔劲记录", oldResolve >= 0);
                assertTrue("旧版测试应保存已激活的夜班精神", oldNightShift);
                assertTrue(oldHero.getBundle("weapon").getString("__className").endsWith(".OxHorn"));
                assertTrue(oldHero.getBundle("armor").getString("__className").endsWith(".CowArmor"));
                LegacySnapshot snapshot = new LegacySnapshot(save, oldHero, oldResolve, oldHits);

                SPDSettings.language(Languages.CHI_SMPL);
                Messages.setup(Languages.CHI_SMPL);
                SPDSettings.intro(false);
                GamesInProgress.curSlot = 99;
                Dungeon.hero = null;
                InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
                ShatteredPixelDungeon.switchNoFade(InterlevelScene.class);
                return snapshot;
            });

            waitFor(() -> onGame(() -> Game.scene() instanceof GameScene
                    && Dungeon.hero != null && Dungeon.hero.ready), 90);
            onGame(() -> {
                assertLegacyCharacter(legacy);
                // 旧版有效命中记录迁入一次；不能把旧档奖励重复结算。
                assertEquals(legacy.practice, CowLegend.get(Dungeon.hero).points());
                assertEquals(legacy.stage(), CowLegend.stage(Dungeon.hero));
                assertNotNull(Dungeon.hero.buff(NightShift.class));
                assertChinese(Dungeon.hero.belongings.weapon.info());
                assertChinese(Dungeon.hero.belongings.getItem(CowBell.class).info());
                return null;
            });
            screenshot("00-旧版存档覆盖升级.png");

            onGame(() -> {
                Dungeon.saveAll();
                Bundle upgraded = FileUtils.bundleFromFile(GamesInProgress.gameFile(99));
                assertEquals(Game.versionCode, upgraded.getInt("version"));
                ShatteredPixelDungeon.switchNoFade(TitleScene.class);
                return null;
            });
            waitFor(() -> onGame(() -> Game.scene() instanceof TitleScene), 30);
            onGame(() -> {
                Dungeon.hero = null;
                GamesInProgress.curSlot = 99;
                InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
                ShatteredPixelDungeon.switchNoFade(InterlevelScene.class);
                return null;
            });
            waitFor(() -> onGame(() -> Game.scene() instanceof GameScene
                    && Dungeon.hero != null && Dungeon.hero.ready), 90);
            onGame(() -> {
                assertLegacyCharacter(legacy);
                assertEquals("再次读档不得重复迁入旧版命中次数", legacy.practice,
                        CowLegend.get(Dungeon.hero).points());
                assertEquals(legacy.stage(), CowLegend.stage(Dungeon.hero));
                return null;
            });
            screenshot("01-升级后再次保存恢复.png");
            System.out.println("已发布1.0.0 APK真实存档 → 覆盖安装新版 → 正常读取 → 新版保存再次读取，验证通过。");
        }
    }

    private static void assertLegacyCharacter(LegacySnapshot old) {
        Hero hero = Dungeon.hero;
        assertEquals(HeroClass.COW, hero.heroClass);
        assertEquals(HeroSubClass.HERDGUARD, hero.subClass);
        assertEquals(old.position, hero.pos);
        assertEquals(old.depth, Dungeon.depth);
        assertEquals(old.branch, Dungeon.branch);
        assertEquals(old.level, hero.lvl);
        assertEquals(old.maxHealth, hero.HT);
        assertEquals(old.gold, Dungeon.gold);
        assertTrue(hero.isAlive());
        assertTrue(hero.belongings.weapon instanceof OxHorn);
        assertTrue(hero.belongings.armor instanceof CowArmor);
        assertEquals(old.weaponLevel, hero.belongings.weapon.trueLevel());
        assertEquals(old.armorLevel, hero.belongings.armor.trueLevel());
        assertNotNull(hero.belongings.getItem(CowBell.class));
        assertTrue(hero.armorAbility instanceof MidnightSun);
        assertNotNull(hero.buff(Stubbornness.class));
        assertEquals(old.resolve, hero.buff(Stubbornness.class).charge());
    }

    private static final class LegacySnapshot {
        final int position, depth, branch, level, maxHealth, gold, weaponLevel, armorLevel, resolve, practice;

        LegacySnapshot(Bundle save, Bundle hero, int resolve, int oldHits) {
            position = hero.getInt("pos");
            depth = save.getInt("depth");
            branch = save.getInt("branch");
            level = hero.getInt("lvl");
            maxHealth = hero.getInt("HT");
            gold = save.getInt("gold");
            weaponLevel = hero.getBundle("weapon").getInt("level");
            armorLevel = hero.getBundle("armor").getInt("level");
            this.resolve = resolve;
            practice = Math.max(0, oldHits);
        }

        int stage() {
            if (level >= 14 && practice >= 90) return 3;
            if (level >= 8 && practice >= 40) return 2;
            if (level >= 3 && practice >= 12) return 1;
            return 0;
        }
    }
}
