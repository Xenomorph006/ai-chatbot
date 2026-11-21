import os
import secrets
from typing import Literal

from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException
from groq import Groq
from pydantic import BaseModel

load_dotenv()

client = Groq(api_key=os.environ["GROQ_API_KEY"])
MODEL = os.getenv("GROQ_MODEL", "openai/gpt-oss-20b")
SERVICE_SECRET = os.environ["AI_SERVICE_SECRET"]
SYSTEM_PROMPT = "You are a helpful, concise assistant."

app = FastAPI(title="AI Service")


class Message(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class GenerateRequest(BaseModel):
    messages: list[Message]


class GenerateResponse(BaseModel):
    reply: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/generate", response_model=GenerateResponse)
def generate(req: GenerateRequest, x_service_key: str = Header(default="")):
    if not secrets.compare_digest(x_service_key, SERVICE_SECRET):
        raise HTTPException(status_code=401, detail="Invalid service key")
    if not req.messages:
        raise HTTPException(status_code=400, detail="messages cannot be empty")

    chat = [{"role": "system", "content": SYSTEM_PROMPT}]
    chat += [m.model_dump() for m in req.messages]

    try:
        completion = client.chat.completions.create(model=MODEL, messages=chat)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Model API error: {e}")

    return GenerateResponse(reply=completion.choices[0].message.content)