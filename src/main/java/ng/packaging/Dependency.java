package ng.packaging;

import java.io.File;
import java.util.Objects;

/**
 * A project dependency along with some metadata.
 *
 * FIXME: Remember to convert back to a record when we re-upgrade to JDK 17 (or more likely, 21)
 */

public class Dependency {

	private String _groupId;
	private String _artifactId;
	private String _version;
	private File _file;

	public Dependency( String groupId, String artifactId, String version, File file ) {
		_groupId = groupId;
		_artifactId = artifactId;
		_version = version;
		_file = file;
	}

	public String groupId() {
		return _groupId;
	}

	public void setGroupId( String groupId ) {
		this._groupId = groupId;
	}

	public String artifactId() {
		return _artifactId;
	}

	public void setArtifactId( String artifactId ) {
		this._artifactId = artifactId;
	}

	public String version() {
		return _version;
	}

	public void setVersion( String version ) {
		this._version = version;
	}

	public File file() {
		return _file;
	}

	public void setFile( File file ) {
		this._file = file;
	}

	@Override
	public int hashCode() {
		return Objects.hash( _artifactId, _file, _groupId, _version );
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj ) {
			return true;
		}
		if( obj == null ) {
			return false;
		}
		if( getClass() != obj.getClass() ) {
			return false;
		}
		Dependency other = (Dependency)obj;
		return Objects.equals( _artifactId, other._artifactId ) && Objects.equals( _file, other._file ) && Objects.equals( _groupId, other._groupId ) && Objects.equals( _version, other._version );
	}

	@Override
	public String toString() {
		return "Dependency [_groupId=" + _groupId + ", _artifactId=" + _artifactId + ", _version=" + _version + ", _file=" + _file + "]";
	}
}