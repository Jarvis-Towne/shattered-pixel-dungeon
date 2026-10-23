/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowRush;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** A unique class tool. Kept in the backpack so it does not consume an artifact slot. */
public class CowBell extends Item {
    public static final String AC_CHARGE = "CHARGE";
    public static final String AC_STOMP = "STOMP";
    public static final int COST = 3;
    {
        image = ItemSpriteSheet.COW_BELL;
        defaultAction = AC_CHARGE;
        usesTargeting = true;
        unique = true;
        bones = false;
    }

    @Override public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        // This is a class ability tool, not a tradable artifact.
        actions.remove(AC_DROP);
        actions.remove(AC_THROW);
        if (hero.heroClass == HeroClass.COW) { actions.add(AC_CHARGE); actions.add(AC_STOMP); }
        return actions;
    }

    @Override public void execute(final Hero hero, String action) {
        super.execute(hero, action);
        if (!AC_CHARGE.equals(action) && !AC_STOMP.equals(action)) return;
        if (hero.heroClass != HeroClass.COW) { GLog.w(Messages.get(this, "not_cow")); return; }
        if (Buff.affect(hero, Stubbornness.class).charge() < COST) {
            GLog.w(Messages.get(this, "no_charge", COST)); return;
        }
        if (AC_STOMP.equals(action)) { stomp(hero); return; }
        if (hero.rooted) { GLog.w(Messages.get(this, "rooted")); return; }
        GameScene.selectCell(new CellSelector.Listener() {
            @Override public void onSelect(Integer cell) {
                if (cell != null) charge(hero, cell);
            }
            @Override public String prompt() { return Messages.get(CowBell.class, "prompt"); }
        });
    }

    public static int range(Hero hero) { return 3 + hero.pointsInTalent(Talent.COW_LONG_CHARGE); }
    public static int impactDamage(Hero hero, int travelled) {
        int damage = 4 + hero.lvl + Math.max(0, travelled)*2;
        return hero.subClass == HeroSubClass.BULLDOZER ? Math.round(damage*1.25f) : damage;
    }

    /** Returns -1 for an invalid target; no state or resources are changed during validation. */
    public static int chargeDestination(Hero hero, int cell) {
        if (hero == null || hero.heroClass != HeroClass.COW || !hero.isAlive() || hero.paralysed > 0
                || Dungeon.level == null || !Dungeon.level.insideMap(cell) || hero.rooted
                || hero.fieldOfView == null || !hero.fieldOfView[cell]) return -1;
        Char enemy = Actor.findChar(cell);
        if (enemy == null || enemy == hero || enemy.alignment != Char.Alignment.ENEMY || !enemy.isAlive()) return -1;
        Ballistica path = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
        if (path.collisionPos != cell || path.dist > range(hero) || path.dist < 1) return -1;
        for (int i = 1; i < path.dist; i++) {
            int step = path.path.get(i);
            if (!Dungeon.level.passable[step] || Dungeon.level.pit[step]
                    || Dungeon.level.solid[step] || Actor.findChar(step) != null) return -1;
        }
        return path.path.get(path.dist - 1);
    }

    public static boolean charge(Hero hero, int cell) {
        int destination = chargeDestination(hero, cell);
        if (destination < 0) { GLog.w(Messages.get(CowBell.class, "bad_target")); return false; }
        Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
        if (!stubborn.spendCharge(COST)) { GLog.w(Messages.get(CowBell.class, "no_charge", COST)); return false; }
        Char enemy = Actor.findChar(cell);
        Ballistica path = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
        int from = hero.pos, travelled = 0;
        Level level = Dungeon.level;
        // Press each crossed tile: a dash must not bypass traps, webs or terrain effects.
        for (int i = 1; i < path.dist; i++) {
            int step = path.path.get(i);
            // A previous trap can summon an actor or transform the next tile after validation.
            // Revalidate immediately before moving, without refunding an already begun action.
            if (Dungeon.level != level || !level.passable[step] || level.pit[step]
                    || level.solid[step] || Actor.findChar(step) != null) break;
            hero.move(step, false);
            travelled++;
            if (!hero.isAlive() || Dungeon.level != level || hero.pos != step || hero.rooted || hero.paralysed > 0) break;
        }
        if (hero.sprite != null) {
            hero.sprite.place(hero.pos);
            hero.sprite.turnTo(from, cell);
            Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
            PixelScene.shake(1, 0.2f);
        }
        if (hero.isAlive() && Dungeon.level == level && enemy != null && enemy.isAlive()
                && Dungeon.level.adjacent(hero.pos, enemy.pos) && !hero.rooted && hero.paralysed == 0) {
            int damage = Math.max(1, impactDamage(hero, travelled) - enemy.drRoll());
            enemy.damage(damage, hero);
            if (enemy.isAlive()) Buff.prolong(enemy, Cripple.class, 3f);
            if (hero.subClass == HeroSubClass.BULLDOZER) {
                Buff.prolong(hero, CowRush.class, 3f);
                int splash = hero.pointsInTalent(Talent.COW_AFTERSHOCK);
                if (splash > 0) {
                    for (Char other : new ArrayList<>(Actor.chars())) {
                        if (other != enemy && other.alignment == Char.Alignment.ENEMY
                                && Dungeon.level.distance(cell, other.pos) <= 1) {
                            other.damage(Math.max(1, Math.round(damage*0.2f*splash)), hero);
                        }
                    }
                }
            }
        }
        if (hero.isAlive()) Stubbornness.afterBell(hero);
        Invisibility.dispel();
        Dungeon.observe();
        GameScene.updateFog();
        hero.spendAndNext(Actor.TICK);
        return true;
    }

    public static boolean stomp(Hero hero) {
        if (hero == null || hero.heroClass != HeroClass.COW || !hero.isAlive()
                || hero.paralysed > 0 || Dungeon.level == null) return false;
        Stubbornness stubborn = Buff.affect(hero, Stubbornness.class);
        if (!stubborn.spendCharge(COST)) return false;
        int talent = hero.pointsInTalent(Talent.COW_EARTHSHAKER);
        for (Char enemy : new ArrayList<>(Actor.chars())) {
            if (enemy.alignment == Char.Alignment.ENEMY && Dungeon.level.distance(hero.pos, enemy.pos) <= 1) {
                enemy.damage(Math.max(1, 3 + hero.lvl/2 + 2*talent - enemy.drRoll()), hero);
                if (enemy.isAlive()) Buff.prolong(enemy, Cripple.class, 2f + talent);
            }
        }
        Buff.affect(hero, CowGuard.class).set(2 + hero.lvl/5, 4);
        if (hero.hasTalent(Talent.COW_STEADY_HOOVES)) Buff.detach(hero, Cripple.class);
        if (hero.pointsInTalent(Talent.COW_STEADY_HOOVES) >= 2) Buff.detach(hero, Roots.class);
        Stubbornness.afterBell(hero);
        if (hero.sprite != null) {
            hero.sprite.operate(hero.pos);
            Sample.INSTANCE.play(Assets.Sounds.ROCKS);
            PixelScene.shake(1, 0.3f);
        }
        Invisibility.dispel();
        hero.spendAndNext(Actor.TICK);
        return true;
    }

    @Override public boolean isUpgradable() { return false; }
    @Override public boolean isIdentified() { return true; }
    @Override public int value() { return 0; }
    @Override public String status() {
        Hero hero = Dungeon.hero;
        if (hero == null) return "";
        Stubbornness stubborn = hero.buff(Stubbornness.class);
        return (stubborn == null ? 0 : stubborn.charge()) + "/" + Stubbornness.capacity(hero);
    }
}
