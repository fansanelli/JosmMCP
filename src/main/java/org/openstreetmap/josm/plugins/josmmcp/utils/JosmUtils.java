package org.openstreetmap.josm.plugins.josmmcp.utils;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public class JosmUtils {
	public static String printElement(OsmPrimitive el) {
		StringBuilder sb = new StringBuilder();

		sb.append("id: " + el.getUniqueId());
		sb.append("\ntype: " + el.getType());
		switch (el.getType()) {
		case WAY:
			sb.append("\nnode_ids" + ((Way) el).getNodes());
			break;
		case RELATION:
			sb.append("\nmembers" + ((Relation) el).getMembers());
			break;
		case NODE:
			sb.append("\n" + ((Node) el).getCoor().toString());
			break;
		}

		sb.append(el.getKeys().toString());
		sb.append("\nversion: " + el.getVersion());
		sb.append("\nuser: " + el.getUser());
		return sb.toString();
	}
}
