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
package fr.ritaly.dungeonmaster.actuator;

/**
 * An object which handles an {@link Actuator} per trigger type.
 *
 * @author <a href="mailto:francois.ritaly@gmail.com">Francois RITALY</a>
 */
public interface HasActuators {

	/**
	 * Returnt the {@link Actuator} mapped to the given trigger type.
	 *
	 * @param triggerType
	 *            a {@link TriggerType}. Can't be null.
	 * @return an {@link Actuator} or null if none is mapped to the given
	 *         trigger type.
	 */
	public Actuator getActuator(TriggerType triggerType);

	/**
	 * Sets the {@link Actuator} mapped to the given trigger type.
	 *
	 * @param triggerType
	 *            an {@link TriggerType}.
	 * @param actuator
	 *            an {@link Actuator}.
	 */
	public void setActuator(TriggerType triggerType, Actuator actuator);

	public void addActuator(TriggerType triggerType, Actuator actuator);

	public void clearActuator(TriggerType triggerType);
}