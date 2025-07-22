import nbformat
from fastapi import HTTPException, Request, Response
from fastapi.responses import HTMLResponse
from sciserverutils.apps.fastapi import FastAPI, getServiceLog
from nbconvert import HTMLExporter
from traitlets.config import Config


app = FastAPI()


def do_notebook_convert(data, inputs=True, prompts=True):
    notebook = nbformat.reads(data, as_version=4)
    c = Config()
    c.TemplateExporter.exclude_input_prompt = not prompts
    c.TemplateExporter.exclude_output_prompt = not prompts
    c.TemplateExporter.exclude_input = not inputs
    body, _ = HTMLExporter(config=c).from_notebook_node(notebook)
    return HTMLResponse(body)


@app.post('/notebook/convert')
async def convert(request: Request, inputs: bool = True, prompts: bool = True) -> Response:
    data = await request.body()
    slog = getServiceLog(request)
    slog.counterAdd('notebook-convert')
    with slog.timer('converttime'):
        try:
            converted = do_notebook_convert(data, inputs, prompts)
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))
    return converted
