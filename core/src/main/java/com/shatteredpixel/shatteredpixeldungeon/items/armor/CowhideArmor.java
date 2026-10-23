/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class CowhideArmor extends ClothArmor {
    { image = ItemSpriteSheet.COW_HIDE; unique = true; bones = false; }
    @Override public int proc(Char attacker, Char defender, int damage) {
        Stubbornness stubborn = defender.buff(Stubbornness.class);
        if (defender instanceof Hero && ((Hero)defender).heroClass == HeroClass.COW
                && stubborn != null && stubborn.charge() >= 3 && damage > 0) {
            damage = Math.max(0, damage - 1);
        }
        return super.proc(attacker, defender, damage);
    }
}
