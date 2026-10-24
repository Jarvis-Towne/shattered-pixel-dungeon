/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowCraft;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PastureField;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

public class PastureWard extends ArmorAbility {
    { baseChargeUse = 35f; }
    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (armor == null || hero == null || !hero.isAlive() || hero.paralysed > 0
                || Dungeon.level == null || armor.charge < chargeUse(hero)) return;
        Buff.affect(hero, PastureField.class).setup(hero);
        armor.charge -= chargeUse(hero);
        Item.updateQuickslot();
        if (hero.sprite != null) {
            hero.sprite.operate(hero.pos);
            SpellSprite.show(hero, SpellSprite.PURITY);
            Sample.INSTANCE.play(Assets.Sounds.CHARMS);
        }
        Invisibility.dispel();
        hero.spendAndNext(Actor.TICK);
    }
    @Override public int icon() { return HeroIcon.COW_PASTURE_WARD; }
    @Override public String desc() {
        return super.desc() + "\n\n" + CowCraft.progress(Dungeon.hero, CowCraft.PASTURE)
                + "\n" + Messages.get(this, "evolution");
    }
    @Override public Talent[] talents() { return new Talent[]{Talent.COW_WIDE_PASTURE, Talent.COW_LIVING_FENCE, Talent.COW_RESTFUL_FIELD, Talent.HEROIC_ENERGY}; }
}
