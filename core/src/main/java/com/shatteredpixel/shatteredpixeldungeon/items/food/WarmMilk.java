/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Rumination;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class WarmMilk extends Food {
    { image = ItemSpriteSheet.COW_MILK; energy = Hunger.HUNGRY/2; bones = false; }
    @Override protected void satisfy(Hero hero) {
        super.satisfy(hero);
        Buff.affect(hero, Rumination.class).setHeal(5 + 3*hero.pointsInTalent(Talent.COW_WARM_MILK));
    }
    @Override protected void eatSFX() { Sample.INSTANCE.play(Assets.Sounds.DRINK); }
    @Override public int value() { return 12*quantity(); }
}
