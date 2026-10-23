/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cow;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
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
    public static int damage(Hero hero) { return Math.round((10 + hero.lvl)*(1f + 0.15f*hero.pointsInTalent(Talent.COW_THUNDER_HOOVES))); }

    @Override protected void activate(ClassArmor armor, Hero hero, Integer target) {
        if (target == null) return;
        if (target == hero.pos || !Dungeon.level.insideMap(target) || !hero.fieldOfView[target]) {
            GLog.w(Messages.get(this, "bad_target")); return;
        }
        Ballistica line = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
        int distance = Math.min(range(hero), line.dist);
        if (distance < 1) { GLog.w(Messages.get(this, "bad_target")); return; }
        armor.charge -= chargeUse(hero);
        int hit = 0;
        for (int i = 1; i <= distance; i++) {
            int cell = line.path.get(i);
            Char enemy = Actor.findChar(cell);
            if (enemy != null && enemy.alignment == Char.Alignment.ENEMY) {
                enemy.damage(Math.max(1, damage(hero) - enemy.drRoll()), hero);
                if (enemy.isAlive()) Buff.prolong(enemy, Cripple.class, 4f);
                hit++;
            }
            if (hero.sprite != null && Dungeon.level.heroFOV[cell]) {
                CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
            }
        }
        armor.charge = Math.min(100, armor.charge + Math.min(12, hit*hero.pointsInTalent(Talent.COW_CHAIN_RUSH)));
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
    @Override public Talent[] talents() { return new Talent[]{Talent.COW_LONG_RUSH, Talent.COW_THUNDER_HOOVES, Talent.COW_CHAIN_RUSH, Talent.HEROIC_ENERGY}; }
}
