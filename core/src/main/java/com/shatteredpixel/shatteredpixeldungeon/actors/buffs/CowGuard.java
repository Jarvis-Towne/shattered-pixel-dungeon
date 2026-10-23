/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** A non-stacking, time-limited pasture shield. */
public class CowGuard extends ShieldBuff {
    private int turns;
    { type = buffType.POSITIVE; }

    public void set(int shield, int duration) {
        setShield(Math.max(0, shield));
        turns = Math.max(turns, duration);
    }

    @Override public boolean act() {
        if (--turns <= 0 || shielding() <= 0) detach();
        spend(TICK);
        return true;
    }
    @Override public int icon() { return BuffIndicator.ARMOR; }
    @Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 1f, 0.65f); }
    @Override public String iconTextDisplay() { return Integer.toString(shielding()); }
    @Override public String desc() { return Messages.get(this, "desc", shielding(), turns); }
    @Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("cow_guard_turns", turns); }
    @Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); turns = b.getInt("cow_guard_turns"); }
}
