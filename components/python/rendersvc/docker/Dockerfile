FROM python:3

WORKDIR /opt
COPY sciserver_utils*.whl dist/*.whl docker/entrypoint.sh ./
RUN pip install uvicorn -f file:///opt/ sciserver_rendersvc
CMD /opt/entrypoint.sh
