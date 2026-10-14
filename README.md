# AI Support Assistant

A customer-support chat app that combines **Python RAG**, a **Java Spring Boot MCP server**, and an **LLM tool-calling agent**.

```text
Browser :3000 → Agent service → LLM
                         └── MCP Streamable HTTP → Java :8080
                                                   ├── Ticket tools → H2
                                                   └── Knowledge search → Python RAG :8000 → ChromaDB
```

## What it can do

- Answer policy and troubleshooting questions using the support articles in `knowledge-base/`.
- Create a support ticket after collecting the customer's email and issue details.
- Look up a ticket or list tickets for an email address.
- Keep a short conversation history for follow-up questions.
- Show the conversation in a responsive browser chat interface.

The agent discovers tools from the Java server through MCP and invokes them over Streamable HTTP. RAG retrieval and embedding remain in the Python service; Java owns ticket records and publishes all four MCP tools.

## Run everything with Docker Compose

Requirements: Docker Desktop with Compose enabled, and an API key for an OpenAI-compatible model provider.

1. Copy `.env.example` to `.env` and set `LLM_API_KEY`. You can also change `LLM_MODEL` and optionally set `LLM_BASE_URL` for another OpenAI-compatible endpoint.
2. From the project root, run:

   ```sh
   docker compose up --build
   ```

3. Open [http://localhost:3000](http://localhost:3000). The first RAG startup downloads the sentence-transformer model; allow a few minutes on first run.
4. To stop the services, press Ctrl+C and run `docker compose down`. Persistent Chroma and H2 data remain in Docker volumes. To remove the stored data too, run `docker compose down -v`.

The chat app and development endpoints bind to localhost. The MCP server is unauthenticated in local mode; keep it on a trusted local network.

## Free demo deployment on Render

The included `render.yaml` describes three free web services. Push this project to a GitHub repository (make sure `.env` is not uploaded), sign in to Render, choose **New → Blueprint**, and connect that repository. Render will ask for `LLM_API_KEY` and `DEMO_ACCESS_CODE` during setup. Enter your Gemini key and choose a code that you can give to your teacher; do not put either value in the repository. The Gemini model and compatible API URL are already set in the Blueprint.

When the services finish deploying, open the `ai-support-assistant` service's public `onrender.com` URL. The first RAG startup downloads its embedding model and may take a while. Free Render services sleep after 15 minutes without traffic and wake when visited. The three services share the account's free service-hour allowance, and Render may restart them. Chroma and H2 use temporary storage on this free setup, so indexed articles are rebuilt from the included knowledge files and demo tickets may disappear after a restart. This setup is for a class presentation, not real customer support.

## Run services locally

Use three terminals. Install Java 21+, Maven 3.9+, and Python 3.10+ first.

### 1. Python RAG service

```sh
cd rag-service
python -m venv .venv
# Windows: .venv\Scripts\activate
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8000
```

In another terminal, ingest the included help articles:

```sh
curl -X POST http://localhost:8000/ingest
```

### 2. Java MCP and ticket service

```sh
cd mcp-server
mvn spring-boot:run
```

### 3. Agent and chat interface

Set `LLM_API_KEY` in your environment, then run:

```sh
cd agent-service
python -m venv .venv
# Windows: .venv\Scripts\activate
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 3000
```

Open [http://localhost:3000](http://localhost:3000). If your provider uses a compatible endpoint, set `LLM_BASE_URL`; set `MCP_URL` if the Java MCP server is not at `http://localhost:8080/mcp`.

## API and MCP

- Chat: `POST /api/chat` on port 3000 with `{"message":"How do I reset my password?"}`. Include the returned `conversation_id` in later requests to continue a conversation.
- Agent health: `GET /health` on port 3000.
- RAG health/search/ingest: `GET /health`, `POST /search`, and `POST /ingest` on port 8000.
- Ticket REST endpoints: `POST /api/tickets`, `GET /api/tickets/{ticketNumber}`, and `GET /api/tickets?email=...` on port 8080.
- MCP tools at `http://localhost:8080/mcp`: `search_knowledge_base`, `create_support_ticket`, `get_ticket_status`, and `list_customer_tickets`. Use the MCP Inspector with Streamable HTTP to explore them.

## Configuration

| Variable | Service | Default | Purpose |
| --- | --- | --- | --- |
| `LLM_API_KEY` | Agent | required | Provider credential |
| `LLM_MODEL` | Agent | `gpt-4o-mini` | Chat and tool-calling model |
| `LLM_BASE_URL` | Agent | provider default | Optional OpenAI-compatible endpoint |
| `MCP_URL` | Agent | `http://localhost:8080/mcp` | Java MCP endpoint |
| `RAG_BASEURL` | Java | `http://localhost:8000` | Python RAG endpoint |
| `KNOWLEDGE_DIR` | RAG | `../knowledge-base` | Markdown and text articles to ingest |
| `EMBEDDING_MODEL` | RAG | `sentence-transformers/all-MiniLM-L6-v2` | Local embedding model |
| `CHROMA_PATH` | RAG | `rag-service/data/chroma` | Persistent vector-store directory |

## Lightweight answer evaluation

With all three services running and a configured model key, run `python agent-service/evaluations/run_eval.py` from the project root. It checks that common support questions call RAG and include key details from the included support articles. Add cases to `agent-service/evaluations/cases.json` when the knowledge articles change.

## Safety and current limitations

- The assistant is instructed to ground support-policy answers in retrieved articles and to offer escalation when retrieval is insufficient.
- Ticket creation requires customer details and model confirmation/intent; status changes are not available to the model.
- Conversation history is held in memory and expires after six hours. Restarting the agent clears it; use a persistent store and authentication for a multi-user deployment.
- Ticket lookup currently uses email and ticket number without identity verification. The Render demo access code is a basic gate for a class demonstration, not a full login system; do not use real customer data. Render's Blueprint also protects MCP and RAG service endpoints with generated internal tokens.
- Do not paste passwords, payment-card information, or other secrets into chat.

## Existing checks

The repository includes RAG chunker unit tests and Java service/client tests. From `rag-service`, run `python -m unittest discover -s tests -v`. From `mcp-server`, run `mvn test`.
