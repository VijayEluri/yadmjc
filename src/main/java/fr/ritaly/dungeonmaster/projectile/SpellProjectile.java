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
package fr.ritaly.dungeonmaster.projectile;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.Direction;
import fr.ritaly.dungeonmaster.Position;
import fr.ritaly.dungeonmaster.SubCell;
import fr.ritaly.dungeonmaster.audio.AudioClip;
import fr.ritaly.dungeonmaster.audio.SoundSystem;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.magic.Spell;
import fr.ritaly.dungeonmaster.map.Door;
import fr.ritaly.dungeonmaster.map.Dungeon;
import fr.ritaly.dungeonmaster.map.Element;

/**
 * Un projectile cr�� � l'aide d'un {@link Spell}.
 * 
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public final class SpellProjectile extends AbstractProjectile {

	private final Log log = LogFactory.getLog(this.getClass());

	private final Spell spell;

	public SpellProjectile(Spell spell, Champion champion) {
		// TODO Distance � calculer
		super(champion.getParty().getDungeon(), champion.getParty()
				.getPosition(), champion.getParty().getDirection(), champion
				.getSubCell(), spell.getDuration());

		Validate.isTrue(spell.isValid(), "The given spell <" + spell.getName()
				+ "> isn't valid");
		Validate.isTrue(spell.getType().isProjectile(), "The given spell <"
				+ spell.getName() + "> isn't a projectile spell");

		this.spell = spell;
	}

	public SpellProjectile(final Spell spell, final Dungeon dungeon,
			final Position position, final Direction direction,
			final SubCell subCell) {

		// TODO Distance � calculer
		super(dungeon, position, direction, subCell, spell.getDuration());

		Validate.notNull(spell, "The given spell is null");
		Validate.isTrue(spell.isValid(), "The given spell <" + spell.getName()
				+ "> isn't valid");
		Validate.isTrue(spell.getType().isProjectile(), "The given spell <"
				+ spell.getName() + "> isn't a projectile spell");

		this.spell = spell;
	}

	@Override
	protected void projectileDied() {
		// Jouer le son TODO Le son varie selon le type de projectile
		SoundSystem.getInstance().play(getPosition(), AudioClip.FIRE_BALL);

		if (Spell.Type.OPEN_DOOR.equals(spell.getType())) {
			openDoor();
		} else if (Spell.Type.FIREBALL.equals(spell.getType())) {
			fireballExplodes();
		} else if (Spell.Type.POISON_CLOUD.equals(spell.getType())) {
			poisonCloudExplodes();
		} else {
			// TODO Impl�menter les autres types de SpellProjectile
		}

		// FIXME Appliquer les d�g�ts aux cr�atures / champions
	}

	private void poisonCloudExplodes() {
		// Cr�er un nuage de poison sur place
		dungeon.getElement(getPosition()).createPoisonCloud();
	}

	private void fireballExplodes() {
		// TODO D'autres sorts permettent-ils d'exploser une porte ?
		// (Lightning par exemple)
		final Element currentElement = dungeon.getElement(getPosition());

		if (currentElement.getType().equals(Element.Type.DOOR)) {
			// Exploser la porte si elle peut l'�tre
			final Door door = (Door) currentElement;

			// On doit tester en amont si la porte n'est pas d�j�
			// cass�e autrement �a l�ve une exception
			if (!door.isBroken()) {
				if (door.destroy()) {
					// La porte a explos�
					// TODO Conditionner le son jou� par le type
					// d'attaque de la porte. Prendre en compte la
					// force restante du sort
				}
			}
		} else {
			// TODO Faire des d�g�ts aux champions
		}
	}

	private void openDoor() {
		final Element currentElement = dungeon.getElement(getPosition());

		if (currentElement.getType().equals(Element.Type.DOOR)) {
			// Ouvrir ou fermer la porte
			final Door door = (Door) currentElement;

			if (Door.Motion.IDLE.equals(door.getMotion())) {
				if (Door.State.OPEN.equals(door.getState())) {
					// Fermer la porte
					door.close();
				} else if (Door.State.CLOSED.equals(door.getState())) {
					// Ouvrir la porte
					door.open();
				} else {
					// Pas g�r�
					throw new IllegalStateException("Unexpected door state: "
							+ door.getState());
				}
			} else if (Door.Motion.CLOSING.equals(door.getMotion())) {
				// Ouvrir la porte
				door.open();
			} else if (Door.Motion.OPENING.equals(door.getMotion())) {
				// Fermer la porte
				door.close();
			} else {
				// Pas g�r�
				throw new IllegalStateException("Unexpected door motion: "
						+ door.getMotion());
			}
		}
	}

//	@Override
//	public String toString() {
//		return getClass().getSimpleName() + "[id=" + getId() + ", position="
//				+ getPosition() + ", subCell=" + getSubCell() + ", direction="
//				+ getDirection() + ", spell=" + spell.getName() + "]";
//	}
}