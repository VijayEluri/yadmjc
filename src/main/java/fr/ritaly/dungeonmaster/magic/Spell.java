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
package fr.ritaly.dungeonmaster.magic;

import static fr.ritaly.dungeonmaster.magic.AlignmentRune.DAIN;
import static fr.ritaly.dungeonmaster.magic.AlignmentRune.KU;
import static fr.ritaly.dungeonmaster.magic.AlignmentRune.NETA;
import static fr.ritaly.dungeonmaster.magic.AlignmentRune.RA;
import static fr.ritaly.dungeonmaster.magic.AlignmentRune.ROS;
import static fr.ritaly.dungeonmaster.magic.AlignmentRune.SAR;
import static fr.ritaly.dungeonmaster.magic.ElementRune.DES;
import static fr.ritaly.dungeonmaster.magic.ElementRune.FUL;
import static fr.ritaly.dungeonmaster.magic.ElementRune.OH;
import static fr.ritaly.dungeonmaster.magic.ElementRune.VI;
import static fr.ritaly.dungeonmaster.magic.ElementRune.YA;
import static fr.ritaly.dungeonmaster.magic.ElementRune.ZO;
import static fr.ritaly.dungeonmaster.magic.FormRune.BRO;
import static fr.ritaly.dungeonmaster.magic.FormRune.EW;
import static fr.ritaly.dungeonmaster.magic.FormRune.GOR;
import static fr.ritaly.dungeonmaster.magic.FormRune.IR;
import static fr.ritaly.dungeonmaster.magic.FormRune.KATH;
import static fr.ritaly.dungeonmaster.magic.FormRune.VEN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.RandomUtils;

import fr.ritaly.dungeonmaster.Projectile;
import fr.ritaly.dungeonmaster.Skill;
import fr.ritaly.dungeonmaster.SpellProjectile;
import fr.ritaly.dungeonmaster.Utils;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.champion.Champion.Level;
import fr.ritaly.dungeonmaster.champion.body.BodyPart;
import fr.ritaly.dungeonmaster.item.EmptyFlask;
import fr.ritaly.dungeonmaster.item.Item;
import fr.ritaly.dungeonmaster.item.ItemFactory;
import fr.ritaly.dungeonmaster.item.Potion;

/**
 * Un sort lanc� par un champion, une cr�ature ou un pi�ge. Un sort est cr�� en
 * invoquant des runes de type {@link PowerRune}, {@link ElementRune},
 * {@link FormRune} et {@link AlignmentRune}. Au minimum, le sort requiert un
 * {@link PowerRune} pour d�terminer sa puissance et un {@link ElementRune}. Le
 * sort peut aussi prendre un {@link FormRune} en plus (3 runes en tout) ou un
 * {@link FormRune} et un {@link AlignmentRune} en plus (4 runes en tout). C'est
 * la combinaison de {@link ElementRune}, {@link FormRune} et
 * {@link AlignmentRune} qui d�termine si le sort est valide: seules certaines
 * combinaisons sont valides. Si un sort n'est pas valide cela n'emp�che donc
 * <b>pas</b> de cr�er l'instance de {@link Spell}. Pour v�rifier la validit� du
 * sort, appeler la m�thode {@link #isValid()}. Pour identifier le sort, appeler
 * sa m�thode {@link #getType()}.
 * 
 * 
 * @author <a href="mailto:francois.ritaly@free.fr">Francois RITALY</a>
 */
public class Spell {

	/**
	 * Enum�ration des identifiants de sort.
	 */
	public static enum Type {
		// Sorts de pr�tre //
		HEALTH_POTION(VI, 32, 1),
		STAMINA_POTION(YA, 15, 2),
		MANA_POTION(ZO, BRO, RA, 63, 3),
		STRENGTH_POTION(FUL, BRO, KU, 15, 4),
		DEXTERITY_POTION(OH, BRO, ROS, 15, 4),
		WISDOM_POTION(YA, BRO, DAIN, 15, 4),
		VITALITY_POTION(YA, BRO, NETA, 15, 4),
		ANTIDOTE_POTION(VI, BRO, 26, 1),
		SHIELD_POTION(YA, BRO, 25, 2),
		ANTI_MAGIC(FUL, BRO, NETA, 28, 4),
		SHIELD(YA, IR, 30, 2),
		DARKNESS(DES, IR, SAR, 12, 1),
		DISPELL_ILLUSION(OH, GOR, ROS, 0, 0),
		// Sorts de magicien //
		TORCH(FUL, 15, 1),
		LIGHT(OH, IR, RA, 22, 4),
		OPEN_DOOR(ZO, 15, 1),
		MAGIC_FOOTPRINTS(YA, BRO, ROS, 18, 1),
		SEE_THROUGH_WALLS(OH, EW, RA, 33, 3),
		INVISIBILITY(OH, EW, SAR, 45, 3),
		POISON_POTION(ZO, VEN, 30, 2),
		POISON_BOLT(DES, VEN, 16, 1),
		POISON_CLOUD(OH, VEN, 27, 3),
		WEAKEN_IMMATERIAL(DES, EW, 20, 1),
		FIREBALL(FUL, IR, 42, 3),
		LIGHTNING_BOLT(OH, KATH, RA, 30, 4),
		ZO_KATH_RA(ZO, KATH, RA, 15, 0);

		/**
		 * L'identifiant du sort calcul� � partir des runes qui sont n�cessaires
		 * pour invoquer le sort.
		 */
		private final int id;

		/**
		 * La dur�e d'effet du sort en 1/6 de seconde.
		 */
		private final int duration;

		/**
		 * La difficult� � invoquer le sort. Permet de d�terminer si un
		 * {@link Champion} est assez comp�tent pour invoquer un sort donn�.
		 */
		private final int difficulty;

		private final ElementRune elementRune;

		private final FormRune formRune;

		private final AlignmentRune alignmentRune;

		private Type(ElementRune elementRune, int duration, int difficulty) {
			Validate.isTrue(elementRune != null,
					"The given element rune is null");
			Validate.isTrue(duration >= 0, "The given duration <" + duration
					+ "> must be positive or zero");
			Validate.isTrue(difficulty >= 0, "The given difficulty <"
					+ difficulty + "> must be positive or zero");

			this.id = elementRune.getId();
			this.duration = duration;
			this.difficulty = difficulty;

			this.elementRune = elementRune;
			this.formRune = null;
			this.alignmentRune = null;
		}

		private Type(ElementRune elementRune, FormRune formRune, int duration,
				int difficulty) {

			Validate.isTrue(elementRune != null,
					"The given element rune is null");
			Validate.isTrue(formRune != null, "The given form rune is null");
			Validate.isTrue(duration >= 0, "The given duration <" + duration
					+ "> must be positive or zero");
			Validate.isTrue(difficulty >= 0, "The given difficulty <"
					+ difficulty + "> must be positive or zero");

			this.id = elementRune.getId() * 10 + formRune.getId();
			this.duration = duration;
			this.difficulty = difficulty;

			this.elementRune = elementRune;
			this.formRune = formRune;
			this.alignmentRune = null;
		}

		private Type(ElementRune elementRune, FormRune formRune,
				AlignmentRune alignmentRune, int duration, int difficulty) {

			Validate.isTrue(elementRune != null,
					"The given element rune is null");
			Validate.isTrue(formRune != null, "The given form rune is null");
			Validate.isTrue(alignmentRune != null,
					"The given alignment rune is null");
			Validate.isTrue(duration >= 0, "The given duration <" + duration
					+ "> must be positive or zero");
			Validate.isTrue(difficulty >= 0, "The given difficulty <"
					+ difficulty + "> must be positive or zero");

			this.id = (elementRune.getId() * 10 + formRune.getId()) * 10
					+ alignmentRune.getId();
			this.duration = duration;
			this.difficulty = difficulty;

			this.elementRune = elementRune;
			this.formRune = formRune;
			this.alignmentRune = alignmentRune;
		}

		/**
		 * Retourne l'instance de {@link Type} associ�e � l'identifiant de sort
		 * donn�.
		 * 
		 * @param id
		 *            un entier repr�sentant un identifiant de sort.
		 * @return une instance de {@link Type} ou null si l'identifiant donn�
		 *         est invalide.
		 */
		public static Type byValue(int id) {
			for (Type anId : values()) {
				if (anId.id == id) {
					return anId;
				}
			}

			return null;
		}

		/**
		 * Indique si ce type de sort produit un projectile (missile).
		 * 
		 * @return si ce type de sort produit un projectile (missile).
		 */
		public boolean isProjectile() {
			switch (this) {
			case FIREBALL:
			case LIGHTNING_BOLT:
			case OPEN_DOOR:
			case POISON_BOLT:
			case POISON_CLOUD:
			case WEAKEN_IMMATERIAL:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Indique si ce type de sort produit une potion.
		 * 
		 * @return si ce type de sort produit une potion.
		 */
		public boolean isPotion() {
			switch (this) {
			case ANTIDOTE_POTION:
			case DEXTERITY_POTION:
			case HEALTH_POTION:
			case MANA_POTION:
			case POISON_POTION:
			case SHIELD_POTION:
			case STAMINA_POTION:
			case STRENGTH_POTION:
			case VITALITY_POTION:
			case WISDOM_POTION:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Indique si l'identifiant de sort donn� est valide.
		 * 
		 * @param id
		 *            un entier repr�sentant un identifiant de sort.
		 * @return si l'identifiant de sort donn� est valide.
		 */
		public static boolean isValid(int id) {
			return (byValue(id) != null);
		}

		/**
		 * Retourne le niveau requis dans la comp�tence du sort (cf
		 * {@link #getSkill()}) pour r�ussir celui-ci.
		 * 
		 * @return une instance de {@link Level}. Ne retourne jamais null.
		 */
		public Champion.Level getRequiredLevel() {
			switch (this) {
			case HEALTH_POTION:
			case STAMINA_POTION:
			case STRENGTH_POTION:
			case DEXTERITY_POTION:
			case WISDOM_POTION:
			case VITALITY_POTION:
			case ANTIDOTE_POTION:
			case LIGHT:
			case OPEN_DOOR:
			case INVISIBILITY:
			case LIGHTNING_BOLT:
				// Niveau 3
				return Champion.Level.APPRENTICE;
			case MANA_POTION:
			case ZO_KATH_RA:
				// Niveau 1
				return Champion.Level.NEOPHYTE;
			case SHIELD_POTION:
			case ANTI_MAGIC:
			case SHIELD:
			case DARKNESS:
			case SEE_THROUGH_WALLS:
			case POISON_POTION:
			case POISON_BOLT:
			case POISON_CLOUD:
				// Niveau 5
				return Champion.Level.CRAFTSMAN;
			case TORCH:
			case FIREBALL:
				// Niveau 2
				return Champion.Level.NOVICE;
			case MAGIC_FOOTPRINTS:
			case WEAKEN_IMMATERIAL:
				// Niveau 4
				return Champion.Level.JOURNEYMAN;
			default:
				// Niveau 0
				return Champion.Level.NONE;
			}
		}

		/**
		 * Retourne la comp�tence mise en oeuvre lors de l'invocation de ce
		 * sort. Permet de faire gagner de l'exp�rience au champion qui r�ussit
		 * le sort. Peut retourner null si aucune comp�tence particuli�re n'est
		 * requise.
		 * 
		 * @return une instance de {@link Skill} ou null.
		 */
		public Skill getSkill() {
			switch (this) {
			case HEALTH_POTION:
			case STAMINA_POTION:
			case STRENGTH_POTION:
			case DEXTERITY_POTION:
			case WISDOM_POTION:
			case VITALITY_POTION:
			case ANTIDOTE_POTION:
				// Comp�tence #13
				return Skill.HEAL;
			case MANA_POTION:
				// Comp�tence #2
				return Skill.PRIEST;
			case SHIELD_POTION:
			case ANTI_MAGIC:
			case SHIELD:
			case DARKNESS:
			case SEE_THROUGH_WALLS:
				// Comp�tence #15
				return Skill.DEFEND;
			case TORCH:
			case FIREBALL:
				// Comp�tence #16
				return Skill.FIRE;
			case LIGHT:
			case OPEN_DOOR:
			case INVISIBILITY:
			case LIGHTNING_BOLT:
				// Comp�tence #17
				return Skill.AIR;
			case MAGIC_FOOTPRINTS:
			case WEAKEN_IMMATERIAL:
				// Comp�tence #18
				return Skill.EARTH;
			case POISON_POTION:
			case POISON_BOLT:
			case POISON_CLOUD:
				// Comp�tence #19
				return Skill.WATER;
			case ZO_KATH_RA:
				// Comp�tence #3
				return Skill.WIZARD;
			default:
				// Aucune comp�tence sp�ciale requise !
				return null;
			}
		}

		/**
		 * Retourne les {@link Rune}s composant le sort (n'inclut pas le rune de
		 * puissance).
		 * 
		 * @return une {@link List} de {@link Rune}s. Ne retourne jamais null.
		 */
		public List<Rune> getRunes() {
			final List<Rune> runes = new ArrayList<Rune>(3);
			runes.add(elementRune);

			if (formRune != null) {
				runes.add(formRune);

				if (alignmentRune != null) {
					runes.add(alignmentRune);
				}
			}

			return runes;
		}

		/**
		 * Indique si l'invocation du sort requiert une fiole vide pour r�ussir.
		 * 
		 * @return si l'invocation du sort requiert une fiole vide pour r�ussir.
		 */
		public boolean requiresEmptyFlask() {
			switch (this) {
			case ANTIDOTE_POTION:
			case DEXTERITY_POTION:
			case HEALTH_POTION:
			case MANA_POTION:
			case POISON_POTION:
			case SHIELD_POTION:
			case STAMINA_POTION:
			case STRENGTH_POTION:
			case VITALITY_POTION:
			case WISDOM_POTION:
				return true;
			default:
				return false;
			}
		}

		public int getDuration() {
			return duration;
		}

		public int getDifficulty() {
			return difficulty;
		}

		/**
		 * Retourne le nom du sort � partir des {@link Rune}s le composant.
		 * Exemple: "FUL", "FUL IR", etc.
		 * 
		 * @return une {@link String} repr�sentant le nom complet du sort.
		 */
		public String getName() {
			final StringBuilder builder = new StringBuilder(32);

			builder.append(elementRune.name());

			if (formRune != null) {
				builder.append(' ');
				builder.append(formRune.name());

				if (alignmentRune != null) {
					builder.append(' ');
					builder.append(alignmentRune.name());
				}
			}

			return builder.toString();
		}
	}

	/**
	 * Le {@link Rune} de puissance du sort. Forc�ment non null.
	 */
	private final PowerRune powerRune;

	/**
	 * Le {@link Rune} de type {@link ElementRune} du sort. Forc�ment non null.
	 */
	private final ElementRune elementRune;

	/**
	 * Le {@link Rune} de type {@link FormRune} du sort. Peut �tre null.
	 */
	private final FormRune formRune;

	/**
	 * Le {@link Rune} de type {@link AlignmentRune} du sort. Peut �tre null.
	 */
	private final AlignmentRune alignmentRune;

	/**
	 * L'identifiant du sort sous forme d'entier. Sa valeur est calcul�e �
	 * partir des {@link ElementRune}, {@link FormRune} et {@link AlignmentRune}
	 * du sort. Ne prend donc pas en compte le {@link Rune} de puissance du sort
	 * !
	 */
	private final int id;

	/**
	 * Le co�t (en nombre de points de mana) n�cessaires pour invoquer le sort.
	 */
	private final int cost;

	public Spell(PowerRune powerRune, ElementRune elementRune,
			FormRune formRune, AlignmentRune alignmentRune) {

		if (powerRune == null) {
			throw new IllegalArgumentException("The given power rune is null");
		}
		if (elementRune == null) {
			throw new IllegalArgumentException("The given element rune is null");
		}
		if (formRune == null) {
			throw new IllegalArgumentException("The given form rune is null");
		}
		if (alignmentRune == null) {
			throw new IllegalArgumentException(
					"The given alignment rune is null");
		}

		this.powerRune = powerRune;
		this.elementRune = elementRune;
		this.formRune = formRune;
		this.alignmentRune = alignmentRune;
		this.id = computeId(elementRune, formRune, alignmentRune);
		this.cost = computeCost(powerRune, elementRune, formRune, alignmentRune);
	}

	public Spell(PowerRune powerRune, ElementRune elementRune, FormRune formRune) {
		if (powerRune == null) {
			throw new IllegalArgumentException("The given power rune is null");
		}
		if (elementRune == null) {
			throw new IllegalArgumentException("The given element rune is null");
		}
		if (formRune == null) {
			throw new IllegalArgumentException("The given form rune is null");
		}

		this.powerRune = powerRune;
		this.elementRune = elementRune;
		this.formRune = formRune;
		this.alignmentRune = null; // Rune non aliment�
		this.id = computeId(elementRune, formRune, alignmentRune);
		this.cost = computeCost(powerRune, elementRune, formRune, alignmentRune);
	}

	public Spell(PowerRune powerRune, ElementRune elementRune) {
		if (powerRune == null) {
			throw new IllegalArgumentException("The given power rune is null");
		}
		if (elementRune == null) {
			throw new IllegalArgumentException("The given element rune is null");
		}

		this.powerRune = powerRune;
		this.elementRune = elementRune;
		this.formRune = null; // Rune non aliment�
		this.alignmentRune = null; // Rune non aliment�
		this.id = computeId(elementRune, formRune, alignmentRune);
		this.cost = computeCost(powerRune, elementRune, formRune, alignmentRune);
	}

	/**
	 * Calcule et retourne l'identifiant du sort sous forme d'entier � partir
	 * des {@link Rune}s composant le sort.
	 * 
	 * @param elementRune
	 *            un {@link ElementRune}. Ne peut �tre null.
	 * @param formRune
	 *            un {@link FormRune}. Peut �tre null.
	 * @param alignmentRune
	 *            un {@link AlignmentRune}. Peut �tre null.
	 * @return un entier positif identifiant le sort. Example: 1, 64, etc.
	 */
	private int computeId(ElementRune elementRune, FormRune formRune,
			AlignmentRune alignmentRune) {

		Validate.notNull(elementRune, "The given element rune is null");

		int id = elementRune.getId();

		if (formRune != null) {
			id = (id * 10) + formRune.getId();

			if (alignmentRune != null) {
				id = (id * 10) + alignmentRune.getId();
			}
		}

		return id;
	}

	/**
	 * Calcule et retourne le nombre de points de mana n�cessaires pour invoquer
	 * le sort sous forme d'entier � partir des {@link Rune}s composant le sort.
	 * 
	 * @param powerRune
	 *            un {@link PowerRune}. Ne peut �tre null.
	 * @param elementRune
	 *            un {@link ElementRune}. Ne peut �tre null.
	 * @param formRune
	 *            un {@link FormRune}. Peut �tre null.
	 * @param alignmentRune
	 *            un {@link AlignmentRune}. Peut �tre null.
	 * @return un entier positif repr�sentant un nombre de points de mana (le
	 *         "co�t" du sort).
	 */
	private int computeCost(PowerRune powerRune, ElementRune elementRune,
			FormRune formRune, AlignmentRune alignmentRune) {

		Validate.notNull(powerRune, "The given power rune is null");
		Validate.notNull(elementRune, "The given element rune is null");

		int cost = powerRune.getCost() + elementRune.getCost(powerRune);

		if (formRune != null) {
			cost += formRune.getCost(powerRune);

			if (alignmentRune != null) {
				cost += alignmentRune.getCost(powerRune);
			}
		}

		return cost;
	}

	/**
	 * Retourne le nom du sort � partir des {@link Rune}s le composant. Exemple:
	 * "LO FUL", "MON FUL IR", etc.
	 * 
	 * @return une {@link String} repr�sentant le nom complet du sort.
	 */
	public String getName() {
		final StringBuilder builder = new StringBuilder(32);

		builder.append(powerRune.name());
		builder.append(' ');
		builder.append(elementRune.name());

		if (formRune != null) {
			builder.append(' ');
			builder.append(formRune.name());

			if (alignmentRune != null) {
				builder.append(' ');
				builder.append(alignmentRune.name());
			}
		}

		return builder.toString();
	}

	public PowerRune getPower() {
		return powerRune;
	}

	/**
	 * Retourne le type du sort. Calcul� � partir des runes qui rentrent en jeu
	 * pour le formuler.
	 * 
	 * @return une instance de {@link Type} ou null si le sort n'est pas valide.
	 */
	public Type getType() {
		// Retourne null si le sort est invalide
		return Type.byValue(id);
	}

	/**
	 * Retourne la comp�tence mise en oeuvre lors de l'invocation de ce sort.
	 * Permet de faire gagner de l'exp�rience au champion qui r�ussit le sort.
	 * Peut retourner null si aucune comp�tence particuli�re n'est requise pour
	 * invoquer le sort.
	 * 
	 * @return une instance de {@link Skill} ou null si le sort est invalide ou
	 *         si aucune comp�tence particuli�re n'est requise pour invoquer le
	 *         sort.
	 */
	public Skill getSkill() {
		final Type spellType = getType();

		return (spellType != null) ? spellType.getSkill() : null;
	}

	/**
	 * Retourne le nombre de runes utilis� pour formuler ce sort.
	 * 
	 * @return un entier.
	 */
	public int getRuneCount() {
		// Runes power + element forc�ment pr�sents
		int count = 2;

		if (formRune != null) {
			count++;

			if (alignmentRune != null) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Retourne les {@link Rune}s du sort (dont le rune de puissance).
	 * 
	 * @return une {@link List} de {@link Rune}s. Ne retourne jamais null.
	 */
	public List<Rune> getRunes() {
		final List<Rune> runes = new ArrayList<Rune>(4);
		runes.add(powerRune);
		runes.add(elementRune);

		if (formRune != null) {
			runes.add(formRune);

			if (alignmentRune != null) {
				runes.add(alignmentRune);
			}
		}

		return runes;
	}

	/**
	 * Retourne le co�t (en nombre de points de mana) d'invocation du sort.
	 * 
	 * @return un entier positif repr�sentant un nombre de points de mana.
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Retourne le nombre de points d'exp�rience gagn�s pour avoir r�ussi �
	 * invoquer ce sort.
	 * 
	 * @return un entier positif repr�sentant un nombre de points d'exp�rience
	 *         ou z�ro si le sort n'est pas valide.
	 */
	public int getEarnedExperience() {
		if (isValid()) {
			// Sort valide, l'exp�rience gagn�e d�pend de la difficult� du sort
			// et de son co�t TODO A affiner

			// Attention car la difficult� peut �tre nulle (cf ZO KATH RA)
			final int difficulty = getDifficulty();

			if (difficulty > 0) {
				return 5 + RandomUtils.nextInt(difficulty);
			}

			return 5;
		}

		// Sort rat�, aucune exp�rience gagn�e
		return 0;
	}

	/**
	 * Retourne la difficult� � invoquer ce sort sous forme d'un nombre.
	 * 
	 * @return un entier positif ou -1 si le sort est non valide.
	 */
	public int getDifficulty() {
		// La difficult� d�pend du rune de puissance et de la difficult� de base
		// du sort
		final Type spellType = getType();

		if (spellType != null) {
			return powerRune.getDifficultyMultiplier()
					* spellType.getDifficulty();
		}

		// Sort non valide
		return -1;
	}

	/**
	 * Retourne le temps en 1/6 de secondes pendant lequel le {@link Champion}
	 * qui a lanc� ce sort ne peut en lancer un autre.
	 * 
	 * @return un entier positif repr�sentant un nombre de "tics" d'horloge ou
	 *         -1 si le sort est non valide.
	 */
	public int getDuration() {
		final Type spellType = getType();

		return (spellType != null) ? spellType.getDuration() : -1;
	}

	/**
	 * Indique si le sort formul� est valide.
	 * 
	 * @return si le sort formul� est valide.
	 */
	public boolean isValid() {
		return Type.isValid(id);
	}

	/**
	 * Indique si le {@link Champion} donn� est assez comp�tent pour invoquer ce
	 * sort. Le r�sultat d�pend de la comp�tence du {@link Champion} dans la
	 * comp�tence requise pour invoquer ce sort ainsi que de la difficult� du
	 * sort !
	 * 
	 * @param champion
	 *            un {@link Champion} dont on cherche � savoir s'il est assez
	 *            comp�tent pour invoquer ce sort.
	 * @return si le {@link Champion} donn� est assez comp�tent pour invoquer ce
	 *         sort.
	 */
	public boolean canBeCastBy(Champion champion) {
		Validate.notNull(champion, "The given champion is null");
		Validate.isTrue(isValid(), "The spell isn't valid");

		final Skill skill = getSkill();
		
		if (skill == null) {
			// Aucune comp�tence requise pour invoquer le sort
			return true;
		}
		
		final Champion.Level requiredLevel = getType().getRequiredLevel();
		final Champion.Level actualLevel = champion.getLevel(skill);

		return (actualLevel.compareTo(requiredLevel) >= 0);
	}

	/**
	 * Fait agir le {@link Spell} sur le {@link Champion} donn�.
	 * 
	 * @param champion
	 *            un {@link Champion} sur lequel faire agir le sort.
	 * @throws EmptyFlaskNeededException
	 *             si le sort requiert une flasque vide pour r�ussir.
	 * @throws EmptyHandNeededException
	 *             si le sort demande que l'une des mains du {@link Champion}
	 *             soit vide.
	 */
	public void actUpon(Champion champion) throws EmptyFlaskNeededException,
			EmptyHandNeededException {

		Validate.notNull(champion);
		Validate.isTrue(isValid(), "The spell isn't valid");

		switch (getType()) {
		case ANTIDOTE_POTION:
		case DEXTERITY_POTION:
		case HEALTH_POTION:
		case MANA_POTION:
		case POISON_POTION:
		case SHIELD_POTION:
		case STAMINA_POTION:
		case STRENGTH_POTION:
		case VITALITY_POTION:
		case WISDOM_POTION: {
			// Premi�re fiole vide port�e par le champion ?
			final EmptyFlask emptyFlask;
			final BodyPart bodyPart;

			final Item item1 = champion.getBody().getWeaponHand().getItem();

			if ((item1 != null)
					&& item1.getType().equals(Item.Type.EMPTY_FLASK)) {

				emptyFlask = (EmptyFlask) item1;
				bodyPart = champion.getBody().getWeaponHand();
			} else {
				final Item item2 = champion.getBody().getShieldHand().getItem();

				if ((item2 != null)
						&& item2.getType().equals(Item.Type.EMPTY_FLASK)) {

					emptyFlask = (EmptyFlask) item2;
					bodyPart = champion.getBody().getShieldHand();
				} else {
					emptyFlask = null;
					bodyPart = null;
				}
			}

			if (getType().requiresEmptyFlask() && (emptyFlask == null)) {
				// N'arrive normalement pas car cette condition a �t� v�rifi�e
				// en amont !
				throw new EmptyFlaskNeededException();
			}

			// Remplacer la fiole vide par une potion
			bodyPart.putOn(new Potion(this));
			break;
		}
		case TORCH:
			// Augmenter la lumi�re g�n�r�e par le h�ros
			champion.getSpells().getLight()
					.inc(Utils.random(20, 30) * getPower().getPowerLevel());
			break;
		case ZO_KATH_RA:
			// L'une des mains du champion doit �tre vide
			final boolean shieldHandEmpty = champion.getBody().getShieldHand()
					.isEmpty();
			final boolean weaponHandEmpty = champion.getBody().getWeaponHand()
					.isEmpty();

			if (!shieldHandEmpty && !weaponHandEmpty) {
				// Les deux mains sont pleines, le sort ne peut r�ussir
				throw new EmptyHandNeededException();
			}

			// Placer un item de type ZO_KATH_RA dans la main vide du champion
			final Item zokathra = ItemFactory.getFactory().newItem(
					Item.Type.ZOKATHRA_SPELL);

			if (shieldHandEmpty) {
				champion.getBody().getShieldHand().putOn(zokathra);
			} else {
				champion.getBody().getWeaponHand().putOn(zokathra);
			}

			// FIXME G�rer le cas de la main vide: KICK, CRY, PUNCH !!
			break;
		case LIGHT:
			// Augmenter la lumi�re g�n�r�e par le h�ros
			champion.getSpells().getLight()
					.inc(Utils.random(20, 30) * getPower().getPowerLevel());
			break;
		case OPEN_DOOR: {
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		case DARKNESS:
			// FIXME Impl�menter actUpon(Champion)
			throw new UnsupportedOperationException("Unsupported spell <"
					+ getType() + ">");
		case DISPELL_ILLUSION:
			champion.getParty().getSpells().getDispellIllusion()
					.inc(powerRune.getPowerLevel() * Utils.random(10, 15));
			break;
		case ANTI_MAGIC:
			champion.getParty().getSpells().getAntiMagic()
					.inc(powerRune.getPowerLevel() * Utils.random(10, 15));
			break;
		case FIREBALL: {
			// Cr�er une boule de feu
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		case INVISIBILITY:
			champion.getParty().getSpells().getInvisibility()
					.inc(powerRune.getPowerLevel() * Utils.random(10, 15));
			break;
		case LIGHTNING_BOLT: {
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		case MAGIC_FOOTPRINTS:
			// FIXME Impl�menter actUpon(Champion)
			throw new UnsupportedOperationException("Unsupported spell <"
					+ getType() + ">");
		case POISON_BOLT: {
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		case POISON_CLOUD: {
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		case SEE_THROUGH_WALLS:
			// FIXME Impl�menter actUpon(Champion)
			throw new UnsupportedOperationException("Unsupported spell <"
					+ getType() + ">");
		case SHIELD:
			champion.getParty().getSpells().getShield()
					.inc(powerRune.getPowerLevel() * Utils.random(10, 15));
			break;
		case WEAKEN_IMMATERIAL: {
			// FIXME Changer mani�re de cr�er un projectile
			final Projectile projectile = new SpellProjectile(this, champion);
			break;
		}
		default:
			throw new UnsupportedOperationException("Unsupported spell <"
					+ getType() + ">");
		}
	}
}