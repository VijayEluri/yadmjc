package fr.ritaly.dungeonmaster;

/**
 * Enum�ration des "mat�rialit�s" support�es dans le jeu.
 * 
 * @author <a href="mailto:francois.ritaly@free.fr">Francois RITALY</a>
 */
public enum Materiality {
	/**
	 * D�finit une entit� mat�rielle (la plupart des objets dans le jeu).
	 */
	MATERIAL,

	/**
	 * D�finit une entit� immat�rielle (les cr�atures de type GHOST, ZYTAZ, etc
	 * qui peuvent passer � travers les murs, portes)
	 */
	IMMATERIAL;
}
