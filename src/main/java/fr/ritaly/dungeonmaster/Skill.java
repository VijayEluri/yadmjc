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
package fr.ritaly.dungeonmaster;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import fr.ritaly.dungeonmaster.stat.Stats;

/**
 * Enumerates the possible champion skills. A {@link Skill} is either "basic" or
 * "hidden". There are 4 basic skills ({@link #FIGHTER}, {@link #NINJA},
 * {@link #PRIEST} and {@link #WIZARD}) and 16 hidden skills (4 per basic
 * skill).<br>
 * <br>
 * Source: <a href="http://dmweb.free.fr/?q=node/691">Technical Documentation -
 * Dungeon Master and Chaos Strikes Back Skills and Statistics</a>
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public enum Skill {
	/** Basic skill */
	FIGHTER,
	/** Basic skill */
	NINJA,
	/** Basic skill */
	PRIEST,
	/** Basic skill */
	WIZARD,

	/** Hidden fighter skills */
	SWING,
	/** Hidden fighter skills */
	THRUST,
	/** Hidden fighter skills */
	CLUB,
	/** Hidden fighter skills */
	PARRY,

	/** Hidden ninja skills */
	STEAL,
	/** Hidden ninja skills */
	FIGHT,
	/** Hidden ninja skills */
	THROW,
	/** Hidden ninja skills */
	SHOOT,

	/** Hidden priest skills */
	IDENTIFY,
	/** Hidden priest skills */
	HEAL,
	/** Hidden priest skills */
	INFLUENCE,
	/** Hidden priest skills */
	DEFEND,

	/** Hidden wizard skills */
	FIRE,
	/** Hidden wizard skills */
	AIR,
	/** Hidden wizard skills */
	EARTH,
	/** Hidden wizard skills */
	WATER;

	/**
	 * Tells whether this {@link Skill} is basic (or hidden).
	 *
	 * @return whether this {@link Skill} is basic (or hidden).
	 */
	public boolean isBasic() {
		switch (this) {
		case FIGHTER:
		case NINJA:
		case PRIEST:
		case WIZARD:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Tells whether this {@link Skill} is hidden (or basic).
	 *
	 * @return whether this {@link Skill} is hidden (or basic).
	 */
	public boolean isHidden() {
		return !isBasic();
	}

	/**
	 * Returns the basic {@link Skill} mapped to this hidden {@link Skill}. This
	 * method throws an {@link UnsupportedOperationException} if this skill
	 * isn't basic.
	 *
	 * @return a {@link Skill}.
	 */
	public Skill getRelatedSkill() {
		if (isBasic()) {
			throw new UnsupportedOperationException();
		}

		switch (this) {
		case SWING:
		case THRUST:
		case CLUB:
		case PARRY:
			return FIGHTER;
		case STEAL:
		case FIGHT:
		case THROW:
		case SHOOT:
			return NINJA;
		case IDENTIFY:
		case HEAL:
		case INFLUENCE:
		case DEFEND:
			return PRIEST;
		case FIRE:
		case AIR:
		case EARTH:
		case WATER:
			return WIZARD;
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Tells whether the champion's strength improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's strength improves when the champion gains
	 *         a new level of this {@link Skill}.
	 */
	boolean improvesStrength() {
		return equals(FIGHTER) || equals(NINJA);
	}

	/**
	 * Tells whether the champion's dexterity improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's dexterity improves when the champion gains
	 *         a new level of this {@link Skill}.
	 */
	boolean improvesDexterity() {
		return equals(FIGHTER) || equals(NINJA);
	}

	/**
	 * Tells whether the champion's mana improves when the champion gains a new
	 * level of this {@link Skill}.
	 *
	 * @return whether the champion's mana improves when the champion gains a
	 *         new level of this {@link Skill}.
	 */
	boolean improvesMana() {
		return equals(PRIEST) || equals(WIZARD);
	}

	/**
	 * Tells whether the champion's wisdom improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's wisdom improves when the champion gains a
	 *         new level of this {@link Skill}.
	 */
	boolean improvesWisdom() {
		return equals(PRIEST) || equals(WIZARD);
	}

	/**
	 * Tells whether the champion's anti-magic improves when the champion gains
	 * a new level of this {@link Skill}.
	 *
	 * @return whether the champion's anti-magic improves when the champion
	 *         gains a new level of this {@link Skill}.
	 */
	boolean improvesAntiMagic() {
		return equals(PRIEST) || equals(WIZARD);
	}

	/**
	 * Tells whether the champion's health improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's health improves when the champion gains a
	 *         new level of this {@link Skill}.
	 */
	boolean improvesHealth() {
		return true;
	}

	/**
	 * Tells whether the champion's stamina improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's stamina improves when the champion gains a
	 *         new level of this {@link Skill}.
	 */
	boolean improvesStamina() {
		return true;
	}

	/**
	 * Tells whether the champion's vitality improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's vitality improves when the champion gains
	 *         a new level of this {@link Skill}.
	 */
	boolean improvesVitality() {
		return true;
	}

	/**
	 * Tells whether the champion's anti-fire improves when the champion gains a
	 * new level of this {@link Skill}.
	 *
	 * @return whether the champion's anti-fire improves when the champion gains
	 *         a new level of this {@link Skill}.
	 */
	boolean improvesAntiFire() {
		return true;
	}

	/**
	 * Returns this {@link Skill}'s label. Example: Returns "Ninja" for the
	 * {@link #NINJA} skill.
	 *
	 * @return a {@link String}.
	 */
	public String getLabel() {
		return StringUtils.capitalize(name().toLowerCase());
	}

	/**
	 * Randomly improves the given stats. The stats improved depend on the
	 * skill.
	 *
	 * @param stats
	 *            the stats to improve. Can't be null.
	 */
	public void improve(Stats stats) {
		Validate.notNull(stats, "The given stats is null");

		if (improvesHealth()) {
			final int healthBonus = Utils.random(5, 15);

			stats.getHealth().incMax(healthBonus);
			stats.getHealth().inc(healthBonus);
		}
		if (improvesStamina()) {
			final int staminaBonus = Utils.random(5, 15);

			stats.getStamina().incMax(staminaBonus);
			stats.getStamina().inc(staminaBonus);
		}
		if (improvesVitality()) {
			final int vitalityBonus = Utils.random(5, 15);

			stats.getVitality().incMax(vitalityBonus);
			stats.getVitality().inc(vitalityBonus);
		}
		if (improvesAntiFire()) {
			final int antiFireBonus = Utils.random(5, 15);

			stats.getAntiFire().incMax(antiFireBonus);
			stats.getAntiFire().inc(antiFireBonus);
		}
		if (improvesStrength()) {
			final int strengthBonus = Utils.random(5, 15);

			stats.getStrength().incMax(strengthBonus);
			stats.getStrength().inc(strengthBonus);
		}
		if (improvesDexterity()) {
			final int dexterityBonus = Utils.random(5, 15);

			stats.getDexterity().incMax(dexterityBonus);
			stats.getDexterity().inc(dexterityBonus);
		}
		if (improvesMana()) {
			final int manaBonus = Utils.random(5, 15);

			stats.getMana().incMax(manaBonus);
			stats.getMana().inc(manaBonus);
		}
		if (improvesWisdom()) {
			final int wisdomBonus = Utils.random(5, 15);

			stats.getWisdom().incMax(wisdomBonus);
			stats.getWisdom().inc(wisdomBonus);
		}
		if (improvesAntiMagic()) {
			final int antiMagicBonus = Utils.random(5, 15);

			stats.getAntiMagic().incMax(antiMagicBonus);
			stats.getAntiMagic().inc(antiMagicBonus);
		}
	}
}
