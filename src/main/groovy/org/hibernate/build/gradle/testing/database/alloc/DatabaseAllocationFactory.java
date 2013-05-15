package org.hibernate.build.gradle.testing.database.alloc;

import org.hibernate.build.gradle.testing.database.DatabaseProfile;

/**
 * @author Steve Ebersole
 */
public interface DatabaseAllocationFactory {
	public DatabaseAllocation getDatabaseAllocation(DatabaseProfile profile);
}
