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
package fr.ritaly.dungeonmaster.item;

import junit.framework.TestCase;
import fr.ritaly.dungeonmaster.Clock;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.champion.Champion.Name;
import fr.ritaly.dungeonmaster.champion.ChampionFactory;
import fr.ritaly.dungeonmaster.champion.Party;

public class IllumuletTest extends TestCase {

	public IllumuletTest() {
	}

	public IllumuletTest(String name) {
		super(name);
	}

	// The illumlet is a special neck lace which generates light

	public void testIllumulet() {
		Champion tiggy = ChampionFactory.getFactory().newChampion(Name.TIGGY);

		Party party = new Party();
		party.addChampion(tiggy);

		final Item illumulet = ItemFactory.getFactory().newItem(Item.Type.ILLUMULET);

		// --- There's no light initially
		assertEquals(0, tiggy.getLight());

		// --- The illumlet doesn't work when grabbed
		assertNull(tiggy.getBody().getWeaponHand().putOn(illumulet));
		assertEquals(0, tiggy.getLight());

		// --- The illumlet only works when worn as a neck lace
		assertEquals(illumulet, tiggy.getBody().getWeaponHand().takeOff());
		assertNull(tiggy.getBody().getNeck().putOn(illumulet));
		assertTrue(tiggy.getLight() > 0);

		// --- The illumlet stops working when no longer worn
		assertEquals(illumulet, tiggy.getBody().getNeck().takeOff());
		assertEquals(0, tiggy.getLight());
	}

	@Override
	protected void setUp() throws Exception {
		Clock.getInstance().reset();
	}
}