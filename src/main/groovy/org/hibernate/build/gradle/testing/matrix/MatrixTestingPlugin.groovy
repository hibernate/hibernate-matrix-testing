/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.build.gradle.testing.matrix;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.hibernate.build.gradle.testing.database.DatabaseProfile
import org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin
import org.hibernate.build.gradle.testing.database.alloc.DatabaseAllocator

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME

/**
 * TODO : 1) add a base configuration of common attribute across all matrix node tasks (convention)
 * TODO : 2) somehow allow applying just a single database to a project (non matrix testing).
 *
 * @author Steve Ebersole
 * @author Strong Liu
 * @author Brett Meyer
 */
public class MatrixTestingPlugin implements Plugin<Project> {
    private static final Logger log = Logging.getLogger(MatrixTestingPlugin.class);

    public static final String MATRIX_COMPILE_CONFIG_NAME = "matrixCompile";
    public static final String MATRIX_RUNTIME_CONFIG_NAME = "matrixRuntime";
    public static final String MATRIX_TASK_NAME = "matrix";
    public static final String PROJECT_TEST_TASK_NAME = "test";

    private Project project;
    private SourceSet testSourceSet;

    private Configuration matrixCompileConfig;
    private Configuration matrixRuntimeConfig;
    private Task matrixTask;

    public void apply(Project project) {
        this.project = project;

        project.rootProject.plugins.apply( DatabaseProfilePlugin );
        List<MatrixNode> matrixNodes = locateMatrixNodes();
        if ( matrixNodes == null || matrixNodes.isEmpty() ) {
            // no db profiles defined
            return;
        }

        matrixCompileConfig = prepareCompileConfiguration();
        matrixRuntimeConfig = prepareRuntimeConfiguration();
        testSourceSet = project.convention.getPlugin( JavaPluginConvention ).sourceSets
                .getByName( SourceSet.TEST_SOURCE_SET_NAME );

		matrixTask = prepareGroupingTask();
        for ( MatrixNode matrixNode: matrixNodes ) {
            Task matrixNodeTask = prepareNodeTask( matrixNode );
            matrixTask.dependsOn( matrixNodeTask );
        }
    }

	private List<MatrixNode> locateMatrixNodes() {
        List<MatrixNode> matrixNodes = new ArrayList<MatrixNode>();
		Iterable<DatabaseProfile> profiles = project.rootProject.plugins[DatabaseProfilePlugin].databaseProfiles;
		if ( profiles != null ) {
			for ( DatabaseProfile profile : profiles ) {
				matrixNodes.add( new MatrixNode( project, profile ) );
			}
		}
        return matrixNodes;
    }

    /**
     * Prepare compile configuration for matrix source set.
     */
    private Configuration prepareCompileConfiguration() {
        return project.configurations.create( MATRIX_COMPILE_CONFIG_NAME )
                .setDescription( "Dependencies used to compile the matrix tests" )
                .extendsFrom( project.configurations.getByName( COMPILE_CONFIGURATION_NAME ) )
                .extendsFrom( project.configurations.getByName( TEST_COMPILE_CONFIGURATION_NAME ) );
    }

    /**
     * Prepare runtime configuration for matrix source set.
     */
    private Configuration prepareRuntimeConfiguration() {
		return project.configurations.create( MATRIX_RUNTIME_CONFIG_NAME )
				.setDescription( "Dependencies (baseline) used to run the matrix tests" )
				.extendsFrom( matrixCompileConfig )
				.extendsFrom( project.configurations.getByName( RUNTIME_CONFIGURATION_NAME ) )
				.extendsFrom( project.configurations.getByName( TEST_RUNTIME_CONFIGURATION_NAME ) );
    }

	private Task prepareGroupingTask() {
		Task matrixTask = project.tasks.create( MATRIX_TASK_NAME );
        matrixTask.group = "Verification"
        matrixTask.description = "Runs the unit tests on Database Matrix"
		return matrixTask;
	}

    private void generateNodeTasks(List<MatrixNode> matrixNodes) {
        // For now we just hard code this to locate the databases processed by
        // org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin.  But long term would be much better to
        // abstract this idea via the MatrixNode/MatrixNodeProvider interfaces; this would allow the jvm variance
        // needed for jdbc3/jdbc4 testing for example.  Not to mention its much more generally applicable
        //
        // Also the notion that the plugin as a MatrixNodeProducer might not be appropriate.  probably a split there
        // is in order too (config producer and jvm producer and somehow they get wired into a matrix).
        //
        // but again this is just a start.
    }

    private Task prepareNodeTask(final MatrixNode node) {
        String taskName = MATRIX_TASK_NAME + '_' + node.name
        log.debug( "Adding Matrix Testing task $taskName" );
        final Test nodeTask = project.tasks.create( taskName, Test );
        nodeTask.description = "Runs the matrix against ${node.name}"
        nodeTask.classpath = node.databaseProfile.testingRuntimeConfiguration + testSourceSet.runtimeClasspath
        nodeTask.testClassesDirs = testSourceSet.output.classesDirs
        nodeTask.ignoreFailures = true
        nodeTask.workingDir = node.baseOutputDirectory
        nodeTask.reports.html.destination = new File(node.baseOutputDirectory, "reports")
        nodeTask.reports.junitXml.destination = new File(node.baseOutputDirectory, "results")

        nodeTask.dependsOn( project.tasks.getByName( testSourceSet.classesTaskName ) );
		
		nodeTask.systemProperties.putAll(node.databaseProfile.hibernateProperties);
		// allow the project's test task systemProperties (typically includes System.properties)
		// to overwrite the databaseProfile's
		nodeTask.systemProperties.putAll(project.tasks.getByName( PROJECT_TEST_TASK_NAME ).systemProperties);
//        nodeTask.jvmArgs = ['-Xms1024M', '-Xmx1024M', '-XX:MaxPermSize=512M', '-Xss4096k', '-Xverify:none', '-XX:+UseFastAccessorMethods', '-XX:+DisableExplicitGC']
        nodeTask.jvmArgs = ['-Xms1024M', '-Xmx1024M']//, '-XX:MaxPermSize=512M', '-Xss4096k', '-Xverify:none', '-XX:+UseFastAccessorMethods', '-XX:+DisableExplicitGC']
        nodeTask.maxHeapSize = "1024M"

        nodeTask.doFirst {
            DatabaseAllocator.locate( project ).getAllocation( node.databaseProfile ).prepareForExecution( nodeTask )
        }
        
        // After each test *class* (not method), call afterTestClass.  For most/all DatabaseAllocations, this should
        // erase the entire database.
        String testClassName = "";
        nodeTask.beforeTest { testDescriptor ->
        	// Unfortunately, have to do it this way.  Our only options are afterTest (after each method) and
        	// afterTestSuite.
        	if ( !testDescriptor.className.equals( testClassName ) ) {
				testClassName = testDescriptor.className;
        		DatabaseAllocator.locate( project ).getAllocation( node.databaseProfile ).beforeTestClass();
        	}
        }

        return nodeTask;
    }
}
