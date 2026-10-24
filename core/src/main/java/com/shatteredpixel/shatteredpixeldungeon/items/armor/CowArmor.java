package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** 星牧战铠；防御数值沿用被皇冠升级的原护甲。 */
public class CowArmor extends ClassArmor {
    { image = ItemSpriteSheet.COW_ARMOR; }

    @Override public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (!super.doUnequip(hero, collect, single)) return false;
        CowLegend legend = hero.buff(CowLegend.class);
        if (legend != null) legend.clearBlame();
        return true;
    }

    @Override public int proc(Char attacker, Char defender, int damage) {
        return super.proc(attacker, defender, CowhideArmor.apronDefense(attacker, defender, damage));
    }

    @Override public String desc() {
        int stage = Dungeon.hero != null && Dungeon.hero.belongings.contains(this) ? CowLegend.stage(Dungeon.hero) : 0;
        return super.desc() + "\n\n" + Messages.get(this, "inheritance")
                + "\n\n" + Messages.get(CowhideArmor.class, "stage_" + stage);
    }
}
