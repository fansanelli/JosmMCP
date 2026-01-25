/*
 * JosmMCPPlugin - JOSM plugin to integrate JOSM with the Model Context Protocol
 * Copyright (C) 2025-2026 Pengunaria.dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
