/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/** Attack damage is applied in Stubbornness.onHit; movement in Hero.speed. */
public class NightShift extends FlavourBuff {
    { type = buffType.POSITIVE; }
    @Override public int icon() { return BuffIndicator.LIGHT; }
    @Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.82f, 0.25f); }
}
