/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** One short opportunity to alternate bell actions, consumed after a completed pair. */
public class CowBellCombo extends FlavourBuff {
    public static final int CHARGE = 1;
    public static final int STOMP = 2;
    public static final float WINDOW = 6f;

    private int previousSkill;
    private int depth;
    private int branch;

    { type = buffType.POSITIVE; }

    public static boolean canFinish(Hero hero, int skill) {
        if (hero == null || CowLegend.stage(hero) < 2 || !validSkill(skill)) return false;
        CowBellCombo combo = hero.buff(CowBellCombo.class);
        return combo != null && combo.cooldown() > 0 && combo.depth == Dungeon.depth
                && combo.branch == Dungeon.branch && validSkill(combo.previousSkill)
                && combo.previousSkill != skill;
    }

    /** Call only after real enemy HP or shielding was lost, never merely after a cast. */
    public static boolean onSkillHit(Hero hero, int skill) {
        if (hero == null || !hero.isAlive() || CowLegend.stage(hero) < 2 || !validSkill(skill)) return false;
        boolean finished = canFinish(hero, skill);
        Buff.detach(hero, CowBellCombo.class);
        if (!finished) {
            CowBellCombo combo = Buff.prolong(hero, CowBellCombo.class, WINDOW);
            combo.previousSkill = skill;
            combo.depth = Dungeon.depth;
            combo.branch = Dungeon.branch;
        }
        return finished;
    }

    private static boolean validSkill(int skill) { return skill == CHARGE || skill == STOMP; }

    @Override public int icon() { return BuffIndicator.FURY; }
    @Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.8f, 1f); }
    @Override public String desc() {
        String followup = Messages.get(this, previousSkill == CHARGE ? "follow_stomp" : "follow_charge");
        return Messages.get(this, "desc", dispTurns(), followup);
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("cow_bell_previous", previousSkill);
        bundle.put("cow_bell_depth", depth);
        bundle.put("cow_bell_branch", branch);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        previousSkill = bundle.getInt("cow_bell_previous");
        depth = bundle.getInt("cow_bell_depth");
        branch = bundle.getInt("cow_bell_branch");
    }
}
