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
package fr.ritaly.dungeonmaster.map;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.ClockListener;
import fr.ritaly.dungeonmaster.Temporizer;
import fr.ritaly.dungeonmaster.Utils;
import fr.ritaly.dungeonmaster.ai.Creature;
import fr.ritaly.dungeonmaster.champion.Party;

/**
 * Source: <a href="http://dmweb.free.fr/?q=node/1198">Technical Documentation -
 * Dungeon Master and Chaos Strikes Back Creature Generators</a>
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public final class Generator extends FloorTile implements ClockListener {

	public static final int PERIOD = Clock.ONE_MINUTE;

	private final Log log = LogFactory.getLog(this.getClass());

	// D�clenchement toutes les minutes (60 secondes)
	private final Temporizer temporizer = new Temporizer(
			"Generator.Temporizer", PERIOD);

	// FIXME Garder une r�f�rence sur les cr�atures cr��es pour les reg�n�rer
	// quand elles se font tuer

	private final Creature.Type creatureType;

	/**
	 * Indique si lors du dernier d�clenchement du {@link Generator} la
	 * g�n�ration des cr�atures n'a pu avoir lieu. Quand cette propri�t� vaut
	 * true, la g�n�ration est retard�e tant que les conditions ne sont pas
	 * favorables et a lieu d�s qu'elles le deviennent. De plus, le
	 * {@link Temporizer} associ� au {@link Generator} ne voit plus le temps
	 * passer.
	 */
	private boolean delayed;

	private final int healthMultiplier;

	public Generator(Creature.Type creatureType, int healthMultiplier) {
		super(Type.GENERATOR);

		Validate.notNull(creatureType, "The given creature type is null");
		Validate.isTrue(healthMultiplier >= 0, "The given health multiplier <"
				+ healthMultiplier + "> must be positive or zero");

		this.creatureType = creatureType;
		this.healthMultiplier = healthMultiplier;
	}

	@Override
	public boolean isTraversable(Party party) {
		return true;
	}

	@Override
	public boolean isTraversable(Creature creature) {
		return true;
	}

	@Override
	public boolean isTraversableByProjectile() {
		return true;
	}

	@Override
	public String getSymbol() {
		return "G";
	}

	@Override
	public void validate() throws ValidationException {
	}

	@Override
	public boolean clockTicked() {
		// Attention � l'ordre d'�valuation ici ! delayed est prioritaire !
		if (delayed || temporizer.trigger()) {
			if (!hasParty() && !hasCreatures()) {
				// L'emplacement est libre (pas de champion ou de monstre)

				// Combien doit-on g�n�rer de monstres ? Cela d�pend de la
				// taille de la cr�ature � g�n�rer !!
				final int count;

				switch (creatureType.getSize()) {
				case ONE:
					count = Utils.random(2, 4);
					break;
				case TWO:
					count = Utils.random(1, 2);
					break;
				case FOUR:
					count = 1;
					break;
				default:
					throw new RuntimeException("Unexpected creature size "
							+ creatureType.getSize());
				}

				if (log.isDebugEnabled()) {
					log.debug(this + " is generating " + count + " "
							+ creatureType + " creatures ...");
				}

				for (int i = 0; i < count; i++) {
					final int multiplier;

					// cf Technical Documentation - Dungeon Master and Chaos
					// Strikes Back Creature Generators
					if (healthMultiplier == 0) {
						// Prendre le multiplicateur d'exp�rience du niveau !
						multiplier = getLevel().getExperienceMultiplier();
					} else {
						multiplier = healthMultiplier;
					}

					// Cr�er la cr�ature
					final Creature creature = new Creature(creatureType,
							multiplier);

					// Ajouter la cr�ature � un emplacement tir� au hasard
					addCreature(creature);
				}

				delayed = false;
			} else {
				// On m�morise qu'on n'a pas pu g�n�rer les monstres, cela aura
				// lieu au "tour" d'apr�s
				if (log.isDebugEnabled()) {
					log.debug("Delaying creature generation for " + this
							+ " is occupied ...");
				}

				delayed = true;
			}
		}

		// Un g�n�rateur est toujours actif !
		return true;
	}
}