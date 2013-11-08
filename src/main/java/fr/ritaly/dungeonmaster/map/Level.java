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
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.ClockListener;
import fr.ritaly.dungeonmaster.Constants;
import fr.ritaly.dungeonmaster.Position;
import fr.ritaly.dungeonmaster.ai.Creature;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.map.Element.Type;
import fr.ritaly.dungeonmaster.projectile.Projectile;

/**
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public class Level {

	private final Log log = LogFactory.getLog(Level.class);

	private final int level;

	private final int width;

	private final int height;

	private final Element[][] elements;

	private final Dungeon dungeon;

	/**
	 * Le multiplicateur par lequel l'exp�rience gagn�e par un {@link Champion}
	 * doit �tre multipli�e pour ce niveau-ci.
	 */
	private int experienceMultiplier = 1;

	/**
	 * La luminosit� ambiante propre au niveau. Vaut en g�n�ral 0 sauf pour les
	 * premiers niveaux d'un donjon.
	 */
	private int ambiantLight;

	public Level(Dungeon dungeon, int number, int height, int width) {
		if (dungeon == null) {
			throw new IllegalArgumentException("The given dungeon is null");
		}
		if (number <= 0) {
			throw new IllegalArgumentException("The given level number <"
					+ number + "> must be positive");
		}
		if (height <= 0) {
			throw new IllegalArgumentException("The given height <" + height
					+ "> must be positive");
		}
		if (width <= 0) {
			throw new IllegalArgumentException("The given width <" + width
					+ "> must be positive");
		}

		this.dungeon = dungeon;
		this.level = number;
		this.height = height;
		this.width = width;
		this.elements = new Element[width][height];

		init();
	}

	public void init() {
		if (log.isDebugEnabled()) {
			log.debug("Initializing level " + level + " ...");
		}

		// Placer des murs autour du niveau et des vides � l'int�rieur
		surround(ElementFactory.WALL_FACTORY);
		fill(ElementFactory.FLOOR_FACTORY);

		if (log.isInfoEnabled()) {
			log.info("Initialized level " + level);
		}
	}

	/**
	 * Supprime tous les �l�ments du niveau.
	 */
	public void clear() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final Element removed = elements[x][y];

				if (removed != null) {
					// D�tacher l'�l�ment du niveau
					removed.setLevel(null);
					removed.setPosition(null);

					elements[x][y] = null;
				}
			}
		}
	}

	public void fill(ElementFactory factory) {
		Validate.notNull(factory, "The given element factory is null");

		for (int x = 0; x < width; x++) {
			final boolean borderX = (x == 0) || (x == width - 1);

			for (int y = 0; y < height; y++) {
				final boolean borderY = (y == 0) || (y == height - 1);

				if (!borderX && !borderY) {
					setElement(x, y, factory.createElement());
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Filled level " + level + " with " + factory.getType()
					+ " elements");
		}
	}

	public void surround(ElementFactory factory) {
		Validate.notNull(factory, "The given element factory is null");

		for (int x = 0; x < width; x++) {
			final boolean borderX = (x == 0) || (x == width - 1);

			for (int y = 0; y < height; y++) {
				final boolean borderY = (y == 0) || (y == height - 1);

				if (borderX || borderY) {
					setElement(x, y, factory.createElement());
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Surrounded level " + level + " with "
					+ factory.getType() + " elements");
		}
	}

	private void checkX(int x) {
		if ((x < 0) || (x > width - 1)) {
			throw new IllegalArgumentException(
					"The given x must be in range [0-" + (width - 1) + "]");
		}
	}

	private void checkY(int y) {
		if ((y < 0) || (y > height - 1)) {
			throw new IllegalArgumentException(
					"The given y must be in range [0-" + (height - 1) + "]");
		}
	}
	
	public Element getElement(int x, int y) {
		return getElement(x, y, true);
	}

	public Element getElement(int x, int y, boolean fail) {
		if (fail) {
			checkX(x);
			checkY(y);

			return elements[x][y];			
		} else {
			if ((x >= 0) && (x <= width - 1) && (y >= 0) && (y <= height - 1)) {
				return elements[x][y];
			}
			
			return null;
		}
	}

	public int getLevel() {
		return level;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setElement(int x, int y, Element element) {
		checkX(x);
		checkY(y);
		if (element == null) {
			throw new IllegalArgumentException("The given element is null");
		}

		final Element removed = elements[x][y];

		if (removed != null) {
			// D�tacher l'�l�ment du niveau
			removed.setLevel(null);
			removed.setPosition(null);

			elements[x][y] = null;

			if (removed instanceof ClockListener) {
				Clock.getInstance().unregister((ClockListener) element);
			}
		}

		// Attacher l'�l�ment au niveau
		element.setLevel(this);
		element.setPosition(new Position(x, y, level));

		// Permuter les �l�ments
		elements[x][y] = element;

		if (element instanceof ClockListener) {
			// R�f�rencer le ClockListener
			Clock.getInstance().register((ClockListener) element);
		}
	}

	public Dungeon getDungeon() {
		return dungeon;
	}

	/**
	 * Retourne la luminosit� ambiante propre au niveau.
	 * 
	 * @return un entier positif ou nul dans l'intervalle [0-255].
	 */
	public int getAmbiantLight() {
		return ambiantLight;
	}

	/**
	 * D�finit la luminosit� ambiante propre au niveau.
	 * 
	 * @param ambiantLight
	 *            un entier positif ou nul dans l'intervalle [0-255].
	 */
	public void setAmbiantLight(int ambiantLight) {
		if ((ambiantLight < 0) || (ambiantLight > Constants.MAX_LIGHT)) {
			throw new IllegalArgumentException("The given ambiant light <"
					+ ambiantLight + "> must be in range [0-"
					+ Constants.MAX_LIGHT + "]");
		}

		this.ambiantLight = ambiantLight;
	}

	public int getExperienceMultiplier() {
		return experienceMultiplier;
	}

	public void setExperienceMultiplier(int experienceMultiplier) {
		if (experienceMultiplier <= 0) {
			throw new IllegalArgumentException("The experience multiplier <"
					+ experienceMultiplier + "> must be positive");
		}

		this.experienceMultiplier = experienceMultiplier;
	}

	/**
	 * Indique si la {@link Position} donn�e est situ�e sur ce niveau et est
	 * valide (ses valeurs de x et y sont valides).
	 * 
	 * @param position
	 *            la {@link Position} � tester.
	 * @return si la {@link Position} donn�e est situ�e sur ce niveau et est
	 *         valide (ses valeurs de x et y sont valides).
	 */
	public boolean contains(Position position) {
		Validate.notNull(position, "The given position is null");

		if (position.z != level) {
			// C'est une position sur un niveau diff�rent
			return false;
		}

		if ((position.x < 0) || (position.x > width - 1)) {
			// Valeur x hors intervalle
			return false;
		}

		if ((position.y < 0) || (position.y > height - 1)) {
			// Valeur y hors intervalle
			return false;
		}

		// Position situ�e sur ce niveau et valide
		return true;
	}

	public void validate() throws ValidationException {
		// Tous les �l�ments doivent �tre positionn�s !
		for (int x = 0; x < width; x++) {
			final boolean wallX = (x == 0) || (x == width - 1);

			for (int y = 0; y < height; y++) {
				final boolean wallY = (y == 0) || (y == height - 1);

				final Element element = elements[x][y];

				if (element == null) {
					// Il ne doit y avoir aucun �l�ment nul
					throw new ValidationException("The element at [" + x + ","
							+ y + "] isn't set");
				}

				if (wallX || wallY) {
					// Il doit y avoir des "murs" tout autour du niveau
					if (!element.isConcrete()) {
						throw new ValidationException("The element at [" + x
								+ "," + y + "] must be concrete");
					}
				}

				element.validate();
			}
		}
	}

	public String draw() {
		return draw(null);
	}
	
	public String draw(List<Element> path) {
		final StringBuilder builder = new StringBuilder(1024);
		
		final Element start;
		final Element goal;
		final boolean pathDefined = ((path != null) && (path.size() >= 2));
		
		if (pathDefined) {
			start = path.get(0);
			goal = path.get(path.size() - 1);
		} else {
			start = null;
			goal = null;
		}

		for (int y = 0; y < height; y++) {
			if (y == 0) {
				builder.append("+");

				for (int x = 0; x < width; x++) {
					builder.append("---+");
				}

				builder.append("\n");
			}

			builder.append("|");

			for (int x = 0; x < width; x++) {
				final Element element = getElement(x, y);

				builder.append(" ");
				
				final boolean drawn;
				
				if (pathDefined) {
					if (element.equals(start)) {
						builder.append("S");
						
						drawn = true;
					} else if (element.equals(goal)) {
						builder.append("G");
						
						drawn = true;
					} else if (path.contains(element)) {
						builder.append(".");
						
						drawn = true;
					} else {
						drawn = false;
					}
				} else {
					drawn = false;
				}
				
				if (!drawn) {
					if (element != null) {
						// Pour all�ger le r�sultat g�n�r�, les sols sont
						// repr�sent�s comme " "
						if (Type.FLOOR.equals(element.getType())) {
							builder.append(" ");
						} else {
							builder.append(element.getSymbol());
						}
					} else {
						builder.append("?");
					}
				}

				builder.append(" |");
			}

			builder.append("\n");
			builder.append("+");

			for (int x = 0; x < width; x++) {
				builder.append("---+");
			}

			// Fin de rang�e
			builder.append("\n");
		}

		return builder.toString();
	}

	/**
	 * Returns the projectiles located on this {@link Level}.
	 * 
	 * @return a {@link List} of {@link Projectile}s. Never returns null.
	 */
	public List<Projectile> getProjectiles() {
		final List<Projectile> projectiles = new ArrayList<Projectile>();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				projectiles.addAll(getElement(x, y).getProjectiles().values());
			}
		}

		return projectiles;
	}

	/**
	 * Returns the creatures located on this {@link Level}.
	 * 
	 * @return a {@link List} of {@link Creatures}s. Never returns null.
	 */
	public List<Creature> getCreatures() {
		final List<Creature> creatures = new ArrayList<Creature>();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				creatures.addAll(getElement(x, y).getCreatures());
			}
		}

		return creatures;
	}
	
	/**
	 * Retourne le nombre de cr�atures situ�es sur ce niveau.
	 * 
	 * @return un entier positif ou nul.
	 */
	public int getCreatureCount() {
		int count = 0;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				count += getElement(x, y).getCreatureCount();
			}
		}

		return count;
	}

	// TODO M�thode de r�cup�ration des projectiles du niveau
	
	public List<Element> getElements(List<Position> positions) {
		Validate.notNull(positions);
		
		final List<Element> result = new ArrayList<Element>(positions.size());
		
		for (Position position : positions) {
			// On doit v�rifier la valeur de z associ�e � la position !!
			if (position.z != this.level) {
				continue;
			}
			
			final Element element = getElement(position.x, position.y, false);
			
			if (element != null) {
				result.add(element);
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		final Dungeon dungeon = new Dungeon();
		System.out.println(dungeon.createLevel(1, 10, 5).draw());
	}
}