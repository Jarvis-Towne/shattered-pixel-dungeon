/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** A growing short horn hammer; its extra damage rewards preserving some resolve. */
public class OxHorn extends MeleeWeapon {
    {
        image = ItemSpriteSheet.COW_HORN;
        tier = 1;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 0.9f;
        unique = true;
        bones = false;
    }
    @Override public int min(int level) { return 2 + level; }
    @Override public int max(int level) { return 8 + 2*level; }
    @Override public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero && ((Hero)attacker).heroClass == HeroClass.COW) {
            Hero hero = (Hero)attacker;
            Stubbornness stubborn = hero.buff(Stubbornness.class);
            damage += hero.lvl/6 + hero.pointsInTalent(Talent.COW_HORN_POLISH);
            if (stubborn != null) damage += stubborn.charge()/3;
        }
        return super.proc(attacker, defender, damage);
    }
}
