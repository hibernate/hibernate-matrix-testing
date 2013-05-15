package org.hibernate.build.gradle.testing.database.alloc;

import org.gradle.api.Project;

import org.hibernate.build.gradle.testing.database.DatabaseProfile;

/**
 * @author Steve Ebersole
 */
public interface DatabaseAllocationProvider {
	DatabaseAllocation buildAllocation(Project rootProject, DatabaseProfile profile, DatabaseAllocationCleanUp cleanUp);
}
