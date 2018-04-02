/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.build.gradle.testing.database;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;

import org.hibernate.build.gradle.testing.BuildException;

import static org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.CUSTOM_DATABASES_DIRECTORY_KEY;
import static org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.HIBERNATE_MATRIX_IGNORE;
import static org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.JDBC_DIR;
import static org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.MATRIX_BUILD_FILE;
import static org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.STANDARD_DATABASES_DIRECTORY;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class DatabaseProfilesExtension {
	private final Project project;

	private boolean enabled;
	private Set<File> searchDirectories;
	private Set<String> includes;
	private Set<String> excludes;

	private Map<String,DatabaseProfile> profilesByName;

	public DatabaseProfilesExtension(Project project) {
		this.project = project;
		this.searchDirectories = defaultSearchDirectories( project );
		this.excludes = configuredExcludes( project );
	}

	public Map<String, DatabaseProfile> getProfilesByName() {
		if ( profilesByName == null ) {
			throw new BuildException( "Access to profiles before resolved" );
		}
		return profilesByName;
	}

	private static Set<File> defaultSearchDirectories(Project project) {
		final HashSet<File> directories = new HashSet<>();

		directories.add( project.file( STANDARD_DATABASES_DIRECTORY ) );

		final File localDirectory = resolveLocalDirectory( project );
		if ( localDirectory != null ) {
			directories.add( localDirectory );
		}

		return directories;
	}

	private static File resolveLocalDirectory(Project project) {
		Object localDirectoryProperty = System.getProperty( CUSTOM_DATABASES_DIRECTORY_KEY );

		if ( project.hasProperty( CUSTOM_DATABASES_DIRECTORY_KEY ) ) {
			localDirectoryProperty = project.property( CUSTOM_DATABASES_DIRECTORY_KEY );
		}

		if ( localDirectoryProperty != null ) {
			return project.file( localDirectoryProperty );
		}

		return null;
	}

	private Set<String> configuredExcludes(Project project) {
		final String values = project.hasProperty( HIBERNATE_MATRIX_IGNORE )
				? (String) project.property( HIBERNATE_MATRIX_IGNORE )
				: System.getProperty( HIBERNATE_MATRIX_IGNORE );

		if ( values == null || values.length() == 0 ) {
			return Collections.emptySet();
		}
		else {
			final HashSet<String> excludes = new HashSet<>();
			Collections.addAll( excludes, values.split( "," ) );
			return excludes;
		}
	}

	@Input
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@InputFiles
	public Set<File> getSearchDirectories() {
		return searchDirectories;
	}

	public void setSearchDirectories(Set<File> searchDirectories) {
		this.searchDirectories = searchDirectories;
	}

	public void searchDirectory(File directory) {
		if ( searchDirectories == null ) {
			searchDirectories = new HashSet<>();
		}
		searchDirectories.add( directory );
	}

	public void searchDirectory(Object directory) {
		if ( searchDirectories == null ) {
			searchDirectories = new HashSet<>();
		}
		searchDirectories.add( project.file( directory ) );
	}

	@Input
	public Set<String> getIncludes() {
		return includes;
	}

	public void setIncludes(Set<String> includes) {
		this.includes = includes;
	}

	public void include(String profileName) {
		if ( includes == null ) {
			includes = new HashSet<>();
		}
		includes.add( profileName );
	}

	@Input
	public Set<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(Set<String> excludes) {
		this.excludes = excludes;
	}

	public void exclude(String profileName) {
		if ( excludes == null ) {
			excludes = new HashSet<>();
		}
		excludes.add( profileName );
	}

	void resolve() {
		if ( profilesByName != null ) {
			return;
		}

		profilesByName = new LinkedHashMap<>();
		processParentProfiles( profilesByName );
		processSearchDirectories( profilesByName );
	}

	private void processParentProfiles(Map<String, DatabaseProfile> profileMap) {
		final Project parent = project.getParent();
		if ( parent == null || parent == project ) {
			return;
		}

		final DatabaseProfilesExtension parentExtension = parent.getExtensions().findByType( DatabaseProfilesExtension.class );
		if ( parentExtension == null ) {
			return;
		}

		parentExtension.resolve();

		for ( Map.Entry<String, DatabaseProfile> entry : parentExtension.profilesByName.entrySet() ) {
			final boolean include = includes == null || includes.contains( entry.getKey() );
			if ( !include ) {
				continue;
			}

			final boolean exclude = excludes != null && excludes.contains( entry.getKey() );
			if ( exclude ) {
				continue;
			}

			profileMap.put( entry.getKey(), entry.getValue() );
		}
	}

	private void processSearchDirectories(Map<String, DatabaseProfile> profilesByName) {
		searchDirectories.forEach( dir -> processProfiles( dir, profilesByName ) );
	}

	private void processProfiles(File directory, Map<String, DatabaseProfile> profileMap) {
		if ( ! directory.exists() ) {
			return;
		}

		if ( ! directory.isDirectory() ) {
			return;
		}

		// the directory itself is a "database directory" if it contains either:
		//		1) a file named 'matrix.gradle'
		//		2) a directory named 'jdbc'
		final DatabaseProfile databaseProfile = findDatabaseProfile( directory );

		// if the directory itself is not a profile, check its sub-directories
		if ( databaseProfile == null ) {
			//noinspection ConstantConditions
			for ( File subDirectory : directory.listFiles() ) {
				processProfiles( subDirectory, profileMap );
			}
		}
		else {
			final String profileName = databaseProfile.getName();

			final boolean include = includes == null || includes.contains( profileName );
			if ( !include ) {
				return;
			}

			final boolean exclude = excludes != null && excludes.contains( profileName );
			if ( exclude ) {
				return;
			}

			final DatabaseProfile existing = profileMap.get( profileName );
			if ( existing == null ) {
				profileMap.put( profileName, databaseProfile );
			}
			else {
				project.getLogger().lifecycle(
						"Found duplicate profile definitions [name={}], [{}] taking precedence over [{}]",
						profileName,
						databaseProfile.getDirectory().getAbsolutePath(),
						existing.getDirectory().getAbsolutePath()
				);
				profileMap.put( profileName, MergedDatabaseProfile.merge( databaseProfile, existing ) );
			}
		}
	}

	private DatabaseProfile findDatabaseProfile(File directory) {
		final File matrixDotGradleFile = new File( directory, MATRIX_BUILD_FILE );
		if ( matrixDotGradleFile.exists() && matrixDotGradleFile.isFile() ) {
			project.getLogger().debug( "Found matrix.gradle file : " + matrixDotGradleFile );
			return new MatrixDotGradleProfile( matrixDotGradleFile, project );
		}

		final File jdbcDirectory = new File( directory, JDBC_DIR );
		if ( jdbcDirectory.exists() && jdbcDirectory.isDirectory() ) {
			return new JdbcDirectoryProfile( jdbcDirectory, project );
		}

		return null;
	}

	private static class MergedDatabaseProfile implements DatabaseProfile {
		private final String name;
		private final File profileDirectory;
		private final Configuration testingRuntimeConfiguration;
		private final Map<String,Object> hibernateProperties;

		MergedDatabaseProfile(
				String name,
				File profileDirectory,
				Configuration testingRuntimeConfiguration,
				Map<String, Object> hibernateProperties) {
			this.name = name;
			this.profileDirectory = profileDirectory;
			this.testingRuntimeConfiguration = testingRuntimeConfiguration;
			this.hibernateProperties = hibernateProperties;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public File getDirectory() {
			return profileDirectory;
		}

		@Override
		public Map<String, Object> getHibernateProperties() {
			return hibernateProperties;
		}

		@Override
		public Configuration getTestingRuntimeConfiguration() {
			return testingRuntimeConfiguration;
		}

		private static MergedDatabaseProfile merge(DatabaseProfile precedence, DatabaseProfile fallback) {
			return new MergedDatabaseProfile(
					precedence.getName(),
					precedence.getDirectory(),
					precedence.getTestingRuntimeConfiguration() != null
							? precedence.getTestingRuntimeConfiguration()
							: fallback.getTestingRuntimeConfiguration(),
					mergeProperties( precedence, fallback )
			);
		}

		@SuppressWarnings("unchecked")
		private static Map<String, Object> mergeProperties(DatabaseProfile precedence, DatabaseProfile fallback) {
			final HashMap merged = new HashMap( fallback.getHibernateProperties() );
			merged.putAll( precedence.getHibernateProperties() );
			return merged;
		}
	}
}
