package fr.ritaly.dungeonmaster;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.ritaly.dungeonmaster.ai.Creature;
import fr.ritaly.dungeonmaster.champion.Champion;
import fr.ritaly.dungeonmaster.event.ChangeEvent;
import fr.ritaly.dungeonmaster.event.ChangeEventSource;
import fr.ritaly.dungeonmaster.event.ChangeEventSupport;
import fr.ritaly.dungeonmaster.event.ChangeListener;
import fr.ritaly.dungeonmaster.map.Element;

public class PoisonCloud implements ClockListener, ChangeEventSource {

	private final Log log = LogFactory.getLog(PoisonCloud.class);

	/**
	 * The {@link Element} where the poison cloud is located.
	 */
	private final Element element;

	private final Temporizer temporizer;

	private final ChangeEventSupport eventSupport = new ChangeEventSupport();

	// TODO Impl�menter la notion de force du poison !

	// La dur�e de vie d'un nuage restante constante (10 sec), seule sa force
	// diff�re selon la puissance du sort
	private int lifeTime = 10;

	public PoisonCloud(Element element) {
		Validate.notNull(element, "The given element is null");

		this.element = element;
		this.temporizer = new Temporizer("PoisonCloud" + element.getPosition(),
				Clock.ONE_SECOND);
	}

	@Override
	public void addChangeListener(ChangeListener listener) {
		eventSupport.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(ChangeListener listener) {
		eventSupport.removeChangeListener(listener);
	}

	private void fireChangeEvent() {
		eventSupport.fireChangeEvent(new ChangeEvent(this));
	}

	@Override
	public boolean clockTicked() {
		if (temporizer.trigger()) {
			// Le nuage attaque les champions / cr�atures pr�sentes sur
			// l'�l�ment courant (les deux en m�me temps ne sont pas possibles
			// !)
			if (element.hasParty()) {
				final List<Champion> champions = element.getParty()
						.getChampions(false);

				for (Champion champion : champions) {
					// TODO Passer en param�tre le type de dommage (Poison),
					// prendre en compte la force du nuage de poison
					champion.hit(Utils.random(5, 20));
				}
			} else if (element.hasCreatures()) {
				final Set<Creature> creatures = element.getCreatures();

				for (Creature creature : creatures) {
					// TODO Attaquer la cr�ature
					// TODO Passer en param�tre le type de dommage (Poison)
				}
			}

			// La dur�e de vie du nuage diminue
			final int backup = lifeTime;

			final boolean again = --lifeTime > 0;

			if (log.isDebugEnabled()) {
				log.debug(this + ".LifeTime: " + backup + " -> " + lifeTime
						+ " [-1]");
			}

			if (!again) {
				// Notifie la fin de vie du nuage
				fireChangeEvent();
			}
		}

		return (lifeTime > 0);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + element.getPosition();
	}
}