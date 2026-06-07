FROM node:20 AS dev

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

COPY . .

CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "8083"]
