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

    /** The crown preserves an even higher transferred armor tier, if the player chose one. */
    public void evolveTo(int stage) {
        tier = Math.max(tier, Math.min(4, Math.max(0, stage) + 1));
    }

    private int smooth() {
        Hero hero = Dungeon.hero;
        return hero != null && hero.belongings.contains(this) ? CowLegend.growth(hero) : 0;
    }

    private int ownerStage() {
        Hero hero = Dungeon.hero;
        return hero != null && hero.belongings.contains(this) ? CowLegend.stage(hero) : 0;
    }

    @Override public int STRReq(int level) {
        return Math.max(0, super.STRReq(level) - 2*ownerStage());
    }

    @Override public int DRMax(int level) {
        return super.DRMax(level) +
                (Dungeon.isChallenged(com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_ARMOR) ? 0 : smooth());
    }

    @Override public int DRMin(int level) {
        return Math.min(DRMax(level), super.DRMin(level) +
                (Dungeon.isChallenged(com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_ARMOR) ? 0 : smooth()/2));
    }

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
