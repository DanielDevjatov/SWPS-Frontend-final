FROM node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --production

COPY src ./src
COPY zkp ./zkp

EXPOSE 8083
CMD ["npm", "start"]
