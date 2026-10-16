FROM node:24-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY proxy-api.conf /etc/nginx/proxy-api.conf
COPY --from=build /workspace/dist/canchas-marketplace-front/browser /usr/share/nginx/html
EXPOSE 80
