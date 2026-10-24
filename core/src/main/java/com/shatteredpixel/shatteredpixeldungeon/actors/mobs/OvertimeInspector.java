/* 犟牛地牢扩展，遵循 GPL-3.0。借用原版豺狼人动画，不修改专属美术素材。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfBullishResolve;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfCowCouncil;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** A rare, telegraphed risk/reward encounter from the one-time per-depth bell draw.
 * Not included in global spawner tables, so other classes' dungeon balance is unchanged. */
public class OvertimeInspector extends Gnoll {
    {
        EXP = 4;
        maxLvl = 99;
        loot = PotionOfBullishResolve.class;
        lootChance = 1f;
    }

    public OvertimeInspector() {
        super();
        int danger = Math.max(0, Dungeon.depth - 5);
        HP = HT = 18 + danger*2;
        defenseSkill = 7 + danger/2;
    }

    @Override public int damageRoll() {
        return Random.NormalIntRange(2 + Dungeon.depth/6, 6 + Dungeon.depth/4);
    }

    @Override public int attackSkill(Char target) {
        return 10 + Dungeon.depth/3;
    }

    @Override public int attackProc(Char enemy, int damage) {
        damage = super.attackProc(enemy, damage);
        if (damage > 0 && enemy instanceof Hero && Random.Int(4) == 0) {
            Buff.prolong(enemy, Weakness.class, 2f);
        }
        return damage;
    }

    @Override public Item createLoot() {
        return Random.Int(2) == 0 ? new PotionOfBullishResolve() : new ScrollOfCowCouncil();
    }

    /** Spawn nearby but outside the hero's tile; fail safely in very narrow rooms. */
    public static boolean summon(Hero hero) {
        if (hero == null || !hero.isAlive() || Dungeon.level == null) return false;
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int cell = 0; cell < Dungeon.level.length(); cell++) {
            if (!Dungeon.level.insideMap(cell) || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]
                    || Actor.findChar(cell) != null) continue;
            int distance = Dungeon.level.distance(hero.pos, cell);
            if (distance < 1 || distance > 2) continue;
            if (hero.fieldOfView == null || !hero.fieldOfView[cell]) continue;
            if (new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID).collisionPos != cell) continue;
            candidates.add(cell);
        }
        if (candidates.isEmpty()) return false;
        OvertimeInspector enemy = new OvertimeInspector();
        enemy.pos = Random.element(candidates);
        enemy.state = enemy.HUNTING;
        enemy.beckon(hero.pos);
        GameScene.add(enemy);
        Dungeon.observe();
        return true;
    }
}
