import subprocess

from fastapi import FastAPI, Query
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.get("/")
def data(url: str = Query(...)):

  cmd = ["curl", url]
  kwargs = {
    "stdout": subprocess.PIPE,
    "stderr": subprocess.PIPE,
    "text": True
  }

  process = subprocess.Popen(cmd, **kwargs)
  return StreamingResponse(process.stdout, media_type="text/plain")

if __name__ == "__main__":
  import uvicorn
  uvicorn.run(app, host="0.0.0.0", port=8005)