package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PastureField;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NightShift;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.MidnightSun;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow.PastureWard;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.WarmMilk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** 只在 CI 的空白模拟器运行；测试数据使用独立存档槽 99。 */
@RunWith(AndroidJUnit4.class)
public class CowSmokeTest {
    private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @Test public void cowCanStartMoveCastAndRestoreOnAndroid() throws Exception {
        try (ActivityScenario<AndroidLauncher> ignored = ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(() -> Gdx.app != null && Game.instance != null, 30);
            waitFor(() -> onGame(() -> Game.scene() != null), 30);
            onGame(() -> {
                SPDSettings.language(Languages.CHI_SMPL);
                Messages.setup(Languages.CHI_SMPL);
                SPDSettings.intro(false);
                GamesInProgress.curSlot = 99;
                GamesInProgress.selectedClass = null;
                ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class);
                return null;
            });
            waitFor(() -> onGame(() -> Game.scene() instanceof HeroSelectScene), 30);
            screenshot("00-选人界面.png");
            Point cowButton = onGame(() -> {
                ArrayList<StyledButton> buttons = field(Game.scene(), "heroBtns");
                assertEquals(7, buttons.size());
                StyledButton cow = buttons.get(6);
                assertTrue("第七英雄按钮超出画面", cow.left() >= 0 && cow.right() <= Camera.main.width);
                assertTrue(HeroClass.COW.isUnlocked());
                return GamesInProgress.selectedClass == HeroClass.COW ? null : center(cow);
            });
            if (cowButton != null) {
                System.out.println("点击犟牛图标：" + cowButton.x + "," + cowButton.y
                        + "；屏幕：" + device.getDisplayWidth() + "×" + device.getDisplayHeight());
                assertTrue(device.click(cowButton.x, cowButton.y));
            }
            waitFor(() -> onGame(() -> GamesInProgress.selectedClass == HeroClass.COW), 15);
            screenshot("01-选择犟牛.png");

            WndHeroInfo info = onGame(() -> {
                WndHeroInfo window = new WndHeroInfo(HeroClass.COW);
                Game.scene().add(window);
                return window;
            });
            screenshot("02-中文职业说明.png");
            onGame(() -> { info.hide(); return null; });

            Point start = onGame(() -> center(field(Game.scene(), "startBtn")));
            device.click(start.x, start.y);
            waitFor(() -> {
                Point next = onGame(() -> {
                    if (Game.scene() instanceof InterlevelScene) {
                        StyledButton button = field(Game.scene(), "btnContinue");
                        if (button != null && button.visible && button.active) return center(button);
                    }
                    return null;
                });
                if (next != null) device.click(next.x, next.y);
                return onGame(() -> Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready);
            }, 90);

            int destination = onGame(() -> {
                Hero hero = Dungeon.hero;
                assertEquals(HeroClass.COW, hero.heroClass);
                assertEquals("犟牛", HeroClass.COW.title());
                assertTrue(hero.belongings.weapon instanceof OxHorn);
                assertTrue(hero.belongings.armor instanceof CowhideArmor);
                assertNotNull(hero.belongings.getItem(CowBell.class));
                assertNotNull(hero.belongings.getItem(WarmMilk.class));
                assertEquals(3, hero.buff(Stubbornness.class).charge());
                for (int delta : PathFinder.NEIGHBOURS8) {
                    int cell = hero.pos + delta;
                    if (Dungeon.level.insideMap(cell) && Dungeon.level.passable[cell]
                            && !Dungeon.level.pit[cell] && Dungeon.level.getTransition(cell) == null
                            && Dungeon.level.traps.get(cell) == null && Actor.findChar(cell) == null) {
                        assertTrue(hero.handle(cell));
                        hero.next();
                        return cell;
                    }
                }
                throw new AssertionError("起点没有可供移动测试的地格");
            });
            waitFor(() -> onGame(() -> Dungeon.hero.pos == destination && Dungeon.hero.ready), 20);
            screenshot("03-地牢开局.png");
            onGame(() -> {
                assertTrue(CowBell.stomp(Dungeon.hero));
                assertEquals(0, Dungeon.hero.buff(Stubbornness.class).charge());
                return null;
            });
            waitFor(() -> onGame(() -> Dungeon.hero.ready), 20);

            onGame(() -> {
                Hero hero = Dungeon.hero;
                TengusMask mask = new TengusMask();
                mask.collect();
                mask.execute(hero, "WEAR");
                return null;
            });
            screenshot("04-两条转职路线.png");
            onGame(() -> {
                ArrayList<Object> children = field(Game.scene(), "members");
                for (Object child : new ArrayList<>(children)) {
                    if (child instanceof Window) ((Window)child).hide();
                }
                Dungeon.hero.belongings.getItem(TengusMask.class).choose(HeroSubClass.HERDGUARD);
                return null;
            });
            waitFor(() -> onGame(() -> Dungeon.hero.ready), 20);
            onGame(() -> {
                Hero hero = Dungeon.hero;
                assertEquals(HeroSubClass.HERDGUARD, hero.subClass);
                ClassArmor armor = ClassArmor.upgrade(hero, hero.belongings.armor);
                assertTrue(armor instanceof CowArmor);
                hero.belongings.armor = armor;
                armor.activate(hero);
                ((com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite)hero.sprite).updateArmor();
                assertEquals(3, HeroClass.COW.armorAbilities().length);
                hero.armorAbility = new PastureWard();
                Talent.initArmorTalents(hero);
                armor.charge = 100;
                hero.armorAbility.use(armor, hero);
                assertTrue(armor.charge < 100);
                assertNotNull(hero.buff(PastureField.class));
                return null;
            });
            waitFor(() -> onGame(() -> Dungeon.hero.ready), 20);
            onGame(() -> {
                Hero hero = Dungeon.hero;
                hero.armorAbility = new MidnightSun();
                Talent.initArmorTalents(hero);
                ClassArmor armor = (ClassArmor)hero.belongings.armor;
                armor.charge = 100;
                hero.armorAbility.use(armor, hero);
                assertTrue(armor.charge < 100);
                assertNotNull(hero.buff(NightShift.class));
                return null;
            });
            waitFor(() -> onGame(() -> Dungeon.hero.ready), 20);
            screenshot("05-专属护甲与增益.png");

            final int savedPosition = onGame(() -> {
                Dungeon.saveAll();
                int pos = Dungeon.hero.pos;
                ShatteredPixelDungeon.switchNoFade(TitleScene.class);
                return pos;
            });
            waitFor(() -> onGame(() -> Game.scene() instanceof TitleScene), 20);
            onGame(() -> {
                Dungeon.hero = null;
                GamesInProgress.curSlot = 99;
                InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
                ShatteredPixelDungeon.switchNoFade(InterlevelScene.class);
                return null;
            });
            waitFor(() -> onGame(() -> Game.scene() instanceof GameScene && Dungeon.hero != null && Dungeon.hero.ready), 60);
            onGame(() -> {
                assertEquals(HeroClass.COW, Dungeon.hero.heroClass);
                assertEquals(HeroSubClass.HERDGUARD, Dungeon.hero.subClass);
                assertEquals(savedPosition, Dungeon.hero.pos);
                assertTrue(Dungeon.hero.armorAbility instanceof MidnightSun);
                assertTrue(Dungeon.hero.belongings.armor instanceof CowArmor);
                assertNotNull(Dungeon.hero.buff(Stubbornness.class));
                assertNotNull(Dungeon.hero.buff(NightShift.class));
                assertNotNull(Dungeon.hero.belongings.getItem(CowBell.class));
                assertFalse(Dungeon.hero.belongings.weapon.info().contains(Messages.NO_TEXT_FOUND));
                return null;
            });
            screenshot("06-存档恢复.png");
        }
    }

    private void screenshot(String name) throws Exception {
        // 等待两个真实渲染回调，让刚打开的窗口先完成绘制；避免慢速设备截到上一场景。
        onGame(() -> null);
        onGame(() -> null);
        Thread.sleep(400);
        File directory = new File(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getExternalFilesDir(null), "cow-smoke");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        assertTrue(device.takeScreenshot(new File(directory, name)));
    }

    private static Point center(StyledButton button) {
        // 选人按钮延伸到系统底部安全区域；点击上方可见的图标部分。
        return button.camera().cameraToScreen(button.left() + button.width()/2f,
                button.top() + Math.min(button.height(), 24f)/2f);
    }

    @SuppressWarnings("unchecked") private static <T> T field(Object object, String name) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return (T)field.get(object);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static <T> T onGame(Callable<T> work) throws Exception {
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch complete = new CountDownLatch(1);
        Gdx.app.postRunnable(() -> {
            try { value.set(work.call()); }
            catch (Throwable error) { failure.set(error); }
            finally { complete.countDown(); }
        });
        // 首次启动需要载入中文字体和音效；无硬件加速的兼容性模拟器耗时更长。
        assertTrue("游戏渲染线程在60秒内没有响应", complete.await(60, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError("游戏线程验证失败", failure.get());
        return value.get();
    }

    private static void waitFor(Callable<Boolean> condition, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        fail("等待游戏状态超时：" + seconds + " 秒");
    }
}
