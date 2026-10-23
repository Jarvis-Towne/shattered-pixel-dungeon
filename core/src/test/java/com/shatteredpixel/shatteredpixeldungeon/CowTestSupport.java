package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 只加载资源与游戏逻辑，不创建窗口、不运行渲染循环。 */
public final class CowTestSupport {
    private static HeadlessApplication application;

    private CowTestSupport() {}

    public static synchronized void init() throws InterruptedException {
        if (application != null) return;
        File directory = new File(System.getProperty("cow.test.directory", "build/test-data"));
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建隔离测试目录：" + directory);
        }
        CountDownLatch ready = new CountDownLatch(1);
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = -1;
        config.preferencesDirectory = directory.getAbsolutePath();
        application = new HeadlessApplication(new ApplicationAdapter() {
            @Override public void create() { ready.countDown(); }
        }, config);
        if (!ready.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("无窗口运行环境未能启动");
        }
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute, directory.getAbsolutePath() + File.separator);
        Game.version = "4.0.2"; // 按正式版本检查解锁，不借用 INDEV 的全解锁。
        Game.versionCode = 922;
        new Game(TitleScene.class, null);
        SPDSettings.language(Languages.CHI_SMPL);
        SPDSettings.challenges(0);
        Messages.setup(Languages.CHI_SMPL);
        Journal.loadGlobal();
    }

    public static Hero freshHero(HeroClass heroClass) {
        Dungeon.level = null;
        Dungeon.daily = false;
        Dungeon.dailyReplay = false;
        Dungeon.customSeedText = "";
        Dungeon.seed = 20261023L;
        SPDSettings.challenges(0);
        GamesInProgress.selectedClass = heroClass;
        Dungeon.init();
        return Dungeon.hero;
    }

    public static Bundle roundTrip(Bundle bundle) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!Bundle.write(bundle, bytes, true)) throw new IOException("存档压缩写入失败");
        return Bundle.read(new ByteArrayInputStream(bytes.toByteArray()));
    }
}
