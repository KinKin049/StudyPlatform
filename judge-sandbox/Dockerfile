FROM node:24-bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package.json ./
COPY src ./src

ENV PORT=9000
ENV JUDGE_WORK_DIR=/tmp/studyplatform-judge
ENV CPP_COMPILER=g++

EXPOSE 9000

CMD ["npm", "start"]
