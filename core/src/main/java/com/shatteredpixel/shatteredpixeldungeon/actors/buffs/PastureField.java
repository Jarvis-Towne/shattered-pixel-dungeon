/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/** An anchored field; it is invalidated immediately by a floor or branch transition. */
public class PastureField extends Buff {
    private int center, depth, branch, turns, radius, shieldCap, pulses, mastery;
    private boolean practiced;
    private boolean restoredFooting;
    { type = buffType.POSITIVE; }

    public void setup(Hero hero) {
        center = hero.pos;
        depth = Dungeon.depth;
        branch = Dungeon.branch;
        int wide = hero.pointsInTalent(Talent.COW_WIDE_PASTURE);
        turns = 8 + 2*wide;
        radius = 2 + wide/2;
        mastery = CowCraft.rank(hero, CowCraft.PASTURE);
        shieldCap = 8 + hero.lvl/2 + (mastery >= 1 ? 2 : 0);
        pulses = 0;
        practiced = false;
        restoredFooting = false;
        for (Char ally : new ArrayList<>(Actor.chars())) {
            if (ally.alignment == Char.Alignment.ALLY && contains(ally.pos)) {
                Buff.affect(ally, CowGuard.class).set(shieldCap, turns + 1);
            }
        }
    }

    public boolean contains(int cell) {
        if (Dungeon.level == null || Dungeon.depth != depth || Dungeon.branch != branch
                || !Dungeon.level.insideMap(center) || !Dungeon.level.insideMap(cell)
                || Dungeon.level.distance(center, cell) > radius) return false;
        return new Ballistica(center, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos == cell;
    }

    @Override public boolean act() {
        if (!(target instanceof Hero) || Dungeon.depth != depth || Dungeon.branch != branch || turns <= 0) {
            detach(); return true;
        }
        Hero hero = (Hero)target;
        int fence = hero.pointsInTalent(Talent.COW_LIVING_FENCE);
        boolean enemyPresent = false;
        for (Char ch : new ArrayList<>(Actor.chars())) {
            if (!ch.isAlive() || !contains(ch.pos)) continue;
            if (ch.alignment == Char.Alignment.ENEMY) {
                enemyPresent = true;
                Buff.prolong(ch, Cripple.class, 2f);
                if (fence > 0) ch.damage(fence, hero);
            } else if (ch.alignment == Char.Alignment.ALLY && pulses % 3 == 0) {
                CowGuard guard = Buff.affect(ch, CowGuard.class);
                int restored = 1 + hero.pointsInTalent(Talent.COW_RESTFUL_FIELD);
                guard.set(Math.min(shieldCap, guard.shielding() + restored), 3);
            }
        }
        if (enemyPresent && !practiced) {
            practiced = true;
            CowCraft.record(hero, CowCraft.PASTURE, true);
        }
        if (!restoredFooting && pulses >= 2 && mastery >= 2
                && contains(hero.pos)) {
            restoredFooting = true;
            Buff.detach(hero, Cripple.class);
        }
        if (hero.sprite != null && pulses % 3 == 0) {
            for (int cell = 0; cell < Dungeon.level.length(); cell++) {
                if (Dungeon.level.heroFOV[cell] && contains(cell)
                        && Dungeon.level.distance(center, cell) == radius) {
                    CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 1);
                }
            }
        }
        pulses++;
        turns--;
        spend(TICK);
        return true;
    }
    @Override public int icon() { return BuffIndicator.BARKSKIN; }
    @Override public void tintIcon(Image icon) { icon.hardlight(0.45f, 1f, 0.65f); }
    @Override public String iconTextDisplay() { return Integer.toString(turns); }
    @Override public String desc() { return Messages.get(this, "desc", turns, radius, shieldCap); }
    @Override public void storeInBundle(Bundle b) {
        super.storeInBundle(b);
        b.put("pasture_center", center); b.put("pasture_depth", depth); b.put("pasture_branch", branch);
        b.put("pasture_turns", turns); b.put("pasture_radius", radius); b.put("pasture_cap", shieldCap);
        b.put("pasture_pulses", pulses);
        b.put("pasture_mastery", mastery);
        b.put("pasture_practiced", practiced);
        b.put("pasture_footing", restoredFooting);
    }
    @Override public void restoreFromBundle(Bundle b) {
        super.restoreFromBundle(b);
        center = b.getInt("pasture_center"); depth = b.getInt("pasture_depth"); branch = b.getInt("pasture_branch");
        turns = b.getInt("pasture_turns"); radius = b.getInt("pasture_radius"); shieldCap = b.getInt("pasture_cap");
        pulses = b.getInt("pasture_pulses");
        mastery = Math.max(0, Math.min(2, b.getInt("pasture_mastery")));
        // Old active fields cannot award practice for a cast made before the upgrade.
        practiced = !b.contains("pasture_practiced") || b.getBoolean("pasture_practiced");
        restoredFooting = b.getBoolean("pasture_footing");
    }
}
