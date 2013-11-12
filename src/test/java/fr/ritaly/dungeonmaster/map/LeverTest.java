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
import fr.ritaly.dungeonmaster.actuator.SimpleActuator;
import fr.ritaly.dungeonmaster.actuator.TestActuator;
import fr.ritaly.dungeonmaster.actuator.TriggerAction;

public class LeverTest extends TestCase {

	public LeverTest() {
	}

	public LeverTest(String name) {
		super(name);
	}

	public void testLeverTriggeringPit() {
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+
		// | W | . | I | . | W |
		// +---+---+---+---+---+
		// | W | . | . | . | L |
		// +---+---+---+---+---+
		// | W | . | . | . | W |
		// +---+---+---+---+---+
		// | W | W | W | W | W |
		// +---+---+---+---+---+

		final Dungeon dungeon = new Dungeon();

		final Pit pit = new Pit(false, false);

		final Level level1 = dungeon.createLevel(1, 5, 5);
		level1.setElement(2, 1, pit);

		final Lever lever = new Lever(Direction.EAST, true);
		level1.setElement(4, 2, lever);

		lever.setActuator(new SimpleActuator(2, TriggerAction.TOGGLE, pit));

		// --- Initial state
		assertTrue(lever.isLeverUp());
		assertTrue(pit.isClosed());

		// --- Pull the lever down - the pit must open
		lever.toggle();

		// Let the pit open
		Clock.getInstance().tick(2);

		assertFalse(lever.isLeverUp());
		assertTrue(pit.isOpen());

		// --- Pull up the lever - the pit must close
		lever.toggle();

		// Let the pit open
		Clock.getInstance().tick(2);

		assertTrue(lever.isLeverUp());
		assertFalse(pit.isOpen());
	}

	public void testActuatorTriggeredWhenUsingLever() {
		final TestActuator actuator = new TestActuator();

		final Lever lever = new Lever(Direction.EAST, true);
		lever.setActuator(actuator);

		final Dungeon dungeon = new Dungeon();

		final Level level1 = dungeon.createLevel(1, 5, 5);
		level1.setElement(4, 2, lever);

		// --- Initial state
		assertTrue(lever.isLeverUp());
		assertFalse(actuator.isTriggered());

		// --- Pull the lever down
		lever.toggle();

		Clock.getInstance().tick();

		assertFalse(lever.isLeverUp());
		assertTrue(actuator.isTriggered());

		actuator.reset();

		assertFalse(lever.isLeverUp());
		assertFalse(actuator.isTriggered());

		// --- Pull up the lever
		lever.toggle();

		Clock.getInstance().tick();

		assertTrue(lever.isLeverUp());
		assertTrue(actuator.isTriggered());
	}

	@Override
	protected void setUp() throws Exception {
		Clock.getInstance().reset();
	}
}