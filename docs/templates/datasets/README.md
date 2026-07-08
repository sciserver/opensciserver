## datasets-index.json — instructions

Purpose
-------

This file is used to list dataset entries for a DataVolume or Group of. It contains a single top-level object with a `datasets` array; each element describes one dataset (its title, summary, tags, resources, etc.).

Location
--------

`<<DataVolumeName or UserVolumeName>>/.sciserver/datasets-index.json`

Schema (fields)
----------------

- `name` (string)  <span style="color:red">REQUIRED</span> — Dataset display name.
- `summary` (string) <span style="color:red">REQUIRED</span>  — Short one-line description shown in the dataset card view.
- `description` (string) <span style="color:red">REQUIRED</span>  — Longer HTML-friendly description shown on the dataset detail page. HTML tags are allowed for formatting and images.
- `catalog` (string) — Short catalog or collection name (optional but recommended).
- `logo` (string|null) — URL or null. Path to a logo image if available.
- `tags` (array of strings) — Keyword tags used by search and filters.
- `resources` (array of objects) — Links and resources related to the dataset. Each resource object has:
	- `name` (string) <span style="color:red">REQUIRED</span> — label shown to users (e.g., "Notebook name").
	- `kind` (string) <span style="color:red">REQUIRED</span> — type of resource. <span style="color:red">IMPORTANT NOTE:</span> on the dataset detail view, the resources will be grouped by this field and tabs for each unique value of it will be created. (common values: `notebook`, `external`, `volumes`, `documentation`, etc.).
	- `description` (string) — brief explanation of the resource.
	- `link` (string|null) — URL or null. If null, the site may render a placeholder or omit the link.

Example entry
-------------

Here is an example dataset object (the real `datasets-index.json` contains a template/example entry):

```json
{
	"datasets": [
		{
			"name": "Dataset name",
			"catalog": "Catalog name",
			"summary": "Short description that will appear in the dataset card view.",
			"description": "Detailed description that will appear in the dataset detail view. You can use HTML tags here to format the text and include images, links, etc.",
			"logo": null,
			"tags": ["galaxies", "survey", "photometry"],
			"resources": [
				{"name": "Notebook", "kind": "notebook", "description": "example notebook using example functions", "link": null},
				{"name": "External link", "kind": "external", "description": "More info", "link": "https://example.org"}
			]
		}
	]
}
```

How to add or update a dataset
------------------------------

1. Create a folder called `/.sciserver` at the root of your DataVolume or UserVolume if it doesn't already exist. The dot at the beginning of the name is important. 
2. Copy `datasets-index.json` to this folder and add as many dataset objects as needed to the top-level `datasets` array following the schema above.
3. Keep `name` unique and human-readable. Use `tags` to improve searchability (short words or short phrases).
4. If you add images or logos, use the absolute paths of where they are stored.
5. For `resources.link` use full URLs for any links, as this tool doesn't resolve relative paths within SciServer. If you do not have a link yet, use `null` and update later.
6. Once the file is placed in the correct location and has the correct format, your datasets will appear in https://apps.sciserver.org/web/datasets. 

Validation & tooling
--------------------

- Quick JSON syntax check with `jq` (if installed):

```bash
jq . docs/templates/datasets/datasets-index.json >/dev/null
```

- Alternatively, you can run a Python sanity check (prints schema summary):

```python
import json
f='datasets-index.json'
data=json.load(open(f))
print('datasets:', len(data.get('datasets',[])))
```

Tips and conventions
--------------------

- Keep `summary` short (one sentence). Put longer explanations in `description`.
- Use 3–6 tags per dataset to make filter results useful.
- Prefer `kind` values like `notebook`, `external`, `volumes`.

Troubleshooting
---------------

- If a dataset does not appear on the site:
	- Verify the JSON is valid (see Validation above).
  - Ensure the file is stored at `<<DataVolumeName or UserVolumeName>>/.sciserver/datasets-index.json` (see the [Location](#location) section above).
	- Ensure the dataset object is present in the `datasets` array and `name` is not empty.

