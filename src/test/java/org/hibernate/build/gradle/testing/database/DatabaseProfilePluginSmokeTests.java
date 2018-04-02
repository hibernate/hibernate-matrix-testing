/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.build.gradle.testing.database;

import java.io.File;
import java.net.URL;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import org.hibernate.build.gradle.testing.BuildException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class DatabaseProfilePluginSmokeTests {
	@Test
	public void testBasicApplication() {
		final Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply( DatabaseProfilePlugin.class );

		final DatabaseProfilesExtension extension = project.getExtensions().getByType( DatabaseProfilesExtension.class );

		try {
			extension.getProfilesByName();
			Assertions.fail( "Expecting failure" );
		}
		catch (BuildException expected) {
		}

		extension.searchDirectory( findDatabasesDirectory() );
		extension.resolve();

		Assertions.assertTrue( extension.getProfilesByName().containsKey( "h2" ) );
		Assertions.assertTrue(
				extension.getProfilesByName()
						.get( "h2" )
						.getHibernateProperties().containsKey( "hibernate.dialect" )
		);
	}

	private Object findDatabasesDirectory() {
		final URL url = getClass().getResource( "/databases/h2/matrix.gradle" );
		return new File( url.getFile() ).getParentFile().getParentFile();
	}
}
