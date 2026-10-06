FROM node:24-bookworm-slim AS build
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates && rm -rf /var/lib/apt/lists/*
WORKDIR /app
# Source and package-lock are pinned; no tracking of upstream main at build time.
RUN git init . && git remote add origin https://github.com/CopilotKit/OpenDots.git && git fetch --depth 1 origin c2569bb6a13a22e565cf3eb791c62267d06babb1 && git checkout --detach FETCH_HEAD && test "$(git rev-parse HEAD)" = c2569bb6a13a22e565cf3eb791c62267d06babb1
COPY patch-opendots.mjs /tmp/patch-opendots.mjs
COPY responses.test.ts /tmp/responses.test.ts
RUN node /tmp/patch-opendots.mjs /app && npm ci --no-audit --no-fund && npm run build

FROM node:24-bookworm-slim
ENV NODE_ENV=production HOST=0.0.0.0 PORT=4310 DATABASE_PATH=/data/opendots.sqlite OPENAI_MODEL=gpt-6-astra MAESTRO_TASK_TIMEOUT_MS=600000 WEB_SEARCH_PROVIDER=parallel
WORKDIR /app
COPY --from=build /app/package.json /app/package-lock.json ./
RUN npm ci --omit=dev --no-audit --no-fund && mkdir -p /data && chown node:node /data
COPY --from=build /app/dist ./dist
COPY --from=build /app/LICENSE /app/UPSTREAM-LICENSE
COPY entrypoint.mjs ./entrypoint.mjs
USER node
EXPOSE 4310
CMD ["node", "entrypoint.mjs"]
