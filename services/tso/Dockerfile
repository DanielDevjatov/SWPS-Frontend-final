# Summary (FinalFinal): Added code to *.
# Purpose: document changes and explain behavior.
# Section: Base Node.js runtime for the service
FROM node:20-alpine

# Section: Container working directory
WORKDIR /app

# Section: Install production dependencies
COPY package.json ./
RUN npm install --production

# Section: Copy service source files
COPY src ./src
COPY zkp ./zkp

# Section: Expose service port and start command
EXPOSE 8083
CMD ["npm", "start"]


