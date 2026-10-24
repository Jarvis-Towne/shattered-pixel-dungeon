/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowLegend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CowGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stubbornness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/** A growing short horn hammer; its extra damage rewards preserving some resolve. */
public class OxHorn extends MeleeWeapon {
    private int argumentHits;

    {
        image = ItemSpriteSheet.COW_HORN;
        tier = 1;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 0.9f;
        unique = true;
        bones = false;
    }
    /** Stage changes the actual weapon tier, strength requirement and upgrade scaling.
     * The same item is retained: enchantment, augment, upgrades and saved hit rhythm survive. */
    public void evolveTo(int stage) {
        int form = Math.min(3, Math.max(0, stage));
        tier = Math.max(tier, form + 1);
        // Reuse existing pixel art so each forged form is visually recognizable.
        image = form == 0 ? ItemSpriteSheet.COW_HORN : form == 1 ? ItemSpriteSheet.MACE
                : form == 2 ? ItemSpriteSheet.BATTLE_AXE : ItemSpriteSheet.WAR_HAMMER;
    }

    @Override public boolean doEquip(Hero hero) {
        if (hero != null && hero.heroClass == HeroClass.COW) evolveTo(CowLegend.stage(hero));
        return super.doEquip(hero);
    }

    private int ownerStage() {
        return Dungeon.hero != null && Dungeon.hero.belongings.contains(this) ? CowLegend.stage(Dungeon.hero) : 0;
    }

    @Override public int min(int level) { return tier + 1 + level; }
    @Override public int max(int level) { return 4*(tier + 1) + (tier + 1)*level; }

    @Override public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero && ((Hero)attacker).heroClass == HeroClass.COW
                && defender != null && defender.isAlive() && defender.alignment == Char.Alignment.ENEMY && damage > 0) {
            Hero hero = (Hero)attacker;
            Stubbornness stubborn = hero.buff(Stubbornness.class);
            damage += hero.lvl/6 + hero.pointsInTalent(Talent.COW_HORN_POLISH);
            if (stubborn != null) damage += stubborn.charge()/3;
            damage = super.proc(attacker, defender, damage);
            // Enchantments can kill, negate or redirect a hit. Do not attach effects to a corpse.
            if (damage <= 0 || !defender.isAlive() || defender.alignment != Char.Alignment.ENEMY) return damage;
            argumentHits = (argumentHits + 1) % 3;
            int stage = CowLegend.stage(hero);
            if (stage > 0 && argumentHits == 0) {
                damage += 2*(stage + 1);
                if (stage >= 2) Buff.prolong(defender, Cripple.class, 2f);
                if (stage >= 3) {
                    Buff.prolong(defender, Vulnerable.class, 2f);
                    Buff.prolong(defender, Daze.class, 1f);
                    Buff.affect(hero, CowGuard.class).set(2, 3);
                }
                CowLegend.get(hero).quip("third_hit");
            }
            return damage;
        }
        return super.proc(attacker, defender, damage);
    }

    public int argumentHits() { return argumentHits; }

    @Override public String name() {
        int stage = ownerStage();
        return stage == 0 ? super.name() : super.name().replace(trueName(), Messages.get(this, "name_" + stage));
    }

    @Override public String desc() {
        String description = super.desc() + "\n\n" + Messages.get(this, "stage_" + ownerStage());
        if (Dungeon.hero != null && Dungeon.hero.belongings.contains(this)) {
            CowLegend legend = Dungeon.hero.buff(CowLegend.class);
            if (legend != null) description += "\n\n" + legend.progress();
        }
        return description;
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("cow_argument_hits", argumentHits);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        argumentHits = Math.max(0, bundle.getInt("cow_argument_hits")) % 3;
    }
}
