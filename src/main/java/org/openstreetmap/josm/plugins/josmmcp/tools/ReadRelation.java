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
package org.openstreetmap.josm.plugins.josmmcp.tools;

import java.util.Arrays;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.josmmcp.utils.JosmUtils;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class ReadRelation extends BaseTool {

	@Override
	public String getName() {
		return "read_relation";
	}

	@Override
	public String getDescription() {
		return "Read a relation Id from current Dataset and returns its members and tags";
	}

	@Override
	public JsonSchema getInputSchema() {
		Map<String, Object> readProps = new java.util.HashMap<>();
		Map<String, Object> idProp = new java.util.HashMap<>();
		idProp.put("type", "number");
		readProps.put("id", idProp);
		McpSchema.JsonSchema readSchema = new McpSchema.JsonSchema("object", readProps, Arrays.asList("id"), null, null,
				null);
		return readSchema;
	}

	@Override
	public String handle(Map<String, Object> args) throws Exception {
		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds == null) {
			throw new Exception("no active dataset found");
		}

		long id = Long.parseLong(args.get("id").toString());
		Relation r = (Relation) ds.getPrimitiveById(new SimplePrimitiveId(id, OsmPrimitiveType.RELATION));

		return JosmUtils.printElement(r);
	}
}
