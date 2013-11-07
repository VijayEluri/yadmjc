/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fr.ritaly.dungeonmaster.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.ClockListener;
import fr.ritaly.dungeonmaster.Direction;
import fr.ritaly.dungeonmaster.HasDirection;
import fr.ritaly.dungeonmaster.Materiality;
import fr.ritaly.dungeonmaster.Position;
import fr.ritaly.dungeonmaster.Utils;
import fr.ritaly.dungeonmaster.ai.astar.PathFinder;
import fr.ritaly.dungeonmaster.audio.AudioClip;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.champion.Party;
import fr.ritaly.dungeonmaster.event.ChangeEvent;
import fr.ritaly.dungeonmaster.event.ChangeListener;
import fr.ritaly.dungeonmaster.item.Action;
import fr.ritaly.dungeonmaster.item.Item;
import fr.ritaly.dungeonmaster.item.ItemFactory;
import fr.ritaly.dungeonmaster.magic.PowerRune;
import fr.ritaly.dungeonmaster.magic.Spell;
import fr.ritaly.dungeonmaster.map.Element;
import fr.ritaly.dungeonmaster.stat.Stat;

/**
 * A creature (or a monster).<br>
 * <br>
 * Source: <a href="http://dmweb.free.fr/?q=node/1363">Technical Documentation -
 * Dungeon Master and Chaos Strikes Back Creature Details</a>
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public class Creature implements ChangeListener, ClockListener, HasDirection {

	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Enumerates the possible states of a {@link Creature}.
	 *
	 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
	 */
	public static enum State {
		/**
		 * State of an idle creature.
		 */
		IDLE,

		/**
		 * State of a creature patrolling a level.
		 */
		PATROLLING,

		/**
		 * State of a creature that detected a party and is tracking the
		 * champions to attack them.
		 */
		TRACKING,

		/**
		 * State of a creature attacking a party.
		 */
		ATTACKING;
		// DYING,
		// DEAD;
	}

	private static final EnumSet<Weakness> WEAKNESSES_MATERIAL_4 = EnumSet.of(Weakness.WEAPONS, Weakness.POISON_BOLT,
			Weakness.FIREBALL, Weakness.LIGHTNING_BOLT);

	private static final EnumSet<Weakness> WEAKNESSES_MATERIAL_5 = EnumSet.of(Weakness.WEAPONS, Weakness.POISON_BOLT,
			Weakness.FIREBALL, Weakness.LIGHTNING_BOLT, Weakness.POISON_CLOUD);

	private static final EnumSet<Weakness> WEAKNESSES_IMMATERIAL = EnumSet.of(Weakness.VORPAL_BLADE, Weakness.YEW_STAFF,
			Weakness.STAFF_OF_MANAR, Weakness.WEAKEN_NON_MATERIAL_BEINGS_SPELL);

	private static final EnumSet<Weakness> WEAKNESSES_WEAPONS = EnumSet.of(Weakness.WEAPONS);

	private static final EnumSet<Weakness> WEAKNESSES_FIRESTAFF = EnumSet.of(Weakness.THE_FIRESTAFF);

	/**
	 * Defines the size of a {@link Creature} on the floor in terms of number of
	 * sectors occupied.<br>
	 * <br>
	 * Source: <a href="http://dmweb.free.fr/?q=node/1363">Technical
	 * Documentation - Dungeon Master and Chaos Strikes Back Creature
	 * Details</a>
	 */
	public static enum Size {

		/**
		 * Size of a creature occupying one sector. There can be up to 4
		 * creatures per floor tile. Example: screamers.
		 */
		ONE,

		/**
		 * Size of a creature occupying two sectors. There can be up to 2
		 * creatures per floor tile. Example: worms.
		 */
		TWO,

		/**
		 * Size of a creature occupying four sectors. There can be only 1
		 * creature per floor tile. Example: dragons.
		 */
		FOUR;

		public int value() {
			switch (this) {
			case ONE:
				return 1;
			case TWO:
				return 2;
			case FOUR:
				return 4;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Enumerates the different types of {@link Creature}.
	 *
	 * @author francois_ritaly
	 */
	public static enum Type {
		MUMMY(17, 25, 33, 20, 0, 40, 3, 4, 2, 1, 4, 9, 1, 15, 12, 3, 4, 40),
		SCREAMER(120, 5, 165, 5, 0, 5, 6, 1, 1, 1, 0, 15, 6, 7, 10, 2, 0, 5),
		/** aka STONE_ROCK */
		ROCK_PILE(185, 170, 50, 40, 5, 10, 4, 3, 4, 1, 5, 12, 14, 6, 15, 4, 5, 10),
		/** aka OGRE */
		TROLIN(13, 28, 20, 25, 0, 41, 3, 3, 3, 1, 1, 4, 2, 3, 8, 2, 1, 41),
		/** aka WORM */
		MAGENTA_WORM(18, 72, 70, 45, 35, 35, 4, 1, 10, 1, 5, 10, 9, 11, 19, 3, 5, 35),
		/** aka WASP */
		GIANT_WASP(1, 180, 8, 28, 20, 150, 4, 2, 4, 1, 9, 15, 0, 0, 16, 2, 9, 150),
		GHOST(11, 15, 30, 55, 0, 80, 6, 3, 4, 1, 6, 6, 12, 15, 16, 6, 6, 80),
		/** aka TENTACLE */
		SWAMP_SLIME(15, 20, 110, 80, 15, 20, 3, 2, 1, 3, 3, 10, 4, 14, 32, 4, 3, 20),
		/** aka SNAKE */
		COUATL(5, 42, 39, 90, 100, 88, 4, 3, 4, 1, 7, 3, 3, 6, 10, 2, 7, 88),
		/** aka EYE_BALL */
		WIZARD_EYE(10, 30, 40, 58, 0, 80, 5, 10, 2, 3, 6, 10, 3, 11, 21, 3, 6, 80),
		SKELETON(7, 22, 20, 22, 0, 80, 4, 3, 0, 1, 5, 9, 6, 15, 7, 2, 5, 80),
		STONE_GOLEM(21, 240, 120, 219, 0, 35, 3, 3, 0, 1, 11, 15, 15, 15, 14, 3, 11, 35),
		GIGGLER(3, 50, 10, 10, 0, 110, 0, 6, 3, 1, 1, 0, 3, 2, 5, 2, 1, 110),
		/** aka GIANT_RAT */
		PAIN_RAT(9, 45, 101, 90, 0, 65, 4, 4, 5, 1, 8, 15, 3, 10, 8, 3, 8, 65),
		/** aka SORCERER */
		VEXIRK(10, 45, 44, 75, 0, 90, 5, 5, 3, 4, 9, 5, 5, 3, 20, 3, 9, 90),
		RUSTER(20, 100, 60, 30, 0, 30, 3, 2, 3, 1, 3, 3, 8, 5, 18, 5, 3, 30),
		/** aka SCORPION */
		GIANT_SCORPION(8, 55, 150, 150, 240, 55, 4, 3, 1, 1, 9, 9, 7, 8, 20, 4, 9, 55),
		WATER_ELEMENTAL(25, 75, 144, 66, 0, 50, 3, 1, 3, 1, 6, 7, 10, 14, 25, 5, 6, 50),
		/** aka KNIGHT or DEATH_KNIGHT */
		ANIMATED_ARMOR(14, 140, 60, 105, 0, 70, 4, 5, 0, 1, 10, 15, 15, 15, 6, 3, 10, 70),
		/** aka SPIDER */
		OITU(7, 33, 77, 130, 0, 60, 4, 2, 5, 1, 9, 6, 5, 8, 15, 4, 9, 60),
		/** aka MATERIALIZER */
		ZYTAZ(5, 15, 33, 61, 0, 65, 5, 8, 2, 3, 12, 7, 5, 15, 18, 7, 12, 65),
		/** aka FIRE_ELEMENTAL */
		BLACK_FLAME(255, 45, 80, 105, 0, 60, 1, 4, 3, 1, 5, 10, 15, 15, 8, 4, 5, 60),
		DEMON(10, 68, 100, 100, 0, 75, 3, 4, 3, 4, 13, 15, 5, 10, 14, 4, 13, 75),
		/** aka DRAGON */
		RED_DRAGON(13, 110, 255, 255, 0, 70, 4, 5, 6, 2, 15, 7, 12, 6, 28, 5, 15, 70),
		LORD_CHAOS(12, 255, 180, 210, 0, 130, 5, 9, 3, 6, 15, 3, 11, 15, 22, 4, 15, 130),
		LORD_ORDER(12, 255, 180, 210, 0, 130, 5, 9, 3, 6, 15, 3, 11, 15, 22, 4, 15, 130),
		GREY_LORD(12, 255, 180, 210, 0, 130, 5, 9, 3, 6, 15, 3, 11, 15, 22, 4, 15, 130);

		/**
		 * The armor value is always within range [0,255]. The special value 255
		 * means that the creature is invincible.
		 */
		private final int armor;

		/**
		 * Defines how long it takes for the creature to move from one position
		 * to another (in number of clock ticks). Value within [0,255]. The
		 * special value 255 means that the creature can't move.
		 */
		private final int moveDuration;

		/**
		 * The base health is used to calculate the health of creatures
		 * generated during the game.
		 */
		private final int baseHealth;

		/**
		 * The odds of hitting a {@link Champion}. Value within [0,255].
		 */
		private final int hitProbability;

		/**
		 * The strength of the creature's poisonous attack. Value within
		 * [0,255].
		 */
		private final int poisonAmount;

		private final int attackPower;

		/**
		 * This "number" is used to determine what kind of attack the creature
		 * executes. Changing this value will result in a different "protection"
		 * to be used when calculating the damage:
		 *
		 * 1: Use Anti-Fire to determine damage 2: Half the hero's armor and do
		 * physical damage 3: Unknown 4: Deal physical piercing damage 5: Use
		 * Anti-Magic to determine damage 6: Use Wisdom to determine damage
		 */
		private final AttackType attackType;

		/**
		 * Valeur dans l'intervalle [0-15].
		 */
		private final int sightRange;

		/**
		 * Valeur dans l'intervalle [0-15]. aka "Detection range".
		 */
		private final int awareness;

		/**
		 * Valeur dans l'intervalle [0-15]. aka "Spell casting range".
		 */
		private final int spellRange;

		private final Champion.Level skill;

		/**
		 * Resistance to War Cry, Calm, Brandish and Blow Horn (maybe also
		 * Confuse). Value within [0,15]. The special value 15 means the
		 * creature can't be frightened.
		 */
		private final int bravery;

		/**
		 * Resistance to magical spells like Fireball. Value within [0,15]. The
		 * special value 15 means the creature is immune to magic attacks. aka
		 * "Fire resistance".
		 */
		private final int antiMagic;

		/**
		 * Resistance to magical spells involving poison. Value within [0,15].
		 * The special value 15 means the creature is immune to poison attacks.
		 */
		private final int poisonResistance;

		/**
		 * This is the number of clock ticks per attack, defining the attack
		 * speed of the creature. This is the minimum amount of time required
		 * between two attacks. Value within [0,255].
		 */
		private final int attackDuration;

		/**
		 * The number of clock ticks during which the picture of an attacking
		 * creature should be displayed. Value within [0,255].
		 */
		private final int attackDisplayDuration;

		/**
		 * This value is used as a multiplier to compute the experience earned
		 * by a champion killing this creature
		 */
		private final int experienceMultiplier;

		/**
		 * Value within [0,255]. The special value 255 means the creature is
		 * untouchable. This value represents the difficulty for champions to
		 * hit the creature
		 */
		private final int shield;

		private Type(int moveDuration, int armor, int baseHealth,
				int hitProbability, int poisonAmount, int attackPower,
				int attackType, int sightRange, int awareness, int spellRange,
				int skill, int bravery, int antiMagic, int poisonResistance,
				int attackDuration, int attackDisplayDuration,
				int experienceMultiplier, int shield) {

			Validate.isTrue((moveDuration >= 0) && (moveDuration <= 255),
					"The given move duration " + moveDuration
							+ " must be in range [0-255]");
			Validate.isTrue((armor >= 0) && (armor <= 255), "The given armor "
					+ armor + " must be in range [0-255]");
			Validate.isTrue(baseHealth >= 0, "The given base health "
					+ baseHealth + " must be positive");
			Validate.isTrue((hitProbability >= 0) && (hitProbability <= 255),
					"The given hit probability " + hitProbability
							+ " must be in range [0-255]");
			Validate.isTrue((poisonAmount >= 0) && (poisonAmount <= 255),
					"The given poison amount " + poisonAmount
							+ " must be in range [0-255]");
			Validate.isTrue(attackPower >= 0, "The given attack power "
					+ attackPower + " must be positive");
			Validate.isTrue((attackType >= 0) && (attackType <= 6),
					"The given attack type " + attackType
							+ " must be in range [0-6]");
			Validate.isTrue((sightRange >= 0) && (sightRange <= 15),
					"The given sight range " + sightRange
							+ " must be in range [0-15]");
			Validate.isTrue((awareness >= 0) && (awareness <= 15),
					"The given awareness " + awareness
							+ " must be in range [0-15]");
			Validate.isTrue((spellRange >= 0) && (spellRange <= 15),
					"The given spell range " + spellRange
							+ " must be in range [0-15]");
			Validate.isTrue((skill >= 0) && (skill <= 15), "The given skill "
					+ skill + " must be in range [0-15]");
			Validate.isTrue((bravery >= 0) && (bravery <= 15),
					"The given bravery " + bravery + " must be in range [0-15]");
			Validate.isTrue((antiMagic >= 0) && (antiMagic <= 15),
					"The given anti-magic " + antiMagic
							+ " must be in range [0-15]");
			Validate.isTrue(
					(poisonResistance >= 0) && (poisonResistance <= 15),
					"The given poison resistance " + poisonResistance
							+ " must be in range [0-15]");
			Validate.isTrue((attackDuration >= 0) && (attackDuration <= 255),
					"The given attack duration " + attackDuration
							+ " must be in range [0-255]");
			Validate.isTrue((attackDisplayDuration >= 0)
					&& (attackDisplayDuration <= 255),
					"The given attack display duration "
							+ attackDisplayDuration
							+ " must be in range [0-255]");
			Validate.isTrue((experienceMultiplier >= 0)
					&& (experienceMultiplier <= 15),
					"The given experience multiplier " + experienceMultiplier
							+ " must be in range [0-15]");
			Validate.isTrue((shield >= 0) && (shield <= 255),
					"The given shield " + shield + " must be in range [0-255]");

			this.baseHealth = baseHealth;
			this.moveDuration = moveDuration;
			this.armor = armor;
			this.hitProbability = hitProbability;
			this.poisonAmount = poisonAmount;
			this.attackPower = attackPower;
			this.attackType = AttackType.values()[attackType];
			this.sightRange = sightRange;
			this.awareness = awareness;
			this.spellRange = spellRange;
			this.skill = Champion.Level.values()[skill];
			this.bravery = bravery;
			this.antiMagic = antiMagic;
			this.poisonResistance = poisonResistance;
			this.attackDuration = attackDuration;
			this.attackDisplayDuration = attackDisplayDuration;
			this.experienceMultiplier = experienceMultiplier;
			this.shield = shield;
		}

		public int getShield() {
			return shield;
		}

		public int getExperienceMultiplier() {
			return experienceMultiplier;
		}

		public int getAttackDisplayDuration() {
			return attackDisplayDuration;
		}

		public int getAttackDuration() {
			return attackDuration;
		}

		public boolean absorbsItems() {
			// When this bit is set to '1', the creature can absorb some items
			// when they are thrown at the creature (like the Mummy). The list
			// of items that can be absorbed is hard coded in the program
			// (Arrow, Slayer, Poison Dart, Throwing Star and Dagger). If a
			// thrown item is not absorbed by the creature, it falls on the
			// floor (it is never destroyed). This is not linked to the
			// ability of the Giggler to steal items in champion hands which is
			// hard coded.

			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case MUMMY:
			case SCREAMER:
			case TROLIN:
			case GIANT_WASP:
			case SWAMP_SLIME:
			case COUATL:
			case WIZARD_EYE:
			case GIGGLER:
			case PAIN_RAT:
			case VEXIRK:
			case RUSTER:
			case GIANT_SCORPION:
			case DEMON:
			case RED_DRAGON:
				return true;
			default:
				return false;
			}
		}

		public boolean isImmuneToPoison() {
			return (15 == poisonResistance);
		}

		public boolean isImmuneToMagic() {
			return (15 == antiMagic);
		}

		public int getPoisonResistance() {
			return poisonResistance;
		}

		public int getAntiMagic() {
			return antiMagic;
		}

		public int getBravery() {
			return bravery;
		}

		public Champion.Level getSkill() {
			return skill;
		}

		public int getSpellRange() {
			// Maximum number of tiles between creature and party needed to
			// perform a distance attack (cast a spell)
			return spellRange;
		}

		/**
		 * Tells whether the creature is still (that is it can't move).
		 *
		 * @return whether the creature is still.
		 */
		public boolean isStill() {
			return (255 == moveDuration);
		}

		/**
		 * Tells whether the creature can move. Returns true for most creatures
		 * but {@link Type#WATER_ELEMENTAL} and {@link Type#BLACK_FLAME}.
		 *
		 * @return whether the creature can move.
		 */
		public boolean canMove() {
			return !isStill();
		}

		/**
		 * Tells whether the creature is invincible.
		 *
		 * @return whether the creature is invincible.
		 */
		public boolean isInvincible() {
			return (255 == armor);
		}

		/**
		 * Tells whether the creature levitates.
		 *
		 * @return whether the creature levitates.
		 */
		public boolean levitates() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			// If this bit is set to '1', the creature can pass over pits
			// without falling
			switch (this) {
			case GIANT_WASP:
			case GHOST:
			case COUATL:
			case WIZARD_EYE:
			case VEXIRK:
			case ZYTAZ:
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return true;
			default:
				return false;
			}
		}

		// TODO isNyctalop() ?
		public boolean canSeeInDarkness() {
			// When this bit is set to '1', the creature can see the party in
			// darkness because it ignores the sight range reduction caused by
			// low light levels in the dungeon

			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case MUMMY:
			case GHOST:
			case COUATL:
			case SKELETON:
			case WATER_ELEMENTAL:
			case BLACK_FLAME:
			case DEMON:
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return true;
			default:
				return false;
			}
		}

		public boolean isArchenemy() {
			// When this bit is set, the creature never takes any damage
			// (health is not decreased), it can teleport up to two tiles away
			// and it cannot move to a tile containing a Fluxcage

			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return true;
			default:
				return false;
			}
		}

		public boolean canSeeInvisible() {
			// When this bit is set to '1', the creature can see the party even
			// if it is under the effect of the 'Invisibility' spell.

			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case GHOST:
			case BLACK_FLAME:
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return true;
			default:
				return false;
			}
		}

		public boolean canStealItems() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			return equals(GIGGLER);
		}

		public boolean canTeleport() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			return equals(LORD_CHAOS);
		}

		public boolean isNearlyImmuneToSpells() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			return equals(GHOST) || equals(BLACK_FLAME);
		}

		public boolean canOnlyBeKilledWhenMaterialized() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			return equals(ZYTAZ);
		}

		public boolean hitByWeakenNonmaterialBeingsSpell() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case MUMMY:
			case MAGENTA_WORM:
			case GIANT_WASP:
			case SWAMP_SLIME:
			case COUATL:
			case WIZARD_EYE:
			case SKELETON:
			case STONE_GOLEM:
			case VEXIRK:
			case RUSTER:
			case GIANT_SCORPION:
			case WATER_ELEMENTAL:
			case OITU:
			case BLACK_FLAME:
			case DEMON:
			case RED_DRAGON:
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return true;
			default:
				return false;
			}
		}

		public boolean hitByDisruptAttacks() {
			// cf http://www.gamefaqs.com/snes/588299-dungeon-master/faqs/33244
			switch (this) {
			case GHOST:
			case WATER_ELEMENTAL:
			case ZYTAZ:
			case BLACK_FLAME:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Retourne la mat�rialit� de la {@link Creature}.
		 *
		 * @return une instance de {@link Materiality}.
		 */
		public Materiality getMateriality() {
			// If this bit is set to '1', the creature is non material. These
			// creatures ignore normal attacks but take damage from the
			// 'Disrupt' action of the Vorpal Blade. Fire damage is also
			// reduced by a half. All missiles except 'Weaken Non-material
			// Beings' pass through these creatures (this is hard coded).
			// These creatures can pass through all doors of any type.
			switch (this) {
			case BLACK_FLAME:
			case GHOST:
			case WATER_ELEMENTAL:
			case ZYTAZ: // <--- Cas sp�cial car tant�t mat�riel tant�t immat�riel
				return Materiality.IMMATERIAL;
			default:
				return Materiality.MATERIAL;
			}
		}

		private static final ItemFactory ITEM_FACTORY = ItemFactory.getFactory();

		/**
		 * Generates a random list with n (randomly chosen in [min, max]) items
		 * with the given type.
		 *
		 * @param min
		 *            the min number of items to generate.
		 * @param max
		 *            the max number of items to generate.
		 * @param type
		 *            the type of items to generate.
		 * @return a list of items. Never returns null.
		 */
		private static List<Item> randomItems(int min, int max, Item.Type type) {
			final List<Item> items = new ArrayList<Item>();

			for (int i = 0; i < Utils.random(min, max); i++) {
				items.add(ITEM_FACTORY.newItem(type));

			}

			return items;
		}

		/**
		 * Generates and returns a list of items corresponding to the items
		 * dropped by the creature when killed.
		 *
		 * @return a list of items. Never returns null.
		 */
		private List<Item> getItemsLeftWhenKilled() {
			switch (this) {
			case SCREAMER: {
				return randomItems(1, 2, Item.Type.SCREAMER_SLICE);
			}
			case ROCK_PILE: {
				final List<Item> items = new ArrayList<Item>();
				items.addAll(randomItems(1, 2, Item.Type.BOULDER));
				items.addAll(randomItems(0, 2, Item.Type.ROCK));

				return items;
			}
			case TROLIN: {
				final List<Item> items = new ArrayList<Item>();
				items.add(ItemFactory.getFactory().newItem(Item.Type.CLUB));

				return items;
			}
			case MAGENTA_WORM: {
				return randomItems(1, 3, Item.Type.WORM_ROUND);
			}
			case SKELETON: {
				final List<Item> items = new ArrayList<Item>();
				items.add(ItemFactory.getFactory().newItem(Item.Type.WOODEN_SHIELD));
				items.add(ItemFactory.getFactory().newItem(Item.Type.FALCHION));

				return items;
			}
			case STONE_GOLEM: {
				final List<Item> items = new ArrayList<Item>();
				items.add(ItemFactory.getFactory().newItem(Item.Type.STONE_CLUB));

				return items;
			}
			case PAIN_RAT: {
				return randomItems(1, 2, Item.Type.DRUMSTICK);
			}
			case ANIMATED_ARMOR: {
				// The dropped items are cursed !
				final List<Item> items = new ArrayList<Item>();
				items.add(ItemFactory.getFactory().newItem(Item.Type.ARMET));
				items.add(ItemFactory.getFactory().newItem(Item.Type.TORSO_PLATE));
				items.add(ItemFactory.getFactory().newItem(Item.Type.LEG_PLATE));
				items.add(ItemFactory.getFactory().newItem(Item.Type.FOOT_PLATE));
				items.add(ItemFactory.getFactory().newItem(Item.Type.SWORD));
				items.add(ItemFactory.getFactory().newItem(Item.Type.SWORD));

				for (Item item : items) {
					item.curse(PowerRune.UM);
				}

				return items;
			}
			case RED_DRAGON: {
				return randomItems(8, 10, Item.Type.DRAGON_STEAK);
			}
			default:
				return Collections.emptyList();
			}
		}

		/**
		 * Returns the spells the creature can cast.
		 *
		 * @return a list of spell types. Never returns null.
		 */
		public List<Spell.Type> getSpells() {
			switch (this) {
			case SWAMP_SLIME:
				return Arrays.asList(Spell.Type.POISON_CLOUD);
			case WIZARD_EYE:
				return Arrays.asList(Spell.Type.OPEN_DOOR, Spell.Type.LIGHTNING_BOLT);
			case VEXIRK:
				return Arrays.asList(Spell.Type.OPEN_DOOR, Spell.Type.LIGHTNING_BOLT, Spell.Type.POISON_CLOUD,
						Spell.Type.FIREBALL);
			case ZYTAZ:
				return Arrays.asList(Spell.Type.POISON_CLOUD, Spell.Type.FIREBALL);
			case DEMON:
				return Arrays.asList(Spell.Type.FIREBALL);
			case RED_DRAGON:
				return Arrays.asList(Spell.Type.FIREBALL);
			case LORD_CHAOS:
				return Arrays.asList(Spell.Type.OPEN_DOOR, Spell.Type.LIGHTNING_BOLT, Spell.Type.POISON_CLOUD,
						Spell.Type.FIREBALL);
			default:
				return Collections.emptyList();
			}
		}

		/**
		 * Retourne la liste des sorts d'attaque de ce type de {@link Creature}.
		 *
		 * @return une {@link List} de {@link Spell.Type}. Ne retourne jamais
		 *         null.
		 */
		public List<Spell.Type> getAttackSpells() {
			final List<Spell.Type> spells = getSpells();

			// Remove the possible non-attack spells (like OPEN_DOOR)
			for (final Iterator<Spell.Type> it = spells.iterator(); it.hasNext();) {
				final Spell.Type type = (Spell.Type) it.next();

				if (!type.isAttackSpell()) {
					it.remove();
				}
			}

			return spells;
		}

		public boolean canCastSpell() {
			switch (this) {
			case SWAMP_SLIME:
			case WIZARD_EYE:
			case VEXIRK:
			case ZYTAZ:
			case DEMON:
			case RED_DRAGON:
			case LORD_CHAOS:
				return true;
			default:
				return false;
			}
		}

		public Size getSize() {
			// cf Technical Documentation - Dungeon Master and Chaos Strikes
			// Back Creature Details
			switch (this) {
			case MUMMY:
			case SCREAMER:
			case ROCK_PILE:
			case TROLIN:
			case GIANT_WASP:
			case GHOST:
			case SWAMP_SLIME:
			case SKELETON:
			case WIZARD_EYE:
			case GIGGLER:
			case DEMON:
			case ZYTAZ:
			case ANIMATED_ARMOR:
			case VEXIRK:
				return Size.ONE;
			case MAGENTA_WORM:
			case RUSTER:
			case PAIN_RAT:
				return Size.TWO;
			case GIANT_SCORPION:
			case STONE_GOLEM:
			case BLACK_FLAME:
			case COUATL:
			case WATER_ELEMENTAL:
			case OITU:
			case LORD_CHAOS:
			case RED_DRAGON:
			case LORD_ORDER:
			case GREY_LORD:
				return Size.FOUR;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public Height getHeight() {
			// These two bits define the height of the creature. It is used to
			// check if missiles can fly over the creatures (for example
			// Fireballs can fly over small creatures). This value is also used
			// to define how to animate a door that is closed upon the creature:
			// '0': the door is not animated and stays fully open. The creature
			// still takes damage.
			// '1': the door is animated from the top to 1/4th of its size.
			// This applies to tall creatures like Mummies.
			// '2': the door is animated between 1/4th of its size to half of
			// its size. This applies to medium sized creatures like Screamers.
			// '3': the door is animated from half of its size to 3/4th of its
			// size. This applies to small creatures like the Worm.
			// Note: This value is ignored for non material creatures and the
			// door always closes normally without causing any damage to such
			// creatures.

			// cf Technical Documentation - Dungeon Master and Chaos Strikes
			// Back Creature Details
			switch (this) {
			case SWAMP_SLIME:
			case GIANT_SCORPION:
			case WIZARD_EYE:
			case STONE_GOLEM:
			case MUMMY:
			case BLACK_FLAME:
			case SKELETON:
			case COUATL:
			case TROLIN:
			case GIANT_WASP:
			case ANIMATED_ARMOR:
			case WATER_ELEMENTAL:
			case OITU:
			case DEMON:
			case LORD_CHAOS:
			case RED_DRAGON:
			case LORD_ORDER:
			case GREY_LORD:
				return Height.GIANT;
			case GIGGLER:
			case PAIN_RAT:
			case SCREAMER:
			case ROCK_PILE:
				return Height.MEDIUM;
			case RUSTER:
			case VEXIRK:
			case MAGENTA_WORM:
				return Height.SMALL;
			case GHOST:
			case ZYTAZ:
				return Height.UNDEFINED;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public int getArmor() {
			// This is the resistance to damage including Dispell on non
			// material creatures
			return armor;
		}

		public int computeDamagePoints(Champion champion, Item weapon, Action action) {
			Validate.notNull(champion, "The given champion is null");
			Validate.notNull(weapon, "The given weapon item is null");
			Validate.notNull(action, "The given action is null");

			// Le nombre de points de d�g�ts d�pend de:

			// - Le type d'arme
			final int weaponDamage = weapon.getType().getDamage();

			// - La force du champion
			final int strength = champion.getStats().getStrength().value();

			// - L'armure de la cr�ature
			final int vulnerability = 255 - armor;

			// - Le type d'action utilis�
			final int actionDamage = action.getDamage();

			// FIXME Facteur correctif ?
			return (weaponDamage + actionDamage) * vulnerability * strength;
		}

		public int getMoveDuration() {
			// This is the number of clock ticks (1/6th of a second) per
			// movement, defining the movement speed of the creature. This is
			// the minimum of time required between two movements. If the value
			// is FFh then the creature cannot move at all.
			return moveDuration;
		}

		private int getBaseHealth() {
			// This value is used to compute the health of a new creatures as
			// detailed on Technical Documentation - Dungeon Master and Chaos
			// Strikes Back Creature Generators
			return baseHealth;
		}

		/**
		 * Tells whether the attack of a {@link Creature} against a
		 * {@link Champion} succeeds.
		 *
		 * @return whether the attack of a {@link Creature} against a
		 *         {@link Champion} succeeds.
		 */
		public boolean hitsChampion() {
			return Utils.random(255) < hitProbability;
		}

		public int getPoisonAmount() {
			// The amount of poison inflicted when the creature successfully
			// hits a character
			return poisonAmount;
		}

		public int getAttackPower() {
			// The base value for computing how much damage a creature's attack
			// will inflict
			return attackPower;
		}

		public AttackType getAttackType() {
			return attackType;
		}

		public int getSightRange() {
			// Maximum number of tiles between creature and party needed to see
			// the party. This applies only if the creature is facing the
			// party. This value is affected by the current light level in the
			// dungeon (the value is halved for each level of darkness).
			return sightRange;
		}

		public int getAwareness() {
			// Maximum number of tiles between creature and party needed to
			// detect and "turn" towards the party, perhaps to shoot a
			// projectile. This applies even if the creature is not facing the
			// party
			return awareness;
		}

		public EnumSet<Weakness> getWeaknesses() {
			switch (this) {
			case MUMMY:
			case SKELETON:
			case ANIMATED_ARMOR:
				return WEAKNESSES_MATERIAL_4;
			case SCREAMER:
			case ROCK_PILE:
			case TROLIN:
			case MAGENTA_WORM:
			case GIANT_WASP:
			case GIGGLER:
			case PAIN_RAT:
			case VEXIRK:
			case RUSTER:
			case GIANT_SCORPION:
			case OITU:
			case DEMON:
			case RED_DRAGON:
			case SWAMP_SLIME:
			case COUATL:
			case WIZARD_EYE:
				return WEAKNESSES_MATERIAL_5;
			case STONE_GOLEM:
				return WEAKNESSES_WEAPONS;
			case GHOST:
			case WATER_ELEMENTAL:
			case ZYTAZ:
			case BLACK_FLAME:
				return WEAKNESSES_IMMATERIAL;
			case LORD_CHAOS:
				return WEAKNESSES_FIRESTAFF;
			case LORD_ORDER:
				return EnumSet.noneOf(Weakness.class);
			case GREY_LORD:
				return EnumSet.noneOf(Weakness.class);
			default:
				throw new UnsupportedOperationException();
			}
		}

		public boolean isHurtByWeapon(Item weapon) {
			Validate.notNull(weapon, "The given weapon item is null");

			final EnumSet<Weakness> weaknesses = getWeaknesses();

			if (weaknesses.isEmpty()) {
				return false;
			}
			for (Weakness weakness : weaknesses) {
				if (weakness.acceptsWeapon(weapon)) {
					return true;
				}
			}

			return false;
		}

		public boolean isHurtBySpell(Spell.Type spellType) {
			Validate.notNull(spellType, "The given spell type is null");

			final EnumSet<Weakness> weaknesses = getWeaknesses();

			if (weaknesses.isEmpty()) {
				return false;
			}
			for (Weakness weakness : weaknesses) {
				if (weakness.acceptsSpell(spellType)) {
					return true;
				}
			}

			return false;
		}

		public boolean isHurtByPoisonCloud() {
			final EnumSet<Weakness> weaknesses = getWeaknesses();

			// Optimisation
			return weaknesses.contains(Weakness.POISON_CLOUD);
		}

		public boolean isFrontImageMirrored() {
			switch (this) {
			case GIANT_SCORPION:
			case SWAMP_SLIME:
			case WIZARD_EYE:
			case RUSTER:
			case GHOST:
			case BLACK_FLAME:
			case COUATL:
			case MAGENTA_WORM:
			case GIANT_WASP:
			case ANIMATED_ARMOR:
			case ZYTAZ:
			case WATER_ELEMENTAL:
			case OITU:
			case DEMON:
			case RED_DRAGON:
				return true;
			default:
				return false;
			}
		}

		public boolean hasSideImage() {
			// The creature has a side graphic
			switch (this) {
			case SCREAMER:
			case GIGGLER:
			case PAIN_RAT:
			case RUSTER:
			case STONE_GOLEM:
			case MUMMY:
			case SKELETON:
			case COUATL:
			case VEXIRK:
			case MAGENTA_WORM:
			case TROLIN:
			case GIANT_WASP:
			case ANIMATED_ARMOR:
			case ZYTAZ:
			case OITU:
			case DEMON:
			case LORD_CHAOS:
			case RED_DRAGON:
				return true;
			default:
				return false;
			}
		}

		public boolean hasBackImage() {
			// The creature has a back graphic
			switch (this) {
			case GIANT_SCORPION:
			case SWAMP_SLIME:
			case GIGGLER:
			case PAIN_RAT:
			case RUSTER:
			case STONE_GOLEM:
			case MUMMY:
			case SKELETON:
			case COUATL:
			case VEXIRK:
			case MAGENTA_WORM:
			case TROLIN:
			case GIANT_WASP:
			case ANIMATED_ARMOR:
			case ZYTAZ:
			case OITU:
			case DEMON:
			case LORD_CHAOS:
			case RED_DRAGON:
				return true;
			default:
				return false;
			}
		}

		public boolean hasAttackImage() {
			// The creature has an attack graphic
			switch (this) {
			case GIGGLER:
			case RUSTER:
			case LORD_ORDER:
			case GREY_LORD:
				return false;
			default:
				return true;
			}
		}

		public boolean isAttackImageMirrored() {
			// When enabled, for each attack, the attack image is displayed
			// either normally or mirrored. '0': Disabled, '1': Enabled
			switch (this) {
			case GIGGLER:
			case SCREAMER:
			case STONE_GOLEM:
			case MUMMY:
			case SKELETON:
			case TROLIN:
			case LORD_CHAOS:
			case LORD_ORDER:
			case GREY_LORD:
				return false;
			default:
				return true;
			}
		}

		public boolean isAttackImageMirroredDuringAttack() {
			// When enabled the attack image is displayed both normally and
			// then mirrored during a single attack. '0': Enabled, '1':
			// Disabled.
			switch (this) {
			case SWAMP_SLIME:
			case VEXIRK:
			case RED_DRAGON:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Indique si la {@link Creature} peut attaquer m�me si elle ne fait pas
		 * face aux {@link Champion}s.
		 *
		 * @return si la {@link Creature} peut attaquer m�me si elle ne fait pas
		 *         face aux {@link Champion}s.
		 */
		public boolean isSideAttackAllowed() {
			// The creature does not need to face the party to attack. This
			// flag is set only for creatures that have the same image for all
			// sides. It affects their attack frequency because they don't need
			// to turn to face the party before attacking.
			switch (this) {
			case WIZARD_EYE:
			case SCREAMER:
			case GHOST:
			case BLACK_FLAME:
			case WATER_ELEMENTAL:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Indique si la {@link Creature} pr�f�re rester en arri�re-plan quand
		 * d'autres {@link Creature}s attaquent les {@link Champion}s.
		 *
		 * @return si la {@link Creature} pr�f�re rester en arri�re-plan quand
		 *         d'autres {@link Creature}s attaquent les {@link Champion}s.
		 */
		public boolean prefersBackRow() {
			// The creature will tend to stay in the back row while other
			// creatures will step up to the front row when the party is near
			// and they want to attack
			switch (this) {
			case SCREAMER:
			case VEXIRK:
			case WATER_ELEMENTAL:
			case LORD_CHAOS:
			case RED_DRAGON:
			case LORD_ORDER:
			case GREY_LORD:
				return true;

			default:
				return false;
			}
		}

		/**
		 * Indique si la {@link Creature} peut attaquer n'importe quel
		 * {@link Champion} du groupe, en particulier ceux situ�s derri�re dans
		 * le groupe.
		 *
		 * @return si la {@link Creature} peut attaquer n'importe quel
		 *         {@link Champion} du groupe, en particulier ceux situ�s
		 *         derri�re dans le groupe.
		 */
		public boolean canAttackAnyChampion() {
			// If this bit is set to '1', the creature can attack any champion
			// in the party, even the ones in the back. If both 'Prefer back
			// row' and 'Attack any champion' flags are set to '0', the
			// creature will move to the front row of its tile. In other cases
			// the creature has a 25% chance of moving to the front row
			switch (this) {
			case GIGGLER:
			case WIZARD_EYE:
			case VEXIRK:
			case WATER_ELEMENTAL:
				return true;
			default:
				return false;
			}
		}
	}

	/**
	 * La taille de la cr�ature. Permet de d�terminer de combien une porte doit
	 * se fermer avant de frapper la cr�ature en rebondissant.
	 */
	public static enum Height {
		// Les valeurs doivent �tre class�es du plus petit au plus grand
		UNDEFINED,
		// FIXME Les boules de feu peuvent passer au-dessus des cr�atures les +
		// petites
		SMALL,
		MEDIUM,
		GIANT;
	}

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private final int id = SEQUENCE.incrementAndGet();

	private final Type type;

	// TODO La sant� doit se reg�n�rer avec le temps
	private final Stat health;

	private final List<Item> absorbedItems = new ArrayList<Item>();

	private final Materializer materializer;

	private Element element;

	private Direction direction = Direction.NORTH;

	/**
	 * Membre d'instance synchronis�. A lire / �crire via le getter / setter.
	 */
	private State state = State.IDLE;

	/**
	 * {@link AtomicInteger} utilis� afin de repr�senter le timer de d�placement
	 * de la cr�ature. Celle-ci se d�place quand le timer arrive � z�ro
	 * (expiration).
	 */
	private final AtomicInteger moveTimer = new AtomicInteger();

	/**
	 * {@link AtomicInteger} utilis� afin de repr�senter le timer d'attaque de
	 * la cr�ature. Celle-ci peut attaquer quand le timer est � z�ro ce qui le
	 * r�initialise.
	 */
	private final AtomicInteger attackTimer = new AtomicInteger();

	// Le param�tre multiplier peut repr�senter un "health multiplier" ou un
	// "level experience multiplier"
	public Creature(Type type, int multiplier, Direction direction) {
		Validate.notNull(type);
		Validate.isTrue(multiplier > 0, "The given multiplier <" + multiplier
				+ "> must be positive");
		Validate.notNull(direction);

		this.type = type;
		this.direction = direction;

		// cf Technical Documentation - Dungeon Master and Chaos Strikes Back
		// Creature Generators
		final int healthPoints = (multiplier * getType().getBaseHealth())
				+ Utils.random(getType().getBaseHealth() / 4);

		this.health = new Stat(getId(), "Health", healthPoints, healthPoints);
		this.health.addChangeListener(this);

		if (Type.ZYTAZ.equals(getType())) {
			// Cas sp�cial du ZYTAZ. Son caract�re immat�riel est fonction du
			// temps
			this.materializer = new RandomMaterializer(this);
		} else {
			this.materializer = new StaticMaterializer(getType()
					.getMateriality());
		}

		this.moveTimer.set(getType().getMoveDuration());

		Clock.getInstance().register(this);
	}

	public Creature(Type type, int multiplier) {
		this(type, multiplier, Direction.NORTH);
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		Validate.notNull(direction, "The given direction is null");

		if (this.direction != direction) {
			final Direction initialDirection = this.direction;

			this.direction = direction;

			if (log.isDebugEnabled()) {
				log.debug(this + ".Direction: " + initialDirection + " -> "
						+ this.direction);
			}
		}
	}

	public final Materiality getMateriality() {
		return materializer.getMateriality();
	}

	public final Height getHeight() {
		switch (getType()) {
		case GHOST:
		case ZYTAZ:
			return Height.UNDEFINED;
		case GIANT_SCORPION:
		case COUATL:
		case WIZARD_EYE:
		case STONE_GOLEM:
		case MUMMY:
		case BLACK_FLAME:
		case SKELETON:
		case TROLIN:
		case OITU:
		case DEMON:
		case LORD_ORDER:
		case GREY_LORD:
		case GIANT_WASP:
		case ANIMATED_ARMOR:
		case WATER_ELEMENTAL:
		case LORD_CHAOS:
		case RED_DRAGON:
		case SWAMP_SLIME:
			return Height.GIANT;
		case ROCK_PILE:
		case GIGGLER:
		case PAIN_RAT:
		case SCREAMER:
			return Height.MEDIUM;
		case MAGENTA_WORM:
		case RUSTER:
		case VEXIRK:
			return Height.SMALL;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public final Type getType() {
		return type;
	}

	public final Size getSize() {
		return getType().getSize();
	}

	public final boolean canTakeStairs() {
		// TODO Liste des cr�atures � confirmer
		switch (getType()) {
		case ZYTAZ:
		case GHOST:
		case WIZARD_EYE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indique si la {@link Creature} peut attaquer m�me si elle ne fait pas
	 * face aux {@link Champion}s.
	 *
	 * @return si la {@link Creature} peut attaquer m�me si elle ne fait pas
	 *         face aux {@link Champion}s.
	 */
	public final boolean isSideAttackAllowed() {
		return getType().isSideAttackAllowed();
	}

	/**
	 * Indique si la {@link Creature} pr�f�re rester en arri�re-plan quand
	 * d'autres {@link Creature}s attaquent les {@link Champion}s.
	 *
	 * @return si la {@link Creature} pr�f�re rester en arri�re-plan quand
	 *         d'autres {@link Creature}s attaquent les {@link Champion}s.
	 */
	public final boolean prefersBackRow() {
		return getType().prefersBackRow();
	}

	/**
	 * Indique si la {@link Creature} peut attaquer n'importe quel
	 * {@link Champion} du groupe, en particulier ceux situ�s derri�re dans le
	 * groupe.
	 *
	 * @return si la {@link Creature} peut attaquer n'importe quel
	 *         {@link Champion} du groupe, en particulier ceux situ�s derri�re
	 *         dans le groupe.
	 */
	public final boolean canAttackAnyChampion() {
		return getType().canAttackAnyChampion();
	}

	/**
	 * Indique si la {@link Creature} l�che des {@link Item}s au sol quand elle
	 * meurt.
	 *
	 * @return si la {@link Creature} l�che des {@link Item}s au sol quand elle
	 *         meurt.
	 */
	public final boolean dropItems() {
		// If this bit is set to '1', the creature will drop some items when it
		// is killed
		return !getType().getItemsLeftWhenKilled().isEmpty();
	}

	/**
	 * Indique si la {@link Creature} peut "absorber" les {@link Item}s qu'on
	 * lui lance. Ces objets sont alors r�cup�r�s quand la {@link Creature}
	 * meurt.
	 *
	 * @return si la {@link Creature} peut "absorber" les {@link Item}s qu'on
	 *         lui lance.
	 */
	public final boolean absorbItems() {
		return getType().absorbsItems();
	}

	/**
	 * Indique si la {@link Creature} peut voir le groupe de {@link Champion}s
	 * m�me si celui-ci utilise le sort d'invisibilit�.
	 *
	 * @return si la {@link Creature} peut voir le groupe de {@link Champion}s
	 *         m�me si celui-ci utilise le sort d'invisibilit�.
	 */
	public final boolean canSeeInvisible() {
		return getType().canSeeInvisible();
	}

	/**
	 * Indique si la {@link Creature} peut voir dans le noir. Si c'est le cas,
	 * la {@link Creature} n'est pas soumise � la baisse de lumi�re d�e � la
	 * distance d'observation.
	 *
	 * @return si la {@link Creature} peut voir dans le noir.
	 */
	public final boolean canSeeInDarkness() {
		return getType().canSeeInDarkness();
	}

	// cf d�finition de Dungeon Master and Chaos Strikes Back Creature Details
	public final boolean isArchenemy() {
		return getType().isArchenemy();
	}

	public final boolean isFrontImageMirrored() {
		return getType().isFrontImageMirrored();
	}

	/**
	 * Retourne la dur�e d'un d�placement de {@link Creature} (en 1/6 de
	 * seconde).
	 *
	 * @return un entier positif repr�sentant la dur�e d'un d�placement.
	 */
	public final int getMoveDuration() {
		return getType().getMoveDuration();
	}

	/**
	 * Retourne le bonus d'armure de la {@link Creature} sous forme d'un entier.
	 *
	 * @return un entier positif ou nul repr�sentant un bonus d'armure.
	 */
	public final int getArmor() {
		return getType().getArmor();
	}

	public final int getAttackPower() {
		return getType().getAttackPower();
	}

	public final int getPoison() {
		return getType().getPoisonAmount();
	}

	public final int getSightRange() {
		return getType().getSightRange();
	}

	public final int getSpellRange() {
		return getType().getSpellRange();
	}

	public final int getBravery() {
		return getType().getBravery();
	}

	// FIXME public abstract boolean isSuicidal();

	public final int getPoisonResistance() {
		return getType().getPoisonResistance();
	}

	public final boolean isAlive() {
		return health.value() > 0;
	}

	public final boolean isDead() {
		return !isAlive();
	}

	public int getHealth() {
		return health.value();
	}

	public int hit(AttackType attackType) {
		Validate.notNull(attackType, "The given attack type is null");

		if (getType().isInvincible()) {
			// La cr�ature ne peut �tre bless�e
			return 0;
		}

		final int initialHealth = health.value();
		final int points;

		switch (attackType) {
		case CRITICAL:
			// FIXME Valeurs
			points = Utils.random(1, 5) * 3;
			break;
		case FIRE:
			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		case MAGIC:
			if (isImmuneToMagic()) {
				// Cr�ature immunis�e � la magie, aucun d�g�t possible
				return 0;
			}

			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		case NONE:
			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		case NORMAL:
			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		case PSYCHIC:
			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		case SHARP:
			// FIXME Valeurs
			points = Utils.random(1, 5);
			break;
		default:
			throw new UnsupportedOperationException("Unsupported attack type "
					+ attackType);
		}

		this.health.dec(points);

		// La diff�rence n'est pas forc�ment �gale � la variable points si la
		// cr�ature vient de mourir !
		return initialHealth - health.value();
	}

	/**
	 * Retourne le son de la {@link Creature}.
	 *
	 * @return une instance de {@link AudioClip}.
	 */
	public AudioClip getSound() {
		// FIXME Impl�menter fr.ritaly.dungeonmaster.ai.Creature.getSound()
		throw new UnsupportedOperationException();
	}

	/**
	 * Retourne le type d'attaque de la {@link Creature}.
	 *
	 * @return une instance de {@link AttackType}.
	 */
	public final AttackType getAttackType() {
		return getType().getAttackType();
	}

	public final int getAwareness() {
		return getType().getAwareness();
	}

	public final Champion.Level getSkill() {
		return getType().getSkill();
	}

	public final boolean canTeleport() {
		return getType().canTeleport();
	}

	public final int getAntiMagic() {
		return getType().getAntiMagic();
	}

	public final String getId() {
		return type.name() + "[" + id + "]";
	}

	public final boolean isImmuneToPoison() {
		return getType().isImmuneToPoison();
	}

	public final boolean isInvincible() {
		return getType().isInvincible();
	}

	public final boolean canMove() {
		return getType().canMove();
	}

	public final boolean isImmuneToMagic() {
		return getType().isImmuneToMagic();
	}

	public final boolean levitates() {
		return getType().levitates();
	}

	public final boolean canStealItems() {
		return getType().canStealItems();
	}

	public final boolean canOnlyBeKilledWhenMaterialized() {
		return getType().canOnlyBeKilledWhenMaterialized();
	}

	public final boolean isNearlyImmuneToSpells() {
		return getType().isNearlyImmuneToSpells();
	}

	public final List<Spell.Type> getSpells() {
		return getType().getSpells();
	}

	public boolean absorbItem(Item item) {
		Validate.notNull(item, "The given item is null");

		if (absorbItems()) {
			absorbedItems.add(item);

			return true;
		}

		return false;
	}

	public final List<Item> getItems() {
		final List<Item> items = new ArrayList<Item>();

		// Les objets que porte "nativement" la cr�ature
		items.addAll(getType().getItemsLeftWhenKilled());

		// Les objets qu'il a �ventuellement absorb� ou vol� !
		items.addAll(absorbedItems);

		return items;
	}

	public final boolean canCastSpell() {
		return getType().canCastSpell();
	}

	public final EnumSet<Weakness> getWeaknesses() {
		return getType().getWeaknesses();
	}

	public final int getAttackDuration() {
		return getType().getAttackDuration();
	}

	public final boolean hasSideImage() {
		return getType().hasSideImage();
	}

	public final boolean hasBackImage() {
		return getType().hasBackImage();
	}

	public final boolean hasAttackImage() {
		return getType().hasAttackImage();
	}

	public final boolean isAttackImageMirrored() {
		return getType().isAttackImageMirrored();
	}

	public final boolean isAttackImageMirroredDuringAttack() {
		return getType().isAttackImageMirroredDuringAttack();
	}

	public final int getAttackDisplayDuration() {
		return getType().getAttackDisplayDuration();
	}

	public final int getExperienceMultiplier() {
		return getType().getExperienceMultiplier();
	}

	public final int getShield() {
		return getType().getShield();
	}

	@Override
	public void onChangeEvent(ChangeEvent event) {
		if (event.getSource() == health) {
			if (health.value() == 0) {
				// Le monstre vient de mourir
				if (log.isDebugEnabled()) {
					log.debug(this + " just died");
				}

				if (dropItems()) {
					// FIXME L�cher les objets au sol
				}

				this.health.removeChangeListener(this);
			}
		}
	}

	@Override
	public String toString() {
		return getId();
	}

	/**
	 * Indique si la cr�ature peut voir la position donn�e.
	 *
	 * @return si la cr�ature peut voir la position donn�e.
	 */
	public boolean canSeePosition(Position targetPosition) {
		Validate.notNull(targetPosition, "The given position is null");

		if (getElement() == null) {
			// Cr�ature non install�e dans un donjon
			return false;
		}

		final Position currentPosition = getElement().getPosition();

		// Optimisation: On commence par v�rifier que la position cible donn�e
		// correspond bien au niveau de la cr�ature
		if (targetPosition.z != currentPosition.z) {
			return false;
		}

		// FIXME Prendre en compte la transparence des portes ou les obstacles!!

		// Positions visibles de la cr�ature ?
		final List<Position> visiblePositions = currentPosition
				.getVisiblePositions(direction);

		final List<Element> visibleElements = getElement().getLevel()
				.getElements(visiblePositions);

		for (Element element : visibleElements) {
			if (targetPosition.equals(element.getPosition())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Indique si la cr�ature peut entendre un bruit provenant de la position
	 * donn�e.
	 *
	 * @return si la cr�ature peut entendre un bruit provenant de la position
	 *         donn�e.
	 */
	public boolean canHearPosition(Position targetPosition) {
		Validate.notNull(targetPosition, "The given position is null");

		if (getElement() == null) {
			// Cr�ature non install�e dans un donjon
			return false;
		}

		final Position currentPosition = getElement().getPosition();

		// Optimisation: On commence par v�rifier que la position cible donn�e
		// correspond bien au niveau de la cr�ature
		if (targetPosition.z != currentPosition.z) {
			return false;
		}

		// FIXME Prendre en compte les obstacles !!

		// Positions audibles de la cr�ature ? Cela d�pend de l'acuit� de la
		// cr�ature
		final List<Position> audiblePositions = currentPosition
				.getSurroundingPositions(getType().getAwareness());

		final List<Element> audibleElements = getElement().getLevel()
				.getElements(audiblePositions);

		for (Element element : audibleElements) {
			if (targetPosition.equals(element.getPosition())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Indique si la {@link Creature} peut attaquer (directement) la
	 * {@link Position} donn�e.
	 *
	 * @param targetPosition
	 *            la {@link Position} attaqu�e.
	 * @return si la {@link Creature} peut attaquer (directement) la
	 *         {@link Position} donn�e.
	 */
	public boolean canAttackPosition(Position targetPosition) {
		Validate.notNull(targetPosition, "The given position is null");

		if (getElement() == null) {
			// Cr�ature non install�e dans un donjon
			return false;
		}

		final Position currentPosition = getElement().getPosition();

		// Optimisation: On commence par v�rifier que la position cible donn�e
		// correspond bien au niveau de la cr�ature
		if (targetPosition.z != currentPosition.z) {
			return false;
		}

		// FIXME Prendre en compte les obstacles !!

		final List<Position> attackablePositions;

		// Plut�t que d'appeler la m�thode canCastSpell() qui consid�re tous les
		// sorts (d'attaque ou non), on se base sur le retour de
		// getAttackSpells()
		if (!getType().getAttackSpells().isEmpty()) {
			// La cr�ature peut porter des attaques � distance, on consid�re
			// toutes les positions qu'elle peut attaquer (au contact ou �
			// distance). Port�e maximale du sort que peut lancer la cr�ature ?
			attackablePositions = currentPosition
					.getAttackablePositions(getSpellRange());
		} else {
			// La cr�ature ne peut attaquer qu'au contact, ne consid�rer que
			// les positions d'attaque proches
			attackablePositions = currentPosition.getAttackablePositions();
		}

		return attackablePositions.contains(targetPosition);
	}

	private boolean isMoveAllowed() {
		return (moveTimer.get() == 0);
	}

	private boolean isAttackAllowed() {
		return (attackTimer.get() == 0);
	}

	private void resetMoveTimer() {
		moveTimer.set(getType().getMoveDuration());
	}

	private void resetAttackTimer() {
		attackTimer.set(getType().getAttackDuration());
	}

	@Override
	public boolean clockTicked() {
		// Permet de faire "clignoter" le ZYTAZ
		this.materializer.clockTicked();

		// FIXME Pour l'instant, on ne g�re que les cr�atures de taille 4 !!
		if (!Size.FOUR.equals(getSize())) {
			log.warn("Method Creature.clockTicked() doesn't support creatures whose size is "
					+ getSize() + " (for the moment)");

			return true;
		}

		// TODO La vitesse de d�placement d'une cr�ature de taille 4 est-elle
		// double car elle ne peut que se d�placer de 2 cases � la fois ? Quid
		// pour une cr�ature de taille 2 ?

		// Remise � jour des compteurs
		if (moveTimer.get() > 0) {
			moveTimer.decrementAndGet();
		}
		if (attackTimer.get() > 0) {
			attackTimer.decrementAndGet();
		}

		if (getElement() == null) {
			// N�cessaire pour faire fonctionner les tests unitaires
			return true;
		}

		final Party party = getElement().getLevel().getDungeon().getParty();

		if (isAttackAllowed()) {
			if ((party != null) && canAttackPosition(party.getPosition())) {
				// Si la cr�ature ne fait pas face aux champions, elle se tourne
				// vers eux avant d'attaquer dans la foul�e
				final Direction directionTowardsParty = getElement()
						.getPosition().getDirectionTowards(party.getPosition());

				if (directionTowardsParty != null) {
					// Une direction a �t� trouv�e
					if (!getDirection().equals(directionTowardsParty)) {
						// La cr�ature se tourne vers les champions
						setDirection(directionTowardsParty);
					}
				}

				// Attaquer les champions
				attackParty(party);

				return true;
			}
		}

		if (isMoveAllowed()) {
			// FIXME Prendre en compte luminosit� / invisibilit� des champions
			// pour d�terminer la port�e de vue

			if ((party != null)
					&& (canSeePosition(party.getPosition()) || canHearPosition(party
							.getPosition()))) {

				// La cr�ature voit / entend les champions, elle se met en
				// chasse et se d�place vers eux
				if (moveTo(party.getPosition().x, party.getPosition().y)) {
					// Si la cr�ature peut aussi attaquer dans la foul�e, elle
					// le fait dans le m�me tour
					if (isAttackAllowed()
							&& canAttackPosition(party.getPosition())) {

						// Si la cr�ature ne fait pas face aux champions, elle
						// se tourne vers eux avant d'attaquer dans la foul�e
						final Direction directionTowardsParty = getElement()
								.getPosition().getDirectionTowards(
										party.getPosition());

						if (directionTowardsParty != null) {
							// Une direction a �t� trouv�e
							if (!getDirection().equals(directionTowardsParty)) {
								// La cr�ature se tourne vers les champions
								setDirection(directionTowardsParty);
							}
						}

						attackParty(party);
					}

					// Le d�placement peut ne pas aboutir
					return true;
				}
			}

			// La cr�ature patrouille car elle n'a rien � se mettre sous la dent
			patrol();
		}

		// TODO Animer Creature
		return true;
	}

	private void attackParty(Party party) {
		// FIXME Impl�menter attackParty(Party) (dur�e d'attaque ?)

		// R�initialiser le compteur d'attaque
		resetAttackTimer();

		// Transition vers l'�tat ATTACKING
		setState(State.ATTACKING);
	}

	private boolean moveTo(int x, int y) {
		if (!getType().canMove()) {
			// La cr�ature ne peut pas bouger
			return false;
		}

		final Element element = getElement();

		if (element == null) {
			// Cr�ature non install�e dans un donjon
			return false;
		}

		// Position actuelle de la cr�ature
		final Position position = element.getPosition();

		// Rechercher le chemin � suivre pour atteindre la position cible
		final PathFinder pathFinder = new PathFinder(element.getLevel(),
				getMateriality());
		final List<Element> path = pathFinder.findBestPath(x, y, position.x,
				position.y);

		if (path == null) {
			// Aucun chemin possible pour atteindre la cible, sortir
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Found path: " + path);
		}

		// Se diriger vers la cible (Noeud en seconde position)
		final Element node = path.get(1);

		// On calcule la direction dans laquelle la cr�ature doit se retrouver
		// au final
		final Direction directionTowardsTarget = getElement().getPosition()
				.getDirectionTowards(
						new Position(node.getPosition().x,
								node.getPosition().y, position.z));

		// La cr�ature quitte la position source
		element.creatureSteppedOff(this);

		if (directionTowardsTarget != null) {
			// Tourner la cr�ature en cours de route
			if (!getDirection().equals(directionTowardsTarget)) {
				// La cr�ature se tourne vers la cible
				setDirection(directionTowardsTarget);
			}
		}

		final Element targetElement = element.getLevel().getElement(
				node.getPosition().x, node.getPosition().y);

		// La cr�ature occupe la position cible
		targetElement.creatureSteppedOn(this);

		// R�initialiser le compteur de mouvement
		resetMoveTimer();

		// Transition vers l'�tat TRACKING
		setState(State.TRACKING);

		return true;
	}

	private void patrol() {
		if (!getType().canMove()) {
			// La cr�ature ne peut pas bouger
			return;
		}

		// La cr�ature peut bouger et se d�place. Element cible ?

		// Positions cible possibles ?
		final List<Element> surroundingElements = getElement()
				.getSurroundingElements();

		// Filtrer les positions cible selon qu'elles sont occupables
		for (Iterator<Element> it = surroundingElements.iterator(); it
				.hasNext();) {

			final Element element = it.next();

			// La cr�ature doit pouvoir "traverser" la position
			if (!element.isTraversable(this)) {
				it.remove();

				continue;
			}

			// La position ne doit pas �tre occup�e par les champions
			if (element.hasParty()) {
				it.remove();

				continue;
			}

			// La position peut-elle accueillir la cr�ature ?
			if (!element.canHost(this)) {
				it.remove();

				continue;
			}

			if (Element.Type.STAIRS.equals(element.getType())) {
				// S'il s'agit d'escaliers, la cr�ature peut-elle les prendre ?
				if (!canTakeStairs()) {
					it.remove();

					continue;
				}
			} else if (Element.Type.TELEPORTER.equals(element
					.getType())) {

				// S'il s'agit d'un t�l�porteur, la cr�ature peut-elle le
				// prendre ?
				if (!canTeleport()) {
					it.remove();

					continue;
				}
			} else if (Element.Type.PIT.equals(element.getType())) {
				// S'il s'agit d'une oubliette, la cr�ature peut-elle s'y jeter
				// (si celle-ci est ouverte !) ?
				// FIXME
			}
		}

		if (surroundingElements.isEmpty()) {
			// Impossible de d�placer la cr�ature FIXME La t�l�porter ?
			return;
		}

		// FIXME Impl�menter un changement de direction � intervalle
		// al�atoire

		// FIXME On doit privil�gier la direction dans laquelle la
		// cr�ature se trouve actuellement

		// FIXME Le d�placement est-il physiquement possible ? La
		// cr�ature n'est-elle pas g�n�e par une autre cr�ature
		// devant ?

		if (State.IDLE.equals(getState())) {
			// La cr�ature passe dans l'�tat PATROLLING
			setState(State.PATROLLING);
		}

		// ... et on la d�place
		Collections.shuffle(surroundingElements);

		final Element sourceElement = getElement();
		final Element targetElement = surroundingElements.iterator().next();

		final Direction directionTowardsTarget = getElement().getPosition()
				.getDirectionTowards(targetElement.getPosition());

		// La cr�ature quitte la position source
		sourceElement.creatureSteppedOff(this);

		if (!getDirection().equals(directionTowardsTarget)) {
			// On tourne la cr�ature dans le sens de la marche
			setDirection(directionTowardsTarget);
		}

		// La cr�ature occupe la position cible
		targetElement.creatureSteppedOn(this);

		// R�initialiser le compteur de mouvement
		resetMoveTimer();
	}

	public synchronized State getState() {
		return state;
	}

	private synchronized void setState(State state) {
		Validate.notNull(state, "The given state is null");

		if (this.state != state) {
			final State initialState = this.state;

			this.state = state;

			if (log.isDebugEnabled()) {
				log.debug(this + ".State: " + initialState + " -> "
						+ this.state);
			}
		}
	}

	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		// Le param�tre peut �tre null

		if (!ObjectUtils.equals(this.element, element)) {
			final Element initialElement = this.element;

			this.element = element;

			if (log.isDebugEnabled()) {
				log.debug(this + ".Element: " + initialElement + " -> "
						+ this.element);
			}
		}
	}

	public static void main(String[] args) {
		for (Creature.Type type : Creature.Type.values()) {
			System.out.println("Creature " + type + ": Awareness = "
					+ type.getAwareness());
		}
	}
}