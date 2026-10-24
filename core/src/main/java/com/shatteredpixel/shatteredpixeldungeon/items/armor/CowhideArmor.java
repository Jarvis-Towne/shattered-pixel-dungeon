/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class CowhideArmor extends ClothArmor {
    { image = ItemSpriteSheet.COW_HIDE; unique = true; bones = false; }

    /** Actual armor tier evolves with the cow; glyphs and scroll upgrades remain intact. */
    public void evolveTo(int stage) {
        tier = Math.max(tier, Math.min(4, Math.max(0, stage) + 1));
    }

    @Override public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (!super.doUnequip(hero, collect, single)) return false;
        CowLegend legend = hero.buff(CowLegend.class);
        if (legend != null) legend.clearBlame();
        return true;
    }

    @Override public int proc(Char attacker, Char defender, int damage) {
        return super.proc(attacker, defender, apronDefense(attacker, defender, damage));
    }

    /** Shared by the starting apron and its crowned successor; does not alter armor tier or upgrades. */
    public static int apronDefense(Char attacker, Char defender, int damage) {
        Stubbornness stubborn = defender.buff(Stubbornness.class);
        if (defender instanceof Hero && ((Hero)defender).heroClass == HeroClass.COW
                && CowLegend.wearingApron((Hero) defender) && attacker != null
                && attacker.alignment == Char.Alignment.ENEMY && stubborn != null
                && stubborn.charge() >= 3 && damage > 0) {
            damage = Math.max(0, damage - 1);
            // From the second form onward, the work apron becomes genuine heavy armor.
            // Extra mitigation only applies to substantial direct enemy hits.
            int stage = CowLegend.stage((Hero) defender);
            if (stage >= 2 && damage >= 7) {
                damage = Math.max(0, damage - Math.min(stage - 1, damage/7));
            }
        }
        return damage;
    }

    private int ownerStage() {
        return Dungeon.hero != null && Dungeon.hero.belongings.contains(this) ? CowLegend.stage(Dungeon.hero) : 0;
    }

    @Override public String name() {
        int stage = ownerStage();
        return stage == 0 ? super.name() : super.name().replace(trueName(), Messages.get(this, "name_" + stage));
    }

    @Override public String desc() {
        String description = super.desc() + "\n\n" + Messages.get(CowhideArmor.class, "stage_" + ownerStage());
        if (Dungeon.hero != null && Dungeon.hero.belongings.contains(this)) {
            CowLegend legend = Dungeon.hero.buff(CowLegend.class);
            if (legend != null) description += "\n\n" + legend.progress();
        }
        return description;
    }
}
