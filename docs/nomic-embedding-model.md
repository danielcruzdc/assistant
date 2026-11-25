# Generic RAG Assistant

The application uses the model through an OpenAI-compatible endpoint provided by LM Studio.

## Configuration

The embedding model and vector dimensions are configured in [application.properties](../src/main/resources/application.properties):

```properties
spring.ai.openai.base-url=http://localhost:1235/
spring.ai.openai.api-key=dummy

spring.ai.openai.embedding.options.model=text-embedding-nomic-embed-text-v1.5
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.initialize-schema=true
```

| Property | Value | Purpose |
| --- | --- | --- |
| `spring.ai.openai.base-url` | `http://localhost:1235/` | Base URL of the local OpenAI-compatible LM Studio server |
| `spring.ai.openai.api-key` | `dummy` | Placeholder API key required by the Spring AI OpenAI client |
| `spring.ai.openai.embedding.options.model` | `text-embedding-nomic-embed-text-v1.5` | Name LM Studio exposes for the embedding model |
| `spring.ai.vectorstore.pgvector.dimensions` | `768` | Output vector size used by the PgVector table |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `true` | Allows Spring AI to manage vector-store schema initialization |

## Prerequisites

- PostgreSQL with the `pgvector` extension.
- LM Studio running an embedding server on port `1235`.
- The Nomic Embed Text v1.5 model loaded in LM Studio.
- An embedding API exposed with an OpenAI-compatible `/v1/embeddings` endpoint.

## PostgreSQL Schema

The SQL initialization file in [schema.sql](../src/main/resources/init/schema.sql) creates the vector table and HNSW index:

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id TEXT PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store
    USING HNSW (embedding vector_cosine_ops);
```

The `768` in `VECTOR(768)` must match the embedding model's output dimension and `spring.ai.vectorstore.pgvector.dimensions`.

## How It Works

1. A document is uploaded and split into chunks by `ChatService`.
2. Spring AI sends each chunk to the configured LM Studio embeddings endpoint.
3. LM Studio returns a 768-dimensional vector for each chunk.
4. PgVector stores the vectors in the `vector_store` table.
5. A user query is embedded using the same model and compared with stored vectors using cosine similarity.
6. The application returns the nearest matching documents as context for the chat model.

The similarity search used by the chat service requests five documents and uses a minimum similarity threshold of `0.6`.

## Frontend

The Angular frontend is implemented in [chat-api.service.ts](../frontend/src/app/chat-body/chat-api.service.ts) and [chat-body.ts](../frontend/src/app/chat-body/chat-body.ts).

### Document Upload

The chat UI provides an **Attach document** control that accepts application and text files up to 10 MB. The selected file is sent as multipart form data to the backend:

```text
POST /api/document
Content-Type: multipart/form-data
```

The frontend includes the selected file under the `file` form field. The backend extracts the document text, splits it into chunks, creates embeddings, and stores the resulting vectors in PgVector.

The frontend displays upload progress while the request is running and reports validation or server errors if the upload fails.

### Chat Queries

When the user sends a message, Angular sends it to:

```text
GET /api/{message}
```

The message is URL-encoded by the frontend. The backend embeds the query with the Nomic model, performs a semantic search in PgVector, and uses the matching document context to generate an assistant response.

The response is displayed in the chat conversation together with the user's message.

### Development Proxy

During local Angular development, [proxy.conf.json](../frontend/proxy.conf.json) forwards `/api/**` requests to Spring Boot:

```json
{
  "/api/**": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

Start the backend on port `8080`, then start the frontend from the `frontend` directory:

```powershell
cd frontend
npm install
npm start
```

The Angular development server normally provides the chat UI at `http://localhost:4200`. API calls remain relative to `/api`, allowing the proxy to route them to the Spring Boot application.

### Frontend Limits

- Maximum upload size: 10 MB.
- Accepted file input types: application and text files.
- Enter sends a message.
- Shift+Enter inserts a new line.
- Message sending and document uploading are disabled while a request is in progress.
- Backend and connection errors are shown in the chat UI.

## Starting the Local Services

Start PostgreSQL so it accepts connections at:

```text
jdbc:postgresql://localhost:5432/assistant
```

Then start LM Studio's local server on port `1235` and load `text-embedding-nomic-embed-text-v1.5`.

## Changing the Embedding Model

When changing to a model with a different output size:

1. Update `spring.ai.openai.embedding.options.model`.
2. Update `spring.ai.vectorstore.pgvector.dimensions`.
3. Update `VECTOR(768)` in [schema.sql](../src/main/resources/init/schema.sql).
4. Recreate or migrate the existing `vector_store` table.
5. Re-index uploaded documents.

Do not mix vectors generated by different embedding models in the same table. Rebuild the table and re-upload documents when switching models.

## Troubleshooting

### HTTP connection error to LM Studio

Confirm LM Studio is running, its server port is `1235`, and the configured model is loaded.

### Dimension mismatch

If PostgreSQL reports a vector dimension error, verify that the model output dimension matches both:

- `spring.ai.vectorstore.pgvector.dimensions`
- `embedding VECTOR(<dimensions>)` in `schema.sql`

### No semantic search results

Check that documents were uploaded successfully, the embeddings endpoint is reachable, and the similarity threshold is not filtering every result.

