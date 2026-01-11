# JosmMCP

JosmMCP is a plugin for [JOSM](https://josm.openstreetmap.de/), the Java-based editor for OpenStreetMap. It integrates JOSM with the Model Context Protocol (MCP), providing an HTTP server that allows external MCP clients to access JOSM features.

## ⚠️ Warning

**This plugin is currently in an experimental phase and is not suitable for production use. It serves as a proof-of-concept and does not adhere to zero trust principles. Users should be aware that it may lead to unexpected behaviors or errors.**

## Usage

After installing and loading the plugin in JOSM, it automatically starts an MCP server on port 3000. External MCP clients (such as AI assistants or other applications) can connect to `http://localhost:3000/mcp` and use the available tools to interact with JOSM.

Currently available tools:
- `get_josm_state`: Retrieves the current state of JOSM, including version, downloaded layers, and data information.
- `search_elements`: Searches for OSM elements in the downloaded data using JOSM query syntax (e.g., 'highway=residential', 'amenity=restaurant').
- `modify_tags`: Modifies OSM elements: add/remove/set tags.
- `create_node`: Creates a new node
- `read_node`: Returns coords and tags of a node
- `update_node`: Moves a node
- `delete_node`: Deletes a node
- `create_way`: Creates a new way
- `read_way`: Returns nodes and tags of a way
- `delete_way`: Deletes a way

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

