/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class CowRush extends FlavourBuff {
    { type = buffType.POSITIVE; }
    public static int boost(Hero hero) { return 2 + hero.lvl/5 + 2*hero.pointsInTalent(Talent.COW_RUSHING_FORCE); }
    public int consume(Hero hero) { int amount = boost(hero); detach(); return amount; }
    @Override public int icon() { return BuffIndicator.FURY; }
    @Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.45f, 0.20f); }
    @Override public String desc() { return Messages.get(this, "desc", target instanceof Hero ? boost((Hero)target) : 2, dispTurns()); }
}
