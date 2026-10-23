/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MeadowCake extends Food {
    { image = ItemSpriteSheet.COW_SNACK; energy = Hunger.HUNGRY/2; bones = false; }
    @Override protected void satisfy(Hero hero) {
        super.satisfy(hero);
        Buff.affect(hero, CowGuard.class).set(4, 8);
    }
    @Override public int value() { return 12*quantity(); }
}
