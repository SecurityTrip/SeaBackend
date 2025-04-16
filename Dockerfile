FROM ubuntu:latest
LABEL authors="securitytrip"

ENTRYPOINT ["top", "-b"]