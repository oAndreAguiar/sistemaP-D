# Sistema P&D — Como rodar localmente

## Pré-requisitos
- Java 25+
- Node.js 20+ (testado com Node 24)
- npm
- Git (opcional)

---

## 1) Rodar o Back-end (Quarkus)
Abra um terminal na raiz do projeto:

D:\Pastas\Códigos\SistemaP&D\code-with-quarkus

Execute:

.\mvnw.cmd quarkus:dev

Back-end:
http://localhost:8080

---

## 2) Rodar o Front-end (Vue + Vite)
Abra outro terminal e entre na pasta do front:

cd .\frontend

Instale dependências:

npm install

Rode o front:

npm run dev

Front-end:
http://localhost:5173

---

## 3) Testes (Back-end)
Na raiz do projeto (code-with-quarkus), execute:

.\mvnw.cmd test

---

## 4) Endpoints principais
- GET  http://localhost:8080/raw-materials
- POST http://localhost:8080/raw-materials
- GET  http://localhost:8080/products
- POST http://localhost:8080/products
- POST http://localhost:8080/production/plan

---

## 5) Exemplo rápido (PowerShell)

Criar matéria-prima:
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/raw-materials" -ContentType "application/json" -Body '{
  "code": "RM-001",
  "name": "Steel",
  "stockQuantity": 5000
}'

Rodar plano:
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/production/plan" -ContentType "application/json" -Body '{}'

Obs.: para o /production/plan retornar itens, cadastre matérias-primas e produtos antes.
