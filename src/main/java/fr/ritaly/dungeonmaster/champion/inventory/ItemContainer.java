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
package fr.ritaly.dungeonmaster.champion.inventory;

import java.util.List;

import fr.ritaly.dungeonmaster.event.ChangeListener;
import fr.ritaly.dungeonmaster.item.Item;

/**
 * A container of items with a fixed capacity. Each item inside the container
 * can be accessed with a 0-based index.
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public interface ItemContainer {

	/**
	 * Returns the container's fixed capacity (that is, the number of items the
	 * container can store).
	 *
	 * @return a positive integer.
	 */
	public int getCapacity();

	public void addChangeListener(ChangeListener listener);

	public void removeChangeListener(ChangeListener listener);

	/**
	 * Returns the actual number of items inside the container.
	 *
	 * @return a positive integer within [0,capacity].
	 */
	public int getItemCount();

	/**
	 * Returns the items inside the container.
	 *
	 * @return a list of items. Never returns null.
	 */
	public List<Item> getItems();

	/**
	 * Tells whether the container is full.
	 *
	 * @return whether the container is full.
	 * @see #getCapacity()
	 * @see #getItemCount()
	 */
	public boolean isFull();

	/**
	 * Tells whether the container is empty.
	 *
	 * @return whether the container is empty.
	 * @see #getItemCount()
	 */
	public boolean isEmpty();

	/**
	 * Returns a random item among those inside the container. This method doesn't remove the item.
	 *
	 * @return an item or null if the container is empty.
	 */
	public Item getRandom();

	/**
	 * Tente d'ajouter l'objet donn� � cet {@link ItemContainer} et retourne
	 * l'index auquel a �t� ajout� l'objet si l'op�ration a r�ussi autrement -1.
	 *
	 * @param item
	 *            un {@link Item} � ajouter.
	 * @return un entier repr�sentant l'index auquel a �t� ajout� l'objet si
	 *         l'op�ration a r�ussi autrement -1.
	 */
	public int add(Item item);

	/**
	 * Supprime tous les objets de cet {@link ItemContainer} et les retourne
	 * sous forme de {@link List}.
	 *
	 * @return une {@link List} de {@link Item}. Ne retourne jamais null.
	 */
	public List<Item> removeAll();

	/**
	 * Supprime l'objet d'index donn� et le retourne.
	 *
	 * @param index
	 *            un entier positif ou nul repr�sentant l'index de l'objet �
	 *            retirer.
	 * @return une instance de {@link Item} ou null s'il n'y a aucun objet �
	 *         l'index donn�.
	 */
	public Item remove(int index);

	/**
	 * Supprime un {@link Item} tir� au hasard parmi ceux de cet
	 * {@link ItemContainer} et le retourne.
	 *
	 * @return un {@link Item} ou null si le conteneur est vide.
	 */
	public Item removeRandom();

	/**
	 * Tente de supprimer l'objet donn� et retourne si l'op�ration a r�ussi.
	 *
	 * @param item
	 *            une instance de {@link Item} � supprimer du conteneur.
	 * @return si l'op�ration a r�ussi.
	 */
	public boolean remove(Item item);

	public Item set(int index, Item item);

	/**
	 * Retourne le poids total des objets contenus par cet {@link Item}.
	 *
	 * @return un float repr�sentant le poids des objets en kilogrammes.
	 */
	public float getTotalWeight();
}