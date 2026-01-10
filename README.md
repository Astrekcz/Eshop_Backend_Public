# E-Shop Backend API (Fullstack Showcase)

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Live Production](https://img.shields.io/badge/Status-Live_Production-red)

Vítejte v repozitáři backendové části mého fullstack e-shopu. Jedná se o plně funkční produkční aplikaci. Backend (Railway) obsluhuje Frontend (Vercel) v reálném čase.

Projekt demonstruje kompletní životní cyklus objednávky: od registrace a verifikace e-mailu, přes validaci a generování platebních QR kódů, až po logistiku a správu v administraci.

**Live Aplikace (Frontend):** [https://eshop-frontend-kappa.vercel.app/](https://eshop-frontend-kappa.vercel.app/)
*(Aplikace je plně interaktivní a komunikuje s tímto backendem)*

---

## Demo Účty & Přístup

Aplikaci si můžete vyzkoušet zaregistrováním vlastního uživatelského účtu na frontendu.

**Administrátorský přístup:**
Z bezpečnostních důvodů (ochrana databáze před spamem a scripty) **nejsou** přihlašovací údaje pro roli `ADMIN` veřejně dostupné.
> **Jste IT Recruiter nebo Tech Lead?**
> Pokud si chcete vyzkoušet funkcionalitu admin panelu (správa produktů, objednavek, dashboard), kontaktujte mě prosím. Rád vám obratem poskytnu dočasné admin credentials.

---

## Klíčové Backend Funkcionality

### 1. Řízení objednávek a Notifikace
Backend řídí stavy objednávek a zajišťuje komunikaci se zákazníkem.
* **Workflow:** `NEW` -> `PAID` -> `SHIPPED` -> `DELIVERED` / `CANCELLED`.
* **Transakční E-maily:** Systém automaticky odesílá e-mailové potvrzení při vytvoření objednávky a následně při změnách stavu (např. potvrzení platby, expedice).

### 2. Admin Panel & Správa obsahu
Robustní administrátorské rozhraní (přístupné pouze pro roli `ADMIN`) umožňuje plnou kontrolu nad inventářem a logistikou:

* **Správa produktů a kategorií (CRUD):** * Kompletní rozhraní pro vytváření, úpravu a mazání produktů.
    * Dynamická správa kategorií pro organizaci katalogu.
* **Multimédia & Image Upload:** * Backend podporuje nahrávání obrázků k produktům (Multipart/Form-Data).
    * Logika pro validaci velikosti, formátu a následné mapování souborů na produktovou galerii.
* **Správa objednávek:** * Detailní přehled všech transakcí, manuální změna stavů a správa storen.
* **Integrace PPL (Logistika):** * Backend komunikuje s API dopravce. Admin může přímo z detailu objednávky vygenerovat a **stáhnout PDF štítek** pro balík.
    * *Poznámka:* V demo verzi je volání na produkční API vypnuté, logika je však v kódu plně implementována.

### 3. Platby
* **Smart Platby:** Backend dynamicky generuje validní QR kód (český standard SPAYD) pro okamžitou platbu v CZK ihned po vytvoření objednávky.

### 4. Bezpečnost a Uživatelé
* **Autentizace:** Stateless autentizace a autorizace pomocí JWT (JSON Web Tokens).
* **Validace:** Kontrola plnoletosti (Adult check) při nákupu specifického zboží.
* **E-mailová Verifikace:** Proces ověření vlastnictví e-mailové adresy při registraci pomocí jednorázového kódu (OTP), který backend odesílá přes SMTP server.
* **Správa profilu:** Uživatel má možnost spravovat své osobní údaje a měnit přístupové heslo.
* **Role Management:** Striktní oddělení endpointů v Security Filter Chain:
    * `USER`: Nákup, historie vlastních objednávek, správa profilu.
    * `ADMIN`: Dashboard, editace produktů, správa všech objednávek, logistika.

### 5. Správa obsahu
* **Multimédia:** Upload a správa galerie obrázků k produktům.
* **Katalog:** Kompletní CRUD operace pro produkty a kategorie.

---

## API Endpointy (Výběr)
Backend vystavuje REST API pro SPA frontend.

### Public (Veřejné)
* `POST /api/auth/register` - Registrace uživatele (spouští odeslání verifikačního e-mailu)
* `POST /api/auth/verify` - Ověření registračního kódu
* `POST /api/auth/login` - Přihlášení (vrací JWT)
* `GET /api/products` - Výpis katalogu
* `POST /api/order` - Vytvoření objednávky

### Secured (Uživatel)
* `GET /orders/{id}` - Detail objednávky a její stav
* `PUT /api/users/profile` - Změna profilových údajů a hesla

### Secured (Admin)
* `PUT /api/admin/orders/{id}/status` - Změna stavu objednávky + trigger e-mailu
* `GET /api/shipping/label/{orderId}` - Stažení přepravního štítku (PDF)
* `POST /api/admin/products` - Vytvoření produktu + upload obrázků

---

## Tech Stack

* **Core:** Java 17, Spring Boot 3
* **Data:** Spring Data JPA, PostgreSQL
* **Security:** Spring Security, JWT
* **Integrace & Utility:**
    * `JavaMailSender` (SMTP notifikace)
    * `ZXing` (Generování QR kódů)
    * `WebClient` (Reaktivní komunikace s PPL API)
* **Build & Deploy:** Maven, Docker, Railway (Backend + DB)

---

## Architektura

Backend je navržen jako REST API obsluhující SPA (Single Page Application).

```mermaid
sequenceDiagram
    participant Customer as Zákazník
    participant Frontend
    participant Backend
    participant DB
    participant SMTP as SMTP
    
    Customer->>Frontend: Kliknutí "Objednat"
    Frontend->>Frontend: Adulto Widget (Ověření věku)
    Frontend->>Backend: POST /api/orders
    Backend->>DB: Uložit objednávku (Status: NEW)
    Backend->>SMTP: Odeslat potvrzení
    SMTP-->>Customer: Doručení emailu
    Backend->>Frontend: 200 OK + QR Kód k platbě
