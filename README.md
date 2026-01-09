# E-Shop Backend API (Fullstack Showcase)

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Live Production](https://img.shields.io/badge/Status-Live_Production-red)

Vítejte v repozitáři backendové části mého fullstack e-shopu. Jedná se o plně funkční produkční aplikaci. Backend (Railway) obsluhuje Frontend (Vercel) v reálném čase.

Projekt demonstruje kompletní životní cyklus objednávky: od registrace, přes validaci a generování platebních QR kódů, až po logistiku a správu v administraci.

 **Live Aplikace (Frontend):** [https://eshop-frontend-kappa.vercel.app/](https://eshop-frontend-kappa.vercel.app/)
*(Aplikace je plně interaktivní a komunikuje s tímto backendem)*

---

##  Klíčové Backend Funkcionality

### 1. Řízení objednávek a State Management
Backend řídí stavy objednávek a zajišťuje datovou konzistenci.
* **Workflow:** `NEW` -> `PAID` -> `SHIPPED` -> `DELIVERED` / `CANCELLED`.
* **Event-Driven Notifications:** Změna stavu objednávky automaticky spouští odeslání transakčního e-mailu zákazníkovi (potvrzení přijetí, potvrzení platby, odeslání zásilky).

### 2. Admin Panel & Logistika
Aplikace obsahuje robustní administrátorskou sekci (Role `ADMIN`):
* **Správa objednávek:** Kompletní přehled objednávek, detailní náhledy, manuální změna stavů a řešení storen.
* **Integrace PPL (Logistika):** Backend komunikuje s API dopravce.
    * **Generování štítků:** Admin může přímo z detailu objednávky vygenerovat a **stáhnout PDF štítek** pro balík.
    *  *Poznámka:* V této demo verzi je volání na produkční PPL servery vypnuté (chybějící privátní klíče), logika integrace je však v kódu plně implementována.

### 3. Platby
* **Smart Platby:** Backend dynamicky generuje validní SEPA QR kód (SPAYD standard) pro okamžitou platbu převodem ihned po vytvoření objednávky.

### 4. Bezpečnost a Uživatelé
* **Verifikace:** Dvoufázová registrace (ověření e-mailu kódem) a kontrola plnoletosti (Adult check) při nákupu.
* **Security:** Stateless autentizace pomocí JWT (JSON Web Tokens).
* **Role Management:** Striktní oddělení práv v Security Filter Chain:
    * `USER`: Nákup, historie vlastních objednávek, profil.
    * `ADMIN`: Dashboard, editace produktů, správa všech objednávek, logistika.

### 5. Správa obsahu
* **Multimédia:** Upload a správa galerie obrázků k produktům.
* **Katalog:** Kompletní CRUD operace pro produkty a kategorie.

---

##  API Endpointy (Výběr)
Backend vystavuje REST API pro SPA frontend.

### Public (Veřejné)
* `POST /api/auth/register` - Registrace s odesláním verifikačního e-mailu
* `POST /api/auth/login` - Přihlášení (vrací JWT)
* `GET /api/products` - Výpis katalogu
* `POST /api/order` - Vytvoření objednávky (Guest checkout)
* `POST /api/verification/**` - Ověření emailu

### Secured (Uživatel)
* `GET /orders/{id}` - Detail objednávky a její stav
* `PUT /api/users/profile` - Změna údajů (Heslo, Email, Telefon)

### Secured (Admin)
* `PUT /api/admin/orders/{id}/status` - Změna stavu objednávky + trigger e-mailu
* `GET /api/shipping/label/{orderId}` - Stažení přepravního štítku (PDF)
* `POST /api/admin/products` - Vytvoření produktu + upload obrázků

---

##  Tech Stack

* **Core:** Java 17, Spring Boot 3
* **Data:** Spring Data JPA, PostgreSQL
* **Security:** Spring Security, JWT
* **Integrace & Utility:**
    * `JavaMailSender` (SMTP notifikace)
    * `ZXing` (Generování QR kódů)
    * `RestTemplate` (Komunikace s PPL API)
* **Build & Deploy:** Maven, Docker, Railway (Backend + DB)

---

##  Architektura

Backend je navržen jako REST API obsluhující SPA (Single Page Application).

```mermaid
sequenceDiagram
    participant Frontend
    participant Backend
    participant DB
    participant EmailService
    
    Frontend->>Backend: POST /api/orders (Vytvořit objednávku)
    Backend->>Backend: Validace věku & skladu
    Backend->>DB: Uložit objednávku (Status: NEW)
    Backend->>EmailService: Odeslat potvrzení (Async)
    EmailService-->>Frontend: (Email zákazníkovi)
    Backend->>Frontend: 200 OK + QR Kód k platbě
