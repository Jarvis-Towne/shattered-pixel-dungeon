/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowCraft;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class Stampede extends ArmorAbility {
    { baseChargeUse = 35f; }
    @Override public String targetingPrompt() { return Messages.get(this, "prompt"); }
    public static int range(Hero hero) { return 5 + hero.pointsInTalent(Talent.COW_LONG_RUSH); }
    public static int damage(Hero hero) { return Math.round((10 + hero.lvl + CowCraft.growth(hero, CowCraft.STAMPEDE))*(1f + 0.15f*hero.pointsInTalent(Talent.COW_THUNDER_HOOVES))); }

    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (target == null || armor == null || hero == null || !hero.isAlive()
                || hero.paralysed > 0 || Dungeon.level == null || armor.charge < chargeUse(hero)) return;
        if (target == hero.pos || !Dungeon.level.insideMap(target)
                || hero.fieldOfView == null || !hero.fieldOfView[target]) {
            GLog.w(Messages.get(this, "bad_target")); return;
        }
        Ballistica line = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
        int distance = Math.min(range(hero), line.dist);
        if (distance < 1) { GLog.w(Messages.get(this, "bad_target")); return; }
        armor.charge -= chargeUse(hero);
        int mastery = CowCraft.rank(hero, CowCraft.STAMPEDE);
        int hit = 0;
        for (int i = 1; i <= distance; i++) {
            int cell = line.path.get(i);
            Char enemy = Actor.findChar(cell);
            if (enemy != null && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY) {
                int before = enemy.HP + enemy.shielding();
                enemy.damage(Math.max(1, damage(hero) - enemy.drRoll()), hero);
                if (enemy.isAlive()) Buff.prolong(enemy, Cripple.class, 4f);
                if (enemy.HP + enemy.shielding() < before) {
                    if (hit == 0 && mastery >= 1 && enemy.isAlive()) Buff.prolong(enemy, Weakness.class, 2f);
                    if (mastery >= 3 && enemy.isAlive()) Buff.prolong(enemy, Vulnerable.class, 2f);
                    hit++;
                }
            }
            if (hero.sprite != null && Dungeon.level.heroFOV[cell]) {
                CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
            }
        }
        if (mastery >= 2 && hit >= 2) Buff.affect(hero, CowGuard.class).set(2, 4);
        CowCraft.record(hero, CowCraft.STAMPEDE, hit > 0);
        // Fully practiced thunder hooves recover a small part of their own charge,
        // only if real enemies lost HP or shielding. No reward on an empty cast.
        int skillRefund = mastery >= 3 && hit >= 2 ? Math.min(6, 2*hit) : 0;
        armor.charge = Math.min(100, armor.charge
                + Math.min(12, hit*hero.pointsInTalent(Talent.COW_CHAIN_RUSH)) + skillRefund);
        Item.updateQuickslot();
        if (hero.sprite != null) {
            hero.sprite.operate(hero.pos);
            Sample.INSTANCE.play(Assets.Sounds.BLAST);
            PixelScene.shake(2, 0.4f);
        }
        Invisibility.dispel();
        hero.spendAndNext(Actor.TICK);
    }

    @Override public int icon() { return HeroIcon.COW_STAMPEDE; }
    @Override public String desc() {
        return super.desc() + "\n\n" + CowCraft.progress(Dungeon.hero, CowCraft.STAMPEDE)
                + "\n" + Messages.get(this, "evolution");
    }
    @Override public Talent[] talents() { return new Talent[]{Talent.COW_LONG_RUSH, Talent.COW_THUNDER_HOOVES, Talent.COW_CHAIN_RUSH, Talent.HEROIC_ENERGY}; }
}
