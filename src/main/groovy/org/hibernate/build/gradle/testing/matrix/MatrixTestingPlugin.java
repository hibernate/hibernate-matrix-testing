package org.hibernate.build.gradle.testing.matrix;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;

import org.hibernate.build.gradle.testing.database.DatabaseProfile;
import org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin;
import org.hibernate.build.gradle.testing.database.alloc.DatabaseAllocator;

import groovy.lang.Closure;
import groovy.lang.Reference;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import static org.gradle.api.plugins.JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME;

/**
 * TODO : 1) add a base configuration of common attribute across all matrix node tasks (convention)
 * TODO : 2) somehow allow applying just a single database to a project (non matrix testing).
 *
 * @author Steve Ebersole
 * @author Strong Liu
 * @author Brett Meyer
 */
public class MatrixTestingPlugin implements Plugin<Project> {
    private static final Logger log = Logging.getLogger( MatrixTestingPlugin.class );

    public static final String MATRIX = "matrix";

    public static final String MATRIX_RUNTIME_CONFIG_NAME = "matrixRuntime";
    public static final String MATRIX_TASK_NAME = MATRIX;
    public static final String PROJECT_TEST_TASK_NAME = "test";

    private Project project;
    private SourceSet testSourceSet;
    private Configuration matrixRuntimeConfig;
    private Task matrixTask;

    public void apply(Project project) {
        this.project = project;

        project.getRootProject().getPlugins().apply( DatabaseProfilePlugin.class );
        List<MatrixNode> matrixNodes = locateMatrixNodes();
        if ( matrixNodes.isEmpty() ) {
            // no db profiles defined
            return;

        }

        matrixRuntimeConfig = prepareRuntimeConfiguration();
        testSourceSet = project.getConvention()
                .getPlugin( JavaPluginConvention.class )
                .getSourceSets()
                .getByName( SourceSet.TEST_SOURCE_SET_NAME );

        matrixTask = prepareGroupingTask();
        for ( MatrixNode matrixNode : matrixNodes ) {
            Task matrixNodeTask = prepareNodeTask( matrixNode );
            matrixTask.dependsOn( matrixNodeTask );
        }

    }

    private List<MatrixNode> locateMatrixNodes() {
        List<MatrixNode> matrixNodes = new ArrayList<>();
        Iterable<DatabaseProfile> profiles = project.getRootProject()
                .getPlugins()
                .getAt( DatabaseProfilePlugin.class )
                .getDatabaseProfiles();
        if ( profiles != null ) {
			for ( DatabaseProfile profile : profiles ) {
				matrixNodes.add( new MatrixNode( project, profile ) );
            }

		}

        return matrixNodes;
    }

    /**
     * Prepare runtime configuration for matrix source set.
     */
    private Configuration prepareRuntimeConfiguration() {
		return project.getConfigurations()
                .create( MATRIX_RUNTIME_CONFIG_NAME )
                .setDescription( "Dependencies (baseline) used to run the matrix tests" )
                .extendsFrom( project.getConfigurations().getByName( TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME ) );
    }

    private Task prepareGroupingTask() {
		Task matrixTask = project.getTasks().create( MATRIX_TASK_NAME );
        matrixTask.setGroup( MATRIX );
        matrixTask.setDescription( "Runs the unit tests on Database Matrix" );
		return matrixTask;
    }

    private Task prepareNodeTask(final MatrixNode node) {
        final Test testTask = (Test) project.getTasks().getByName( PROJECT_TEST_TASK_NAME );

        String nodeTaskName = MATRIX_TASK_NAME + "_" + node.getName();
        log.debug( "Adding Matrix Testing task " + nodeTaskName );

        final Test nodeTask = project.getTasks().create( nodeTaskName, Test.class );
        nodeTask.setGroup( MATRIX );
        nodeTask.setDescription( "Runs the tests against " + node.getName() );

        nodeTask.setClasspath( node.getDatabaseProfile()
                .getTestingRuntimeConfiguration()
                .plus( testSourceSet.getRuntimeClasspath() ) );
        nodeTask.setTestClassesDirs( testSourceSet.getOutput().getClassesDirs() );
        nodeTask.setIgnoreFailures( true );
        nodeTask.setWorkingDir( node.getBaseOutputDirectory() );
        nodeTask.getReports().getHtml().setDestination( new File( node.getBaseOutputDirectory(), "reports" ) );
        nodeTask.getReports().getJunitXml().setDestination( new File( node.getBaseOutputDirectory(), "results" ) );

        nodeTask.dependsOn( testSourceSet.getClassesTaskName() );

        nodeTask.getSystemProperties().putAll( node.getDatabaseProfile().getHibernateProperties() );
        // allow the project's test task systemProperties (typically includes System.properties)
		// to overwrite the databaseProfile's
		DefaultGroovyMethods.invokeMethod(
		        nodeTask.getSystemProperties(),
                "putAll",
                new Object[] { (testTask).getSystemProperties() } );
//        nodeTask.jvmArgs = ['-Xms1024M', '-Xmx1024M', '-XX:MaxPermSize=512M', '-Xss4096k', '-Xverify:none', '-XX:+UseFastAccessorMethods', '-XX:+DisableExplicitGC']
        nodeTask.setJvmArgs( new ArrayList<>( Arrays.asList( "-Xms1024M", "-Xmx1024M" ) ) );//, '-XX:MaxPermSize=512M', '-Xss4096k', '-Xverify:none', '-XX:+UseFastAccessorMethods', '-XX:+DisableExplicitGC']
        nodeTask.setMaxHeapSize( "1024M" );

        nodeTask.doFirst( new Closure<Object>( this, this ) {
            public void doCall(Object it) {
                DatabaseAllocator.locate( project )
                        .getAllocation( node.getDatabaseProfile() )
                        .prepareForExecution( nodeTask );
            }

            public void doCall() {
                doCall( null );
            }

        } );

        // After each test *class* (not method), call afterTestClass.  For most/all DatabaseAllocations, this should
        // erase the entire database.
        final Reference<String> testClassName = new Reference<>( "" );
        nodeTask.beforeTest( new Closure<Object>( this, this ) {
            public void doCall(Object it) {
                // Unfortunately, have to do it this way.  Our only options are afterTest (after each method) and
                // afterTestSuite.
                final TestDescriptor testDescriptor = (TestDescriptor) it;
                if ( ! testClassName.get().equals( testDescriptor.getClassName() ) ) {
                    testClassName.set( testDescriptor.getClassName() );
                    DatabaseAllocator.locate( project ).getAllocation( node.getDatabaseProfile() ).beforeTestClass();
                }

            }

        } );

        return nodeTask;
    }
}
