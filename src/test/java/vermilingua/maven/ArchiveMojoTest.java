package vermilingua.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests on {@link ArchiveMojo}. Archive creation itself is covered by the integration tests, since it attaches artifacts to the build.
 */
public class ArchiveMojoTest {

	/**
	 * Invoked without package first, there's nothing to archive. That fails, rather than archiving an empty folder
	 */
	@Test
	public void failsWithoutABundle( @TempDir final Path target ) {
		final ArchiveMojo mojo = mojo( target, "woapplication" );
		mojo.createArchives = true;

		final MojoFailureException e = assertThrows( MojoFailureException.class, mojo::execute );
		assertTrue( e.getMessage().contains( "mvn package vermilingua:archive" ), e.getMessage() );
		assertFalse( Files.exists( target.resolve( "TestApp.woa" ) ) );
	}

	/**
	 * Without performSplit or createArchives there's nothing to do, so a missing bundle doesn't matter
	 */
	@Test
	public void doesNothingWhenNeitherIsEnabled( @TempDir final Path target ) throws Exception {
		mojo( target, "woapplication" ).execute();

		try( var files = Files.list( target ) ) {
			assertEquals( 0, files.count() );
		}
	}

	/**
	 * Frameworks don't get the goal in their lifecycle, but it can still be invoked on a reactor containing them
	 */
	@Test
	public void skipsFrameworks( @TempDir final Path target ) throws Exception {
		final ArchiveMojo mojo = mojo( target, "woframework" );
		mojo.createArchives = true;
		mojo.execute();
	}

	/**
	 * The split folder is made from the bundle as it is when the goal runs, and starts empty,
	 * so it doesn't keep resources that have since been removed from the bundle
	 */
	@Test
	public void splitMirrorsTheBundle( @TempDir final Path target ) throws Exception {
		final Path woa = target.resolve( "TestApp.woa" );
		file( woa.resolve( "Contents/WebServerResources/app.css" ), "minified" );
		file( woa.resolve( "Contents/Frameworks/ERExtensions.framework/WebServerResources/ajax.js" ), "ajax" );
		file( woa.resolve( "Contents/Resources/Main.wo/Main.html" ), "not a webserver resource" );

		final Path split = target.resolve( "TestApp.woa.webserverresources" );
		file( split.resolve( "Contents/WebServerResources/removed.css" ), "stale" );

		final ArchiveMojo mojo = mojo( target, "woapplication" );
		mojo.performSplit = true;
		mojo.execute();

		assertEquals( "minified", Files.readString( split.resolve( "Contents/WebServerResources/app.css" ) ) );
		assertEquals( "ajax", Files.readString( split.resolve( "Contents/Frameworks/ERExtensions.framework/WebServerResources/ajax.js" ) ) );
		assertFalse( Files.exists( split.resolve( "Contents/WebServerResources/removed.css" ) ) );
		assertFalse( Files.exists( split.resolve( "Contents/Resources" ) ) );
	}

	private static ArchiveMojo mojo( final Path target, final String packaging ) {
		final Build build = new Build();
		build.setDirectory( target.toString() );
		build.setFinalName( "TestApp" );

		final MavenProject project = new MavenProject();
		project.setPackaging( packaging );
		project.setArtifactId( "TestApp" );
		project.setBuild( build );

		final ArchiveMojo mojo = new ArchiveMojo();
		mojo.mavenProject = project;
		return mojo;
	}

	private static void file( final Path path, final String content ) throws IOException {
		Files.createDirectories( path.getParent() );
		Files.writeString( path, content );
	}
}
