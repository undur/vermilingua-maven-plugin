package ng.packaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Generates the Info.plist file
 */

public class PlistSerialization {

	private final StringBuilder b = new StringBuilder();

	public PlistSerialization( final Object plist ) {
		Objects.requireNonNull( plist );

		append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
		append( "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" );
		append( "<plist version=\"1.0\">\n" );

		appendEntry( 1, plist );

		append( "</plist>" );
	}

	private void append( final Object value ) {
		b.append( value );
	}

	private void indent( int indent ) {
		append( "\t".repeat( indent ) );
	}

	private void append( final int indent, final Object value ) {
		indent( indent );
		append( value );
	}

	private void appendEntry( final int indent, final Object object ) {
		if( object != null ) {
			final String elementName = elementNameFromObject( object );
			append( indent, "<" + elementName + ">" );

			if( object instanceof String ) {
				append( object );
				append( "</" + elementName + ">\n" );
			}

			if( object instanceof Map ) {
				final Map<String, Object> map = (Map<String, Object>)object;

				append( "\n" );

				for( final Entry<String, Object> entry : map.entrySet() ) {
					append( indent + 1, "<key>" + entry.getKey() + "</key>\n" );
					appendEntry( indent + 1, entry.getValue() );
				}

				append( indent, "</" + elementName + ">\n" );
			}

			if( object instanceof List ) {
				final List<?> list = (List<?>)object;
				append( "\n" );

				for( final Object o : list ) {
					appendEntry( indent + 1, o );
				}

				append( indent, "</" + elementName + ">\n" );
			}
		}
	}

	private static String elementNameFromObject( final Object object ) {
		if( object instanceof List ) {
			return "array";
		}

		if( object instanceof Map ) {
			return "dict";
		}

		if( object instanceof String ) {
			return "string";
		}

		throw new IllegalArgumentException( "I don't know how to serialize " + object.getClass() );
	}

	@Override
	public String toString() {
		return b.toString();
	}

	/**
	 * FIXME: For testing purposes only // Hugi 2021-07-13
	 */
	public static void main( String[] args ) {
		final var m = new HashMap<>();
		m.put( "smu", "bla" );

		final var n = new HashMap<>();
		n.put( "hehe", "hoho" );

		m.put( "more", n );

		final var list = new ArrayList<>();
		list.add( "Hugi" );
		list.add( "Egill" );
		//		list.add( m );

		m.put( "somelist", list );

		final PlistSerialization p = new PlistSerialization( m );
		System.out.println( p.toString() );
	}
}