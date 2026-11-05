FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1
WORKDIR /tests

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY nose2.config testutil.py test_*.py ./
COPY runner.py reporting.py ./
COPY contract ./contract
RUN mkdir /results

ENTRYPOINT ["python", "/tests/runner.py"]
