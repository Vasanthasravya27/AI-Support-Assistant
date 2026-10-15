FROM python:3.12-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY rag-service/requirements.txt ./requirements.txt
# The RAG embedder runs on CPU in this project. Install the CPU PyTorch wheel
# first so pip does not pull the much larger CUDA/NVIDIA dependency stack.
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch==2.12.1
RUN pip install --no-cache-dir --index-url https://pypi.org/simple -r requirements.txt
COPY rag-service/app ./app
COPY knowledge-base /knowledge-base
ENV KNOWLEDGE_DIR=/knowledge-base CHROMA_PATH=/data/chroma
VOLUME ["/data"]
EXPOSE 8000
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
