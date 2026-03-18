import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/auth": "http://localhost:8080",
      "/holders": "http://localhost:8080",
      "/accounts": "http://localhost:8080",
      "/transactions": "http://localhost:8080",
      "/health": "http://localhost:8080",
    },
  },
  build: {
    outDir: "src/main/resources/static",
    emptyOutDir: true,
  },
});
