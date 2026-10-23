/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** Finite healing: repeated uses refresh to the greater remaining dose, never multiply it. */
public class Rumination extends Buff {
    private int remaining;
    { type = buffType.POSITIVE; }
    public void setHeal(int amount) {
        if (Dungeon.isChallenged(Challenges.NO_HEALING)) amount = 0;
        remaining = Math.max(remaining, Math.max(0, amount));
    }
    public int remaining() { return remaining; }
    @Override public boolean act() {
        if (remaining <= 0 || Dungeon.isChallenged(Challenges.NO_HEALING)) { detach(); return true; }
        if (target.HP < target.HT) {
            target.HP++;
            if (target.sprite != null) target.sprite.showStatus(CharSprite.POSITIVE, "+1");
        }
        remaining--;
        if (remaining == 0) detach();
        spend(TICK);
        return true;
    }
    @Override public int icon() { return BuffIndicator.HERB_HEALING; }
    @Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.94f, 0.75f); }
    @Override public String iconTextDisplay() { return Integer.toString(remaining); }
    @Override public String desc() { return Messages.get(this, "desc", remaining); }
    @Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("cow_healing", remaining); }
    @Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); remaining = Math.max(0, b.getInt("cow_healing")); }
}
