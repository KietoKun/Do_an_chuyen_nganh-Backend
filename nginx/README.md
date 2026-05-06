# Nginx HTTPS

Docker Compose starts Nginx on ports `80` and `443` and proxies traffic to the Spring Boot backend at `backend:8080`.

Set `SERVER_NAME` in `.env` to your domain or server IP:

```env
SERVER_NAME=api.example.com
```

Certificate files are loaded from:

```text
nginx/certs/fullchain.pem
nginx/certs/privkey.pem
```

If these files do not exist, the Nginx container creates a self-signed certificate so HTTPS can start for testing. For production, replace them with certificates from Let's Encrypt or your certificate provider.

