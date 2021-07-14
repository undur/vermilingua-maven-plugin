package ng.packaging;

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

	private void append( final int indent, final Object value ) {
		append( "\t".repeat( indent ) );
		append( value );
	}

	private void appendEntry( final int indent, final Object object ) {
		if( object != null ) {

			// Booleans are a bit.. special. The value is the element, so there's no container element
			if( object instanceof Boolean ) {
				if( (Boolean)object ) {
					append( indent, "<true />\n" );
				}
				else {
					append( indent, "<false />\n" );
				}
			}
			else {
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
}