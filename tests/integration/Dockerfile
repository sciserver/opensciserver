FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1
WORKDIR /tests

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY nose2.config testutil.py test_*.py ./

CMD ["sh", "-c", "printf '%s\\n' \"${SCISERVER_BASE_URL:?Set SCISERVER_BASE_URL to the deployment base URL}\" > sciserverbase && exec nose2 --config nose2.config test_preflight test_register test_api test_logout"]
