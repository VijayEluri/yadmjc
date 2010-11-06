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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.Direction;
import fr.ritaly.dungeonmaster.HasPosition;
import fr.ritaly.dungeonmaster.PoisonCloud;
import fr.ritaly.dungeonmaster.Position;
import fr.ritaly.dungeonmaster.SubCell;
import fr.ritaly.dungeonmaster.Teleport;
import fr.ritaly.dungeonmaster.actuator.Actuator;
import fr.ritaly.dungeonmaster.ai.Creature;
import fr.ritaly.dungeonmaster.champion.Party;
import fr.ritaly.dungeonmaster.event.ChangeEvent;
import fr.ritaly.dungeonmaster.event.ChangeEventSource;
import fr.ritaly.dungeonmaster.event.ChangeEventSupport;
import fr.ritaly.dungeonmaster.event.ChangeListener;
import fr.ritaly.dungeonmaster.item.Item;
import fr.ritaly.dungeonmaster.projectile.Projectile;

/**
 * Un �l�ment permettant de construire des niveaux de donjon.
 * 
 * @author <a href="mailto:francois.ritaly@free.fr">Francois RITALY</a>
 */
public abstract class Element implements ChangeEventSource, HasPosition {

	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Enum�ration des diff�rents types d'�l�ment disponibles.
	 */
	public static enum Type {
		/**
		 * Un �l�ment de sol
		 */
		FLOOR,

		/**
		 * Une dalle de sol qui peut d�clencher un {@link Actuator}.
		 */
		FLOOR_SWITCH,

		/**
		 * Un mur avec un bouton qui peut d�clencher un {@link Actuator}.
		 */
		WALL_SWITCH,

		/**
		 * Un mur avec une serrure qui peut d�clencher un {@link Actuator}.
		 */
		WALL_LOCK,

		/**
		 * Un mur avec une fente (� pi�ces) qui peut d�clencher un
		 * {@link Actuator}.
		 */
		WALL_SLOT,

		/**
		 * Un mur simple
		 */
		WALL,

		/**
		 * Un mur qui peut dispara�tre lorsqu'on l'actionne.
		 */
		RETRACTABLE_WALL,

		/**
		 * Un mur invisible
		 */
		INVISIBLE_WALL,

		/**
		 * Un mur factice que l'on peut traverser
		 */
		FAKE_WALL,

		/**
		 * Un pilier de salle
		 */
		PILLAR,

		/**
		 * Une porte
		 */
		DOOR,
		/**
		 * Une oubliette
		 */
		PIT,
		TELEPORTER,
		STAIRS,
		FOUR_SIDE_ALCOVE,
		ALCOVE,
		FOUNTAIN,
		/** Un mur avec un levier */
		LEVER,
		/** Un mur avec une torche */
		TORCH_WALL,

		/**
		 * Un autel pour r�ssusciter les champions
		 */
		ALTAR,

		/**
		 * Un mur avec une inscription
		 */
		TEXT_WALL,

		/**
		 * Un portrait de champion
		 */
		PORTRAIT,

		/**
		 * Un mur avec une d�coration
		 */
		DECORATED_WALL,

		/**
		 * Une dalle de sol avec une d�coration
		 */
		DECORATED_FLOOR,

		/**
		 * Un g�n�rateur de monstres
		 */
		GENERATOR,
		PROJECTILE_LAUNCHER;

		/**
		 * Indique si l'�l�ment est "en dur". C'est le cas d'un mur au sens
		 * large (mur simple, mur d�cor�) mais pas d'un mur invisible ou d'un
		 * faux mur. Permet de d�terminer si un �l�ment peut �tre utilis� en
		 * bordure de niveau et d'en valider la structure.
		 * 
		 * @return si l'�l�ment est "en dur".
		 */
		public boolean isConcrete() {
			switch (this) {
			case ALCOVE:
			case FOUNTAIN:
			case FOUR_SIDE_ALCOVE:
			case LEVER:
			case TORCH_WALL:
			case WALL:
			case WALL_LOCK:
			case WALL_SLOT:
			case WALL_SWITCH:
			case ALTAR:
			case DECORATED_WALL:
			case TEXT_WALL:
			case PORTRAIT:
			case PROJECTILE_LAUNCHER:
				return true;
			case FAKE_WALL:
			case DOOR:
			case FLOOR:
			case FLOOR_SWITCH:
			case INVISIBLE_WALL:
			case PILLAR:
			case RETRACTABLE_WALL:
			case PIT:
			case STAIRS:
			case TELEPORTER:
			case DECORATED_FLOOR:
			case GENERATOR:
				return false;
			default:
				throw new UnsupportedOperationException(
						"Method non supported for type " + this);
			}
		}
	}

	/**
	 * Le niveau auquel appartient l'�l�ment.
	 */
	private Level level;

	/**
	 * La position de l'�l�ment.
	 */
	private Position position;

	/**
	 * Le type de l'�l�ment.
	 */
	private final Type type;

	/**
	 * Le groupe de champions qui occupe l'�l�ment (si c'est pertinent).
	 */
	private Party party;

	// FIXME Conditionner l'instanciation de cette classe par le fait que
	// l'Element peut accueillir des creatures
	private final CreatureManager creatureManager = new CreatureManager(this);

	private Map<SubCell, Projectile> projectiles;
	
	private List<PoisonCloud> poisonClouds;

	/**
	 * Les objets de l'�l�ment (s'il peut en avoir). Ce membre est aliment� lors
	 * du d�p�t du premier objet et mis � null lors du ramassage du dernier
	 * objet (optimisation).
	 */
	private Map<SubCell, Stack<Item>> items;

	private final ChangeEventSupport eventSupport = new ChangeEventSupport();

	protected Element(Type type) {
		if (type == null) {
			throw new IllegalArgumentException("The given type is null");
		}

		this.type = type;
	}

	/**
	 * D�pose l'objet donn� sur l'emplacement indiqu�.
	 * 
	 * @param item
	 *            l'objet � d�poser.
	 * @param subCell
	 *            l'emplacement de d�p�t.
	 */
	public synchronized void itemDroppedDown(Item item, SubCell subCell) {
		// M�thode non finale afin de permettre � certaines impl�mentations de
		// Element de la surcharger afin de lancer une
		// UnsupportedOperationException
		if (item == null) {
			throw new IllegalArgumentException("The given item is null");
		}
		if (subCell == null) {
			throw new IllegalArgumentException("The given corner is null");
		}

		if (items == null) {
			// Cr�er la Map � la vol�e
			items = new EnumMap<SubCell, Stack<Item>>(SubCell.class);
		}

		Stack<Item> stack = items.get(subCell);

		if (stack == null) {
			// Cr�er l'instance � la vol�e
			items.put(subCell, stack = new Stack<Item>());
		}

		// D�poser l'objet
		stack.push(item);

		if (log.isDebugEnabled()) {
			log.debug(item + " dropped on " + getId() + " at " + subCell);
		}

		// Notification
		afterItemDropped(item, subCell);

		fireChangeEvent();
	}

	public final boolean itemPickedUp(Item item) {
		return pickItem(getSubCell(item)) == item;
	}

	/**
	 * Ramasse un objet situ� � l'emplacement donn�.
	 * 
	 * @param subCell
	 *            l'emplacement d'o� l'on tente de r�cup�rer un objet.
	 * @return une instance de {@link Item} repr�sentant l'objet ramass� ou null
	 *         s'il n'y en avait aucun.
	 */
	public synchronized Item pickItem(SubCell subCell) {
		if (subCell == null) {
			throw new IllegalArgumentException("The given corner is null");
		}

		if (items != null) {
			final Stack<Item> stack = items.get(subCell);

			if (stack != null) {
				// Retirer l'objet au-dessus de la pile
				final Item item = stack.pop();

				// Optimisation m�moire
				if (stack.isEmpty()) {
					items.remove(subCell);

					if (items.isEmpty()) {
						items = null;
					}
				}

				if (log.isDebugEnabled()) {
					log.debug(item + " picked from " + getId() + " at "
							+ subCell);
				}

				// Notification
				afterItemPicked(item, subCell);

				fireChangeEvent();

				return item;
			}
		}

		return null;
	}

	public final Type getType() {
		return type;
	}

	public Level getLevel() {
		return level;
	}

	void setLevel(Level level) {
		// level peut �tre null (retrait d'un �l�ment de son niveau de
		// rattachement)
		this.level = level;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	void setPosition(Position position) {
		// position peut �tre null (retrait d'un �l�ment de son niveau de
		// rattachement)
		this.position = position;
	}

	/**
	 * Indique si l'�l�ment peut �tre travers� par le groupe de champions.
	 * 
	 * @param party
	 *            le groupe de champions.
	 * @return si l'�l�ment peut �tre travers� par le groupe de champions.
	 */
	public abstract boolean isTraversable(Party party);

	/**
	 * Indique si la {@link Creature} donn�e peut traverse cet �l�ment. Le
	 * retour de la m�thode d�pend de la nature (mat�rielle, immat�rielle) de la
	 * cr�ature donn�e et / ou de l'�tat de l'�l�ment.
	 * 
	 * @param creature
	 *            une {@link Creature}.
	 * @return si la {@link Creature} donn�e peut traverse cet �l�ment.
	 */
	public abstract boolean isTraversable(Creature creature);

	/**
	 * Indique si l'�l�ment peut �tre travers� par un projectile.
	 * 
	 * @return si l'�l�ment peut �tre travers� par un projectile.
	 */
	public abstract boolean isTraversableByProjectile();

	/**
	 * Notifie l'�lement qu'un objet vient d'�tre d�pos� � l'emplacement donn�.
	 * 
	 * @param item
	 *            l'objet d�pos�.
	 * @param subCell
	 *            l'emplacement de d�p�t.
	 */
	protected void afterItemDropped(Item item, SubCell subCell) {
	}

	/**
	 * Notifie l'�l�ment qu'un objet vient d'�tre ramass� de l'emplacement
	 * donn�.
	 * 
	 * @param item
	 *            l'objet ramass�.
	 * @param subCell
	 *            l'emplacement de ramassage.
	 */
	protected void afterItemPicked(Item item, SubCell subCell) {
	}

	public final void creatureSteppedOn(Creature creature, SubCell subCell) {
		creatureManager.creatureSteppedOn(creature, subCell);
	}

	public final void projectileArrived(Projectile projectile, SubCell subCell) {

		if (projectile == null) {
			throw new IllegalArgumentException("The given projectile is null");
		}
		if (subCell == null) {
			throw new IllegalArgumentException("The given sub-cell is null");
		}
		if (!isTraversableByProjectile() && !Type.DOOR.equals(getType())) {
			// Une porte peut accueillir un projectile m�me si celle-ci est
			// ferm�e afin qu'il puisse exploser
			throw new UnsupportedOperationException(
					"The projectile can't arrive on " + getId());
		}

		if (log.isDebugEnabled()) {
			log.debug(projectile.getId() + " arrived on " + getId()
					+ " (subCell: " + subCell + ")");
		}

		if (projectiles == null) {
			// Cr�er la Map � la vol�e
			projectiles = new EnumMap<SubCell, Projectile>(SubCell.class);
		}

		// L'emplacement doit initialement �tre vide
		if (projectiles.get(subCell) != null) {
			throw new IllegalArgumentException("The cell " + subCell
					+ " of element " + getId()
					+ " is already occupied by a projectile ("
					+ projectiles.get(subCell) + ")");
		}

		// M�moriser le projectile
		projectiles.put(subCell, projectile);

		afterProjectileArrived(projectile);
	}

	public final void projectileLeft(Projectile projectile, SubCell subCell) {
		if (projectile == null) {
			throw new IllegalArgumentException("The given projectile is null");
		}
		if (subCell == null) {
			throw new IllegalArgumentException("The given sub-cell is null");
		}
		if (!isTraversableByProjectile() && !Type.DOOR.equals(getType())) {
			// Une porte peut accueillir un projectile m�me si celle-ci est
			// ferm�e afin qu'il puisse exploser
			throw new UnsupportedOperationException("The projectile "
					+ projectile.getId() + " can't leave " + getId());
		}

		if (log.isDebugEnabled()) {
			log.debug(projectile.getId() + " left " + getId() + " (subCell: "
					+ subCell + ")");
		}

		final Projectile removed = projectiles.remove(subCell);

		if (removed != projectile) {
			throw new IllegalArgumentException("Removed: " + removed
					+ " / Projectile: " + projectile + " / SubCell: " + subCell);
		}

		if (projectiles.isEmpty()) {
			// Purger la Map � la vol�e
			projectiles = null;
		}

		afterProjectileLeft(projectile);
	}

	protected void afterProjectileLeft(Projectile projectile) {
	}

	protected void afterProjectileArrived(Projectile projectile) {
	}

	protected void afterCreatureSteppedOn(Creature creature) {
	}

	protected void afterCreatureSteppedOff(Creature creature) {
	}

	public final SubCell getSubCell(Creature creature) {
		return creatureManager.getSubCell(creature);
	}

	public final SubCell getSubCell(Item item) {
		Validate.notNull(item, "The given item is null");

		if (items == null) {
			return null;
		}

		for (SubCell subCell : items.keySet()) {
			final Stack<Item> stack = items.get(subCell);

			if ((stack != null) && stack.contains(item)) {
				return subCell;
			}
		}

		// Creature introuvable
		return null;
	}

	public final void creatureSteppedOff(Creature creature, SubCell subCell) {
		creatureManager.creatureSteppedOff(creature, subCell);
	}

	/**
	 * Notifie l'�l�ment que le groupe de champions vient d'arriver sur sa
	 * position.
	 * 
	 * @param party
	 *            une instance de {@link Party} repr�sentant le groupe de
	 *            champions.
	 */
	public final void partySteppedOn(Party party) {
		if (party == null) {
			throw new IllegalArgumentException("The given party is null");
		}
		if (!isTraversable(party)) {
			throw new UnsupportedOperationException(
					"The party can't step on element " + getId());
		}

		if (log.isDebugEnabled()) {
			log.debug("Party stepped on " + getId());
		}

		// M�moriser la r�f�rence
		this.party = party;

		afterPartySteppedOn();
	}

	protected void afterPartySteppedOn() {
	}

	protected void afterPartySteppedOff(Party party) {
	}

	/**
	 * Notifie l'�l�ment que le groupe de champions vient de tourner sur place.
	 * Note: Cette m�thode permet � un �lement de type STAIRS de d�placer le
	 * groupe de champions quand celui-ci tourne sur lui-m�me.
	 */
	public void partyTurned() {
		if (party == null) {
			throw new IllegalStateException("The party isn't on " + getId());
		}

		if (log.isDebugEnabled()) {
			log.debug("Party turned on " + getId());
		}
	}

	/**
	 * Notifie l'�l�ment que le groupe de champions vient de quitter sa
	 * position.
	 */
	public final void partySteppedOff() {
		if (this.party == null) {
			throw new IllegalStateException("The party isn't located on this "
					+ getId());
		}
		if (!isTraversable(party)) {
			throw new UnsupportedOperationException(
					"The party can't step off element " + type);
		}

		// R�initialiser la r�f�rence
		final Party backup = this.party;
		this.party = null;

		if (log.isDebugEnabled()) {
			log.debug("Party stepped off " + getId());
		}

		afterPartySteppedOff(backup);
	}

	/**
	 * Indique si le groupe de champions occupe cette position.
	 * 
	 * @return si le groupe de champions occupe cette position.
	 */
	public boolean hasParty() {
		return (party != null);
	}

	/**
	 * Indique si l'�l�ment est occup� par au moins une cr�ature.
	 * 
	 * @return si l'�l�ment est occup� par au moins une cr�ature.
	 */
	public boolean hasCreatures() {
		return creatureManager.hasCreatures();
	}

	/**
	 * Indique si l'�l�ment est occup� par au moins un projectile.
	 * 
	 * @return si l'�l�ment est occup� par au moins un projectile.
	 */
	public boolean hasProjectiles() {
		return (projectiles != null) && !projectiles.isEmpty();
	}

	/**
	 * Indique si l'�l�ment est vide, c'est-�-dire non occup� par des cr�atures,
	 * par le groupe de champions ou tout autre chose qui emp�cherait de s'y
	 * placer.
	 * 
	 * @return si l'�l�ment est vide.
	 */
	public boolean isEmpty() {
		return !hasParty() && !hasCreatures();
	}

	@Override
	public final String toString() {
		if (position != null) {
			return this.type.name() + position;
		} else {
			return this.type.name() + "[?:?,?]";
		}
	}

	/**
	 * Retourne le groupe de champions s'il occupe cette position ou null.
	 * 
	 * @return une instance de {@link Party} ou null.
	 */
	public final Party getParty() {
		return party;
	}

	/**
	 * Retourne les cr�atures occupant cet �l�ment sous forme de Map.
	 * 
	 * @return une Map&lt;SubCell, Creature&gt. Cette m�thode ne retourne jamais
	 *         null.
	 */
	public final Map<SubCell, Creature> getCreatureMap() {
		// Ne pas utiliser en dehors des tests unitaires (acc�s trop bas niveau)
		// Utiliser getCreatures() � la place
		return creatureManager.getCreatureMap();
	}

	/**
	 * Retourne les cr�atures occupant cet �l�ment sous forme de {@link List}.
	 * 
	 * @return une Set&lt;Creature&gt. Cette m�thode ne retourne jamais null.
	 */
	public final Set<Creature> getCreatures() {
		return creatureManager.getCreatures();
	}

	public final Map<SubCell, Projectile> getProjectiles() {
		if (projectiles == null) {
			return Collections.emptyMap();
		}

		// Recopie d�fensive
		return Collections.unmodifiableMap(projectiles);
	}

	/**
	 * Retourne la cr�ature occupant l'emplacement donn� s'il y a lieu.
	 * 
	 * @param subCell
	 *            l'emplacement sur lequel rechercher la cr�ature.
	 * @return une instance de {@link Creature} ou null s'il n'y en a aucune �
	 *         cet emplacement.
	 */
	public final Creature getCreature(SubCell subCell) {
		return creatureManager.getCreature(subCell);
	}

	@Override
	public final void addChangeListener(ChangeListener listener) {
		eventSupport.addChangeListener(listener);
	}

	@Override
	public final void removeChangeListener(ChangeListener listener) {
		eventSupport.addChangeListener(listener);
	}

	protected final void fireChangeEvent() {
		eventSupport.fireChangeEvent(new ChangeEvent(this));
	}

	/**
	 * Indique si l'�l�ment est "en dur". C'est le cas d'un mur au send large
	 * (mur simple, mur d�cor�) mais pas d'un mur invisible ou d'un faux mur.
	 * Permet de d�terminer si un �l�ment peut �tre utilis� en bordure de
	 * niveau.
	 * 
	 * @return si l'�l�ment est "en dur".
	 */
	public final boolean isConcrete() {
		return type.isConcrete();
	}

	/**
	 * Retourne l'identifiant de cet �l�ment sous forme de {@link String}.
	 * 
	 * @return un {@link String} identifiant cet �l�ment.
	 */
	public abstract String getCaption();

	public final String getId() {
		if (position != null) {
			return this.type.name() + position;
		} else {
			return this.type.name() + "[?:?,?]";
		}
	}

	/**
	 * Retourne tous les objets au sol sur cet �l�ment.
	 * 
	 * @return une List&lt;Item&gt;. Cette m�thode ne retourne jamais null.
	 */
	public final List<Item> getItems() {
		if (items != null) {
			// items != null -> Il y a forc�ment au moins un objet au sol
			final List<Item> list = new ArrayList<Item>();

			for (Stack<Item> stack : items.values()) {
				list.addAll(stack);
			}

			return list;
		}

		// Aucun objet au sol
		return Collections.emptyList();
	}

	public final int getItemCount() {
		if (items != null) {
			// items != null -> Il y a forc�ment au moins un objet au sol
			int count = 0;

			for (Stack<Item> stack : items.values()) {
				count += stack.size();
			}

			return count;
		}

		// Aucun objet au sol
		return 0;
	}

	public final int getCreatureCount() {
		return creatureManager.getCreatureCount();
	}

	public final int getItemCount(SubCell subCell) {
		Validate.notNull(subCell, "The given sub-cell is null");

		if (items != null) {
			final Stack<Item> stack = items.get(subCell);

			return (stack != null) ? stack.size() : 0;
		}

		// Aucun objet au sol
		return 0;
	}

	/**
	 * Retourne les objets situ�s � l'emplacement donn� s'il y a lieu.
	 * 
	 * @param subCell
	 *            l'emplacement o� sont situ�s les objets recherch�s.
	 * @return une List&lt;Item&gt; contenant les objets trouv�s. Cette m�thode
	 *         ne retourne jamais null.
	 */
	public List<Item> getItems(SubCell subCell) {
		Validate.isTrue(subCell != null, "The given sub-celle is null");

		if (items != null) {
			final List<Item> list = items.get(subCell);

			if (list != null) {
				// Recopie d�fensive
				return new ArrayList<Item>(list);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Indique si l'�l�ment comporte des objets.
	 * 
	 * @return si l'�l�ment comporte des objets.
	 */
	public boolean hasItem() {
		if (items != null) {
			for (Stack<Item> stack : items.values()) {
				if (!stack.isEmpty()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Calcule et retourne une instance de {@link Teleport} indiquant comment
	 * d�placer un groupe de champions se d�pla�ant dans la direction donn�e.
	 * Dans la majorit� des cas, le groupe se retrouvera sur l'�l�ment situ�
	 * dans la direction donn�e mais pour certains �l�ments (t�l�porteurs,
	 * escaliers) le d�placement du groupe n'est pas aussi simple.
	 * 
	 * @param direction
	 *            la {@link Direction} dans laquelle se d�place le groupe de
	 *            champions.
	 * @return une instance de {@link Teleport} indiquant comment d�placer le
	 *         groupe de champions.
	 */
	public Teleport getTeleport(Direction direction) {
		Validate.isTrue(direction != null, "The given direction is null");

		if (!hasParty()) {
			throw new IllegalStateException("The party isn't on this element");
		}

		// Dans le cas g�n�ral, l'�l�ment ne modifie pas la position finale
		return new Teleport(getPosition().towards(direction), getParty()
				.getLookDirection());
	}

	// /**
	// * Notifie l'�l�ment que le groupe de champions qui l'occupe est sur le
	// * point de bouger dans la direction donn�e et retourne la position finale
	// * du groupe. Cette m�thode est sp�cialement con�ue pour la classe
	// * {@link Stairs} qui a un mode de fonctionnement un peu particulier.
	// *
	// * @param direction
	// * la {@link Direction} de d�placement du groupe.
	// * @return la {@link Position} finale apr�s d�placement du groupe.
	// */
	// public Position computeTargetPosition(Direction direction) {
	// Validate.isTrue(direction != null, "The given direction is null");
	// if (!hasParty()) {
	// throw new IllegalStateException("The party isn't on this element");
	// }
	//
	// // Dans le cas g�n�ral, l'�l�ment ne modifie pas la position finale
	// return getPosition().towards(direction);
	// }
	//
	// public Direction computeTargetDirection(Direction direction) {
	// Validate.isTrue(direction != null, "The given direction is null");
	// if (!hasParty()) {
	// throw new IllegalStateException("The party isn't on this element");
	// }
	//
	// // Dans le cas g�n�ral, l'�l�ment ne modifie pas la direction du groupe
	// return getParty().getLookDirection();
	// }

	/**
	 * Calcule et retourne la place libre restante pour accueillir de nouvelles
	 * {@link Creature}s sous forme d'un entier (repr�sentant un nombre de
	 * {@link SubCell}s).
	 * 
	 * @return un entier dans l'intervalle [0-4] repr�sentant le nombre de
	 *         {@link SubCell}s libres.
	 */
	public int getFreeRoom() {
		return creatureManager.getFreeRoom();
	}

	/**
	 * Retourne les {@link SubCell}s occup�es par les {@link Creature}s
	 * pr�sentes sur cet {@link Element}.
	 * 
	 * @return un EnumSet&lt;SubCell&gt;. Ne retourne jamais null.
	 */
	public EnumSet<SubCell> getOccupiedSubCells() {
		return creatureManager.getOccupiedSubCells();
	}

	/**
	 * Retourne les {@link SubCell}s libres de cet {@link Element}.
	 * 
	 * @return un EnumSet&lt;SubCell&gt;. Ne retourne jamais null.
	 */
	public EnumSet<SubCell> getFreeSubCells() {
		return creatureManager.getFreeSubCells();
	}

	/**
	 * Indique si cet {@link Element} peut accueillir la {@link Creature} donn�e
	 * compte tenu de sa taille et de la place restante.
	 * 
	 * @param creature
	 *            une {@link Creature}.
	 * @return si cet {@link Element} peut accueillir la {@link Creature} donn�e
	 *         compte tenu de sa taille et de la place restante.
	 */
	public boolean canHost(Creature creature) {
		Validate.notNull(creature);

		final int room = getFreeRoom();
		final int creatureSize = creature.getSize().value();

		if (creatureSize > room) {
			// Plus assez de place pour accueillir la cr�ature
			return false;
		}

		// Dans le cas o� la place restante est de 2 et la taille de la cr�ature
		// �galement de 2, il faut s'assurer qu'il s'agit de SubCell voisines
		// qui permettent r�ellement d'accueillir la cr�ature !
		if ((room == 2) && (creatureSize == 2)) {
			final Iterator<SubCell> iterator = getFreeSubCells().iterator();

			final SubCell cell1 = iterator.next();
			final SubCell cell2 = iterator.next();

			if (!cell1.isNeighbourOf(cell2)) {
				return false;
			}
		}

		return true;
	}

	public abstract void validate() throws ValidationException;

	// FIXME Cr�er m�thode Element.setVisited(boolean) pour magic footprints

	protected final Position getPartyPosition() {
		final Level level = getLevel();

		if (level != null) {
			final Dungeon dungeon = level.getDungeon();

			if (dungeon != null) {
				final Party party = dungeon.getParty();

				if (party != null) {
					return party.getPosition();
				}
			}
		}

		return null;
	}

	public final void creatureSteppedOn(Creature creature) {
		creatureManager.creatureSteppedOn(creature);
	}

	public boolean hasCreature(Creature creature) {
		return creatureManager.hasCreature(creature);
	}

	public final void creatureSteppedOff(Creature creature) {
		creatureManager.creatureSteppedOff(creature);
	}

	public final void creatureSteppedOn(Creature creature, Direction direction) {
		creatureManager.creatureSteppedOn(creature, direction);
	}

	public final void creatureSteppedOff(Creature creature, Direction direction) {
		creatureManager.creatureSteppedOff(creature, direction);
	}

	protected final CreatureManager getCreatureManager() {
		return creatureManager;
	}

	public Object removeCreature(Creature creature) {
		return creatureManager.removeCreature(creature);
	}

	public void addCreature(Creature creature, Object location) {
		creatureManager.addCreature(creature, location);
	}

	public void addCreature(Creature creature) {
		creatureManager.addCreature(creature);
	}
	
	public boolean hasPoisonClouds() {
		return (poisonClouds != null) && !poisonClouds.isEmpty();
	}
	
	public int getPoisonCloudCount() {
		if (poisonClouds != null) {
			return poisonClouds.size();
		}
		
		return 0;
	}
	
	// TODO Prendre en compte la force du nuage de poison en param�tre
	public void createPoisonCloud() {
		if (this.poisonClouds == null) {
			this.poisonClouds = new ArrayList<PoisonCloud>();
		}
		
//		if (log.isDebugEnabled()) {
//			log.debug("Creating new poison cloud on " + this + " ...");
//		}
		
		final PoisonCloud poisonCloud = new PoisonCloud(this);

		// S'enregistrer pour savoir quand le nuage dispara�t
		poisonCloud.addChangeListener(new ChangeListener() {
			@Override
			public void onChangeEvent(ChangeEvent event) {
				if (log.isDebugEnabled()) {
					log.debug(event.getSource() + " vanished into thin air");
				}
				
				poisonClouds.remove(event.getSource());
				
				if (poisonClouds.isEmpty()) {
					poisonClouds = null;
				}
			}
		});
		
		// M�moriser le nuage
		this.poisonClouds.add(poisonCloud);
		
		if (log.isDebugEnabled()) {
			log.debug("Created a new poison cloud on " + this);
		}
		
		// Enregistrer ce nuage
		Clock.getInstance().register(poisonCloud);
	}
}