/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** Attack damage is applied in Stubbornness.onHit; movement in Hero.speed. */
public class NightShift extends FlavourBuff {
    private boolean practiced;
    { type = buffType.POSITIVE; }

    public void beginPractice() { practiced = false; }

    /** Called only after an ordinary attack really lowers hostile HP or shielding. */
    public void onEffectiveHit(Hero hero) {
        if (!practiced) {
            practiced = true;
            CowCraft.record(hero, CowCraft.NIGHT, true);
        }
    }

    @Override public int icon() { return BuffIndicator.LIGHT; }
    @Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.82f, 0.25f); }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("night_practiced", practiced);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        // An existing v1.0.0 buff is not a newly paid activation.
        practiced = !bundle.contains("night_practiced") || bundle.getBoolean("night_practiced");
    }
}
