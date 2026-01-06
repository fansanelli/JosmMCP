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

import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

public class StateTool extends BaseTool {

	@Override
	public String getName() {
		return "get_josm_state";
	}

	@Override
	public String getDescription() {
		return "Ottieni lo stato corrente di JOSM: versione, layer scaricati e informazioni sui dati";
	}

	@Override
	public JsonSchema getInputSchema() {
		McpSchema.JsonSchema emptySchema = new McpSchema.JsonSchema("object", new java.util.HashMap<>(), null, null,
				null, null);
		return emptySchema;
	}

	@Override
	public String handle(McpTransportContext exchange, CallToolRequest params) {
		StringBuilder sb = new StringBuilder();

		// Versione JOSM
		String version = Version.getInstance().getVersionString();
		sb.append("Versione JOSM: ").append(version).append("\n");

		// Layer scaricati
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		sb.append("Layer scaricati: ").append(layers.size()).append("\n");
		for (Layer layer : layers) {
			sb.append("- ").append(layer.getName()).append(" (").append(layer.getClass().getSimpleName()).append(")\n");
		}

		// Informazioni sui dati
		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		if (ds != null) {
			sb.append("Informazioni sui dati:\n");
			sb.append("- Nodi: ").append(ds.getNodes().size()).append("\n");
			sb.append("- Ways: ").append(ds.getWays().size()).append("\n");
			sb.append("- Relazioni: ").append(ds.getRelations().size()).append("\n");
			List<Bounds> boundsList = ds.getDataSourceBounds();
			if (!boundsList.isEmpty()) {
				sb.append("- Bounds: ");
				for (Bounds b : boundsList) {
					sb.append(b.toString()).append("; ");
				}
				sb.append("\n");
			}
		} else {
			sb.append("Nessun dataset di modifica attivo.\n");
		}

		return sb.toString();
	}

}
