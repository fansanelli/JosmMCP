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
package org.openstreetmap.josm.plugins.josmmcp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.josmmcp.tools.ModifyTool;
import org.openstreetmap.josm.plugins.josmmcp.tools.SearchTool;
import org.openstreetmap.josm.plugins.josmmcp.tools.SelectedTool;
import org.openstreetmap.josm.plugins.josmmcp.tools.StateTool;
import org.openstreetmap.josm.tools.Logging;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;

public class JosmMCPPlugin extends Plugin {
	private Server jettyServer;

	public JosmMCPPlugin(PluginInformation info) {
		super(info);

		Logging.info("JosmMCPPlugin initialization");
		try {
			this.jettyServer = new Server(3000);
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/");
			jettyServer.setHandler(context);
			
			List<McpStatelessServerFeatures.AsyncToolSpecification> toolSpecs = new ArrayList<McpStatelessServerFeatures.AsyncToolSpecification>();
			toolSpecs.add(new SearchTool().getSpec());
			toolSpecs.add(new ModifyTool().getSpec());
			toolSpecs.add(new SelectedTool().getSpec());
			toolSpecs.add(new StateTool().getSpec());

			HttpServletStatelessServerTransport servlet = HttpServletStatelessServerTransport.builder().build();
			McpStatelessAsyncServer server = McpServer.async(servlet).serverInfo("JOSM MCP Server", "1.0.0")
					.tools(toolSpecs).build();
			context.addServlet(new ServletHolder(servlet), "/mcp");
			jettyServer.start();
			Logging.info("MCP HTTP server started on port 3000");
		} catch (Exception e) {
			Logging.error("Failed to start MCP server: " + e.getMessage(), e);
		}
	}
}
