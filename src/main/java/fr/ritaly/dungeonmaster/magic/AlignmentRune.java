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

import org.apache.commons.lang.Validate;

/**
 * Enumerates the 6 alignment runes.
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public enum AlignmentRune implements Rune {
	KU(2, 3, 4, 5, 6, 7),
	ROS(2, 3, 4, 5, 6, 7),
	DAIN(3, 4, 6, 7, 9, 10),
	NETA(4, 6, 8, 10, 12, 14),
	RA(6, 9, 12, 15, 18, 21),
	SAR(7, 10, 14, 17, 21, 24);

	private final int[] costs;

	private AlignmentRune(int... costs) {
		if (costs.length != 6) {
			throw new IllegalArgumentException("Invalid array length (6 expected)");
		}

		this.costs = costs;
	}

	@Override
	public int getCost() {
		throw new UnsupportedOperationException("Method only supported for a power rune");
	}

	@Override
	public int getCost(final PowerRune powerRune) {
		Validate.notNull(powerRune, "The given power rune is null");

		return costs[powerRune.ordinal()];
	}

	@Override
	public Type getType() {
		return Type.ALIGNMENT;
	}

	@Override
	public int getId() {
		return ordinal() + 1;
	}
}
