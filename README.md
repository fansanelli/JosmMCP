# JosmMCP

JosmMCP is a plugin for [JOSM](https://josm.openstreetmap.de/), the Java-based editor for OpenStreetMap. It integrates JOSM with the Model Context Protocol (MCP), providing an HTTP server that allows external MCP clients to access JOSM features.

## Usage

After installing and loading the plugin in JOSM, it automatically starts an MCP server on port 3000. External MCP clients (such as AI assistants or other applications) can connect to `http://localhost:3000/mcp` and use the available tools to interact with JOSM.

Currently available tools:
- `get_josm_state`: Retrieves the current state of JOSM, including version, downloaded layers, and data information.
- `get_selected_elements`: Retrieves information about the currently selected OSM elements in JOSM, including their display names and tags.

## Building

1. Download the file `josm-latest.jar` and place it in the `lib` folder.
2. Make sure you have [Maven](https://maven.apache.org/) installed.
3. Build the main project with:

   ```sh
   mvn clean package
   ```

The resulting jar with all dependencies will be in the `target` folder.

## Installing the plugin in JOSM

Copy the generated jar from `target/` to your JOSM plugins directory, for example:

```sh
cp target/josmmcp-0.0.1-SNAPSHOT-jar-with-dependencies.jar ~/.local/share/JOSM/plugins/
```

---

**Disclaimer:** JOSM and OpenStreetMap are trademarks of their respective owners. This project is not affiliated with or endorsed by the JOSM or OpenStreetMap projects.

For any issues or contributions, please refer to the repository or open an issue.

