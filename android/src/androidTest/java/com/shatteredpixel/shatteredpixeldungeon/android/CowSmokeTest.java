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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowJournal;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.WarmMilk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndCowNotes;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
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

/** 仅在测试模拟器运行，使用独立存档槽99；后期状态由测试注入，不代表自然通关。 */
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

            // 直接注入后期条件，验证真实 Android 界面、技能执行和存档；成长来源由核心测试覆盖。
            final Rat debateTarget = onGame(() -> {
                Hero hero = Dungeon.hero;
                assertEquals(0, CowLegend.stage(hero));
                hero.lvl = 14;
                CowLegend legend = CowLegend.get(hero);
                legend.addPoints(90 - legend.points());
                assertEquals(3, CowLegend.stage(hero));
                assertEquals(90, legend.points());
                assertChinese(CowLegend.get(hero).progress());
                hero.buff(Stubbornness.class).gain(Stubbornness.capacity(hero));
                System.out.println("后期条件由测试注入：等级14、90历练；不属于通关或自然成长证明。");
                return addRootedDebateTarget(hero);
            });

            WndInfoItem weaponWindow = onGame(() -> {
                Item weapon = Dungeon.hero.belongings.weapon;
                assertChinese(weapon.name());
                assertChinese(weapon.info());
                WndInfoItem window = new WndInfoItem(weapon);
                GameScene.show(window);
                assertWindowOnScreen(window);
                return window;
            });
            screenshot("06-进化武器中文说明.png");
            onGame(() -> { weaponWindow.hide(); return null; });

            WndUseItem bellWindow = onGame(() -> showBellMenu());
            screenshot("07-进化牛铃与动作菜单.png");
            final int practiceBeforeGuide = onGame(() -> CowLegend.get(Dungeon.hero).points());
            final int resolveBeforeGuide = onGame(() -> Dungeon.hero.buff(Stubbornness.class).charge());
            final float timeBeforeGuide = onGame(() -> Actor.now());
            Point guideButton = onGame(() -> itemActionButton(bellWindow, CowBell.AC_GUIDE));
            assertTrue(device.click(guideButton.x, guideButton.y));
            waitFor(() -> onGame(() -> bellWindow.parent == null && visibleNotes() != null), 20);
            onGame(() -> {
                assertChinese(CowBell.guide(Dungeon.hero));
                assertWindowOnScreen(visibleNotes());
                return null;
            });
            screenshot("08-成长手册.png");
            onGame(() -> {
                closeWindows();
                assertEquals("查看手册不得获得历练", practiceBeforeGuide, CowLegend.get(Dungeon.hero).points());
                assertEquals("查看手册不得消耗倔劲", resolveBeforeGuide,
                        Dungeon.hero.buff(Stubbornness.class).charge());
                assertEquals("查看手册不得消耗游戏回合", timeBeforeGuide, Actor.now(), 0f);
                return null;
            });

            onGame(() -> {
                CowJournal journal = CowJournal.get(Dungeon.hero);
                for (int entry = 0; entry < CowJournal.TOTAL; entry++) journal.discover(Dungeon.hero, entry);
                assertEquals(18, journal.count());
                System.out.println("相册18项记录由测试注入，仅验证长文本布局、滚动及存档，不代表自然解锁。");
                return null;
            });
            WndUseItem albumMenu = onGame(() -> showBellMenu());
            Point albumButton = onGame(() -> itemActionButton(albumMenu, CowBell.AC_ALBUM));
            assertTrue(device.click(albumButton.x, albumButton.y));
            waitFor(() -> onGame(() -> albumMenu.parent == null && visibleNotes() != null), 20);
            final ScrollPane albumPane = onGame(() -> {
                WndCowNotes notes = visibleNotes();
                assertWindowOnScreen(notes);
                assertChinese(CowJournal.album(Dungeon.hero));
                ScrollPane pane = notesPane(notes);
                assertTrue("完整相册必须容纳于可滚动内容区", pane.content().height() > pane.height());
                return pane;
            });
            screenshot("09-牛言牛语首屏.png");
            Point swipeStart = onGame(() -> albumPane.camera().cameraToScreen(
                    albumPane.left() + albumPane.width()/2f, albumPane.top() + albumPane.height()*0.8f));
            Point swipeEnd = onGame(() -> albumPane.camera().cameraToScreen(
                    albumPane.left() + albumPane.width()/2f, albumPane.top() + albumPane.height()*0.2f));
            assertTrue(device.swipe(swipeStart.x, swipeStart.y, swipeEnd.x, swipeEnd.y, 20));
            waitFor(() -> onGame(() -> albumPane.content().camera.scroll.y > 0), 20);
            onGame(() -> {
                albumPane.scrollTo(0, albumPane.content().height());
                assertEquals("相册末尾应可滚动到达", albumPane.content().height() - albumPane.height(),
                        albumPane.content().camera.scroll.y, 0.5f);
                assertWindowOnScreen(visibleNotes());
                return null;
            });
            screenshot("10-牛言牛语末屏.png");
            onGame(() -> {
                closeWindows();
                assertEquals(practiceBeforeGuide, CowLegend.get(Dungeon.hero).points());
                assertEquals(resolveBeforeGuide, Dungeon.hero.buff(Stubbornness.class).charge());
                assertEquals(timeBeforeGuide, Actor.now(), 0f);
                return null;
            });

            final int targetHealth = onGame(() -> debateTarget.HP);
            final int resolveBeforeDebate = onGame(() -> Dungeon.hero.buff(Stubbornness.class).charge());
            WndUseItem debateMenu = onGame(() -> showBellMenu());
            Point debateButton = onGame(() -> itemActionButton(debateMenu, CowBell.AC_DEBATE));
            assertTrue(device.click(debateButton.x, debateButton.y));
            waitFor(() -> onGame(() -> debateMenu.parent == null && Dungeon.hero.ready
                    && Dungeon.hero.buff(Stubbornness.class).charge() == resolveBeforeDebate - CowBell.DEBATE_COST), 30);
            onGame(() -> {
                assertTrue("据理力争必须实际伤害敌人", debateTarget.HP < targetHealth);
                assertNotNull(debateTarget.buff(Weakness.class));
                return null;
            });
            screenshot("11-据理力争技能施放.png");
            onGame(() -> {
                // 清除测试目标而不结算击杀，避免它影响存档恢复的状态断言。
                debateTarget.alignment = Char.Alignment.NEUTRAL;
                debateTarget.destroy();
                if (debateTarget.sprite != null) debateTarget.sprite.killAndErase();
                return null;
            });

            final int savedPractice = onGame(() -> CowLegend.get(Dungeon.hero).points());
            final String savedProgress = onGame(() -> CowLegend.get(Dungeon.hero).progress());
            final int savedResolve = onGame(() -> Dungeon.hero.buff(Stubbornness.class).charge());
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
                assertEquals(14, Dungeon.hero.lvl);
                assertEquals(3, CowLegend.stage(Dungeon.hero));
                assertEquals(savedPractice, CowLegend.get(Dungeon.hero).points());
                assertEquals(savedProgress, CowLegend.get(Dungeon.hero).progress());
                assertEquals(savedResolve, Dungeon.hero.buff(Stubbornness.class).charge());
                assertTrue(Dungeon.hero.belongings.getItem(CowBell.class).actions(Dungeon.hero).contains(CowBell.AC_DEBATE));
                assertEquals(18, CowJournal.get(Dungeon.hero).count());
                for (int entry = 0; entry < CowJournal.TOTAL; entry++) {
                    assertTrue(CowJournal.get(Dungeon.hero).found(entry));
                }
                return null;
            });
            screenshot("12-成长与彩蛋存档恢复.png");
        }
    }

    private static Rat addRootedDebateTarget(Hero hero) {
        for (int cell = 0; cell < Dungeon.level.length(); cell++) {
            if (!Dungeon.level.insideMap(cell) || !Dungeon.level.passable[cell]
                    || Dungeon.level.pit[cell] || !hero.fieldOfView[cell]
                    || Dungeon.level.distance(hero.pos, cell) != 2
                    || Dungeon.level.traps.get(cell) != null || Actor.findChar(cell) != null) continue;
            if (new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != cell) continue;
            Rat target = new Rat();
            target.HP = target.HT = 100;
            target.pos = cell;
            target.state = target.PASSIVE;
            GameScene.add(target);
            Buff.prolong(target, Roots.class, 100f);
            return target;
        }
        throw new AssertionError("未找到距离2、可见且安全的技能测试目标位置");
    }

    private static WndUseItem showBellMenu() {
        CowBell bell = Dungeon.hero.belongings.getItem(CowBell.class);
        assertNotNull(bell);
        assertChinese(bell.name());
        assertChinese(bell.info());
        assertTrue(bell.actions(Dungeon.hero).contains(CowBell.AC_DEBATE));
        assertTrue(bell.actions(Dungeon.hero).contains(CowBell.AC_GUIDE));
        for (String action : bell.actions(Dungeon.hero)) assertChinese(bell.actionName(action, Dungeon.hero));
        WndUseItem window = new WndUseItem(null, bell);
        GameScene.show(window);
        assertWindowOnScreen(window);
        return window;
    }

    private static WndCowNotes visibleNotes() throws Exception {
        ArrayList<Object> children = field(Game.scene(), "members");
        for (Object child : children) {
            if (child instanceof WndCowNotes && ((WndCowNotes)child).visible) return (WndCowNotes)child;
        }
        return null;
    }

    private static ScrollPane notesPane(WndCowNotes window) throws Exception {
        ArrayList<Object> children = field(window, "members");
        for (Object child : children) if (child instanceof ScrollPane) return (ScrollPane)child;
        throw new AssertionError("长文本窗口缺少滚动区");
    }

    private static void assertWindowOnScreen(Window window) {
        assertNotNull(window);
        Camera camera = window.camera;
        assertTrue("窗口宽度超过可用视口", camera.width <= PixelScene.uiCamera.width);
        assertTrue("窗口高度超过可用视口", camera.height <= PixelScene.uiCamera.height);
        assertTrue("窗口左边缘超出屏幕", camera.x >= 0);
        assertTrue("窗口上边缘超出屏幕", camera.y >= 0);
        assertTrue("窗口右边缘超出屏幕", camera.x + camera.width * camera.zoom <= Game.width + 1);
        assertTrue("窗口下边缘超出屏幕", camera.y + camera.height * camera.zoom <= Game.height + 1);
    }

    private static Point itemActionButton(WndUseItem window, String action) throws Exception {
        CowBell bell = Dungeon.hero.belongings.getItem(CowBell.class);
        String label = bell.actionName(action, Dungeon.hero);
        ArrayList<Object> children = field(window, "members");
        for (Object child : children) {
            if (!(child instanceof StyledButton)) continue;
            StyledButton button = (StyledButton)child;
            if (!label.equals(button.text()) || !button.visible || !button.active) continue;
            Point point = button.camera().cameraToScreen(button.left() + button.width()/2f,
                    button.top() + button.height()/2f);
            assertTrue("动作按钮必须位于屏幕内", point.x >= 0 && point.x < Game.width
                    && point.y >= 0 && point.y < Game.height);
            return point;
        }
        throw new AssertionError("道具菜单缺少可见动作按钮：" + label);
    }

    static void closeWindows() throws Exception {
        ArrayList<Object> children = field(Game.scene(), "members");
        for (Object child : new ArrayList<>(children)) {
            if (child instanceof Window) ((Window)child).hide();
        }
    }

    static void assertChinese(String text) {
        assertNotNull(text);
        assertFalse(text, text.contains(Messages.NO_TEXT_FOUND));
        // Android 9's ICU regex does not recognize IsHan. Check the CJK ideograph
        // ranges directly so the assertion still requires actual Chinese text.
        boolean containsChinese = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if ((character >= 0x3400 && character <= 0x4DBF)
                    || (character >= 0x4E00 && character <= 0x9FFF)
                    || (character >= 0xF900 && character <= 0xFAFF)) {
                containsChinese = true;
                break;
            }
        }
        assertTrue("新增界面内容必须显示中文：" + text, containsChinese);
    }

    static void screenshot(String name) throws Exception {
        // 等待两个真实渲染回调，让刚打开的窗口先完成绘制；避免慢速设备截到上一场景。
        onGame(() -> null);
        onGame(() -> null);
        Thread.sleep(400);
        File directory = new File(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getExternalFilesDir(null), "cow-smoke");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                .takeScreenshot(new File(directory, name)));
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

    static <T> T onGame(Callable<T> work) throws Exception {
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

    static void waitFor(Callable<Boolean> condition, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        fail("等待游戏状态超时：" + seconds + " 秒");
    }
}
