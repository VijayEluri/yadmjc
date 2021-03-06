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

import junit.framework.TestCase;
import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.Direction;
import fr.ritaly.dungeonmaster.actuator.TestActuator;
import fr.ritaly.dungeonmaster.item.Item;
import fr.ritaly.dungeonmaster.item.ItemFactory;
import fr.ritaly.dungeonmaster.item.Torch;

public class AlcoveTest extends TestCase {

	public AlcoveTest() {
	}

	public AlcoveTest(String name) {
		super(name);
	}

	public void testActuatorTriggeredWhenItemDropped() {
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | . | A | . | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+

		Dungeon dungeon = new Dungeon();

		final TestActuator actuator = new TestActuator();

		final Level level1 = dungeon.createLevel(1, 5, 5);

		final Alcove alcove = new Alcove(Direction.NORTH);
		level1.setElement(2, 2, alcove);

		alcove.addActuator(actuator);

		// --- Initial state
		assertFalse(actuator.isTriggered());

		// --- Drop an item into the alcove
		alcove.dropItem(new Torch(), Direction.NORTH);
		Clock.getInstance().tick();
		assertTrue(actuator.isTriggered());
		actuator.reset();

		// --- Dropping a second item shouldn't trigger the actuator
		assertFalse(actuator.isTriggered());
		alcove.dropItem(new Torch(), Direction.NORTH);
		Clock.getInstance().tick();
		assertFalse(actuator.isTriggered());
	}

	public void testActuatorTriggeredWhenItemOfGivenTypeDropped() {
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | . | A | . | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+

		Dungeon dungeon = new Dungeon();

		final TestActuator actuator = new TestActuator();

		final Level level1 = dungeon.createLevel(1, 5, 5);

		final Alcove alcove = new Alcove(Direction.NORTH, Item.Type.PLATE_OF_RA);
		level1.setElement(2, 2, alcove);

		alcove.addActuator(actuator);

		// --- Initial state
		assertFalse(actuator.isTriggered());

		// --- Drop an item with the incorrect type in the alcove
		alcove.dropItem(new Torch(), Direction.NORTH);
		Clock.getInstance().tick();
		assertFalse(actuator.isTriggered());
		actuator.reset();

		// --- Get back the item
		assertNotNull(alcove.pickItem(Direction.NORTH));
		Clock.getInstance().tick();
		assertFalse(actuator.isTriggered());
		actuator.reset();

		// --- Drop an item with the correct type
		alcove.dropItem(ItemFactory.getFactory().newItem(Item.Type.PLATE_OF_RA), Direction.NORTH);
		Clock.getInstance().tick();
		assertTrue(actuator.isTriggered());
		actuator.reset();

		// --- Get back the item
		assertNotNull(alcove.pickItem(Direction.NORTH));
		Clock.getInstance().tick();
		assertTrue(actuator.isTriggered());
		actuator.reset();
	}

	public void testActuatorTriggeredWhenItemPickedUp() {
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | . | A | . | W |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+

		Dungeon dungeon = new Dungeon();

		final TestActuator actuator = new TestActuator();

		final Level level1 = dungeon.createLevel(1, 5, 5);

		final Alcove alcove = new Alcove(Direction.NORTH);
		level1.setElement(2, 2, alcove);

		alcove.addActuator(actuator);

		// --- Initial state
		final Torch torch1 = new Torch();
		final Torch torch2 = new Torch();

		assertFalse(actuator.isTriggered());
		alcove.dropItem(torch1, Direction.NORTH);
		alcove.dropItem(torch2, Direction.NORTH);
		assertEquals(2, level1.getElement(2, 2).getItemCount());
		Clock.getInstance().tick();
		actuator.reset();
		assertFalse(actuator.isTriggered());

		// --- Picking an item which isn't the last one shouldn't trigger the
		// actuator
		assertEquals(torch2, alcove.pickItem(Direction.NORTH));
		assertEquals(1, level1.getElement(2, 2).getItemCount());
		Clock.getInstance().tick();
		assertFalse(actuator.isTriggered());

		// --- Picking the last item should trigger the actuator
		assertEquals(torch1, alcove.pickItem(Direction.NORTH));
		assertEquals(0, level1.getElement(2, 2).getItemCount());
		Clock.getInstance().tick();
		assertTrue(actuator.isTriggered());
	}

	@Override
	protected void setUp() throws Exception {
		Clock.getInstance().reset();
	}
}