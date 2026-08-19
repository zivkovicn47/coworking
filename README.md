# Coworking Space Reservation System

REST aplikacija za rezervaciju radnih mesta i sala za sastanke, razvijena u Spring Boot okruženju.
Sistem obezbeđuje autentifikaciju, autorizaciju po ulogama, upravljanje radnim jedinicama i
rezervacijama prema zadatim poslovnim pravilima.

Predmet: **IT355** · Student: **Nikola Živković 6090**

---

## Zadatak

Platforma omogućava članovima (freelancerima/kompanijama) i administratorima coworking prostora da
se registruju, prijave i koriste sistem za online rezervaciju radnih jedinica. Članovi pregledaju
dostupne radne prostore na osnovu tipa i opremljenosti projektorom i šalju zahteve za rezervaciju
određenog termina. Administratori upravljaju inventarom jedinica (dodavanje, izmena, brisanje) i
odobravaju ili odbijaju pristigle rezervacije. Sistem sprovodi striktna poslovna pravila kako bi se
sprečilo preklapanje termina i ograničilo maksimalno vreme pojedinačnog zakupa.

---

## Tehnologije

| Komponenta | Verzija / izbor |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.0 (`spring-boot-starter-webmvc`, `-data-jpa`, `-security`, `spring-boot-h2console`) |
| Baza | H2 in-memory (`jdbc:h2:mem:coworkingdb`), konzola na `/h2-console` |
| Autentifikacija | Spring Security — HTTP Basic + sesija, lozinke heširane BCrypt-om |
| Ostalo | Lombok, Hibernate (`ddl-auto` podrazumevano `create-drop` za embedded bazu) |

## Pokretanje

```bash
./mvnw spring-boot:run      # aplikacija se diže na http://localhost:8080
./mvnw test                 # provera da se Spring kontekst podiže
```

H2 konzola: <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:mem:coworkingdb`, korisnik `sa`, bez lozinke.

---

## Arhitektura

```
metropolitan.ac.rs.coworking
├── config       SecurityConfig, CustomUserDetailsService
├── controller   AuthController, WorkspaceController, BookingController
├── service      WorkspaceService, BookingService   (poslovna pravila)
├── repository   UserRepository, WorkspaceRepository, BookingRepository
├── model        User, Role, Workspace, WorkspaceType, Booking, BookingStatus
└── exception    GlobalExceptionHandler, ResourceNotFoundException
```

**Enumeracije**

- `Role`: `ROLE_MEMBER`, `ROLE_ADMIN`
- `WorkspaceType`: `HOT_DESK`, `DEDICATED_DESK`, `MEETING_ROOM`, `PRIVATE_OFFICE`
- `BookingStatus`: `PENDING`, `APPROVED`, `REJECTED`

---

## Funkcionalni zahtevi

### 1. Registracija i prijava korisnika (5 poena)

- **1 poen** — REST endpoint-i za registraciju i prijavu: `POST /api/auth/register`, `POST /api/auth/login`.
- **3 poena** — Spring Security: lozinke se heširaju `BCryptPasswordEncoder`-om, rute su zaštićene
  kroz `SecurityFilterChain` uz HTTP Basic i sesiju (`SecurityContext` se čuva u `HttpSession`).
- **1 poen** — Dve uloge, `ROLE_MEMBER` i `ROLE_ADMIN`, dodeljuju se pri registraciji i mapiraju u
  `GrantedAuthority` kroz `CustomUserDetailsService`.

### 2. Platforma za članove (6 poena)

- **1.5 poena** — `GET /api/workspaces` vraća sve radne jedinice (naziv, tip, kapacitet, cena po satu,
  projektor) uz filtriranje: `?type=MEETING_ROOM`, `?hasProjector=true` ili obe kombinovano.
- **1.5 poena** — `GET /api/workspaces/{id}` vraća detalje jedinice, a `GET /api/workspaces/{id}/bookings`
  njenu zauzetost (sve rezervacije sa terminima i statusima).
- **1 poen** — `POST /api/bookings` prima ID jedinice, vreme početka i vreme završetka.
- **2 poena** — Poslovna pravila (`BookingService.createBooking`):
  - nova rezervacija uvek dobija status `PENDING`;
  - trajanje ne sme preći **8 sati**;
  - `totalPrice = trajanje_u_satima × hourlyRate` radne jedinice;
  - zahtev se odbija ako se termin preklapa sa već **`APPROVED`** rezervacijom iste jedinice;
  - dodatno se odbijaju termini u prošlosti i termini kod kojih je kraj pre ili jednak početku.

### 3. Platforma za administratore prostora (6 poena)

- **2 poena** — `POST /api/workspaces`, `PUT /api/workspaces/{id}`, `DELETE /api/workspaces/{id}`
  (naziv, tip, kapacitet, cena po satu, projektor).
- **2 poena** — `GET /api/bookings` vraća sve pristigle rezervacije, uz filter `?status=PENDING|APPROVED|REJECTED`.
- **2 poena** — `PUT /api/bookings/{id}/status?status=APPROVED|REJECTED` menja status. Pri odobrenju
  termin se blokira, a svi preostali **preklapajući `PENDING`** zahtevi za tu jedinicu se automatski
  prebacuju u `REJECTED` (kaskadno odbijanje, u jednoj transakciji).

---

## Pregled endpoint-a i prava pristupa

| Metoda | Putanja | Pristup | Opis |
|---|---|---|---|
| POST | `/api/auth/register` | svi | Registracija (`username`, `password`, `role`) |
| POST | `/api/auth/login` | svi | Prijava, otvara sesiju |
| GET | `/api/workspaces` | svi | Lista jedinica + filteri `type`, `hasProjector` |
| GET | `/api/workspaces/{id}` | svi | Detalji jedinice |
| GET | `/api/workspaces/{id}/bookings` | svi | Zauzetost jedinice |
| POST | `/api/workspaces` | `ROLE_ADMIN` | Dodavanje jedinice |
| PUT | `/api/workspaces/{id}` | `ROLE_ADMIN` | Izmena jedinice |
| DELETE | `/api/workspaces/{id}` | `ROLE_ADMIN` | Brisanje jedinice |
| POST | `/api/bookings` | `ROLE_MEMBER` | Slanje zahteva za rezervaciju |
| GET | `/api/bookings` | `ROLE_ADMIN` | Sve rezervacije + filter `status` |
| PUT | `/api/bookings/{id}/status` | `ROLE_ADMIN` | Odobravanje / odbijanje |

---

## Rukovanje greškama

`GlobalExceptionHandler` (`@RestControllerAdvice`) mapira izuzetke na HTTP statuse:

| Situacija | Status | Poruka |
|---|---|---|
| Trajanje duže od 8h | `400` | `Maksimalno trajanje jedne rezervacije je 8 sati.` |
| Termin u prošlosti | `400` | `Ne mozete rezervisati termin u proslosti.` |
| Kraj pre/jednak početku | `400` | `Vreme zavrsetka mora biti nakon vremena pocetka.` |
| Preklapanje sa `APPROVED` | `400` | `Izabrana radna jedinica je vec zauzeta u ovom terminu.` |
| Zauzeto korisničko ime | `400` | `Korisnicko ime je vec zauzeto.` |
| Nepostojeća radna jedinica | `404` | `Radna jedinica sa ID-em {id} ne postoji.` |
| Nepostojeća rezervacija | `404` | `Rezervacija nije pronadjena` |
| Neprijavljen korisnik | `401` | — |
| Pogrešna uloga | `403` | — |

Poslovne greške bacaju `IllegalArgumentException` → `400 Bad Request`; nepostojeći resursi
`ResourceNotFoundException` → `404 Not Found`.

---

## Test scenario (Postman / curl)

```jsonc
// 1. Registracija
POST /api/auth/register  { "username": "clan1",  "password": "123", "role": "ROLE_MEMBER" }  // 201
POST /api/auth/register  { "username": "admin1", "password": "123", "role": "ROLE_ADMIN"  }  // 201

// 2. Prijava
POST /api/auth/login     { "username": "admin1", "password": "123" }                          // 200

// 3. Kreiranje radne jedinice (admin)
POST /api/workspaces
{ "name": "Konferencijska Sala Nikola Tesla", "type": "MEETING_ROOM",
  "capacity": 10, "hourlyRate": 25.0, "hasProjector": true }                                  // 201, id = 1

// 4. Rezervacija #1 (član, 10:00–13:00 = 3h)
POST /api/bookings
{ "workspace": { "id": 1 }, "startTime": "2026-09-15T10:00:00",
  "endTime": "2026-09-15T13:00:00" }                       // 201, PENDING, totalPrice 75.0 (3h × 25)

// 5. Rezervacija #2 u preklapajućem terminu (11:00–14:00)
POST /api/bookings
{ "workspace": { "id": 1 }, "startTime": "2026-09-15T11:00:00",
  "endTime": "2026-09-15T14:00:00" }                                                          // 201, PENDING

// 6. Pravilo > 8 sati
POST /api/bookings
{ "workspace": { "id": 1 }, "startTime": "2026-09-16T08:00:00",
  "endTime": "2026-09-16T18:00:00" }        // 400 "Maksimalno trajanje jedne rezervacije je 8 sati."

// 7. Odobrenje i kaskadno odbijanje (admin)
PUT /api/bookings/1/status?status=APPROVED                                                    // 200
GET /api/bookings          // #1 = APPROVED, #2 automatski postala REJECTED

// 8. Novi preklapajući zahtev nakon odobrenja
POST /api/bookings
{ "workspace": { "id": 1 }, "startTime": "2026-09-15T12:00:00",
  "endTime": "2026-09-15T15:00:00" }   // 400 "Izabrana radna jedinica je vec zauzeta u ovom terminu."
```

Isti scenario preko `curl`-a (HTTP Basic umesto sesije):

```bash
curl -u clan1:123 -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"workspace":{"id":1},"startTime":"2026-09-15T10:00:00","endTime":"2026-09-15T13:00:00"}'
```

---

## Napomene

- Baza je in-memory: podaci se brišu pri svakom restartu aplikacije.
- Uloga se zadaje u telu zahteva pri registraciji, što je namerno pojednostavljenje radi
  demonstracije obe platforme bez seed podataka.
- CSRF zaštita je isključena jer je reč o REST API-ju koji se testira Postman-om/curl-om;
  `frameOptions` je isključen da bi H2 konzola radila u iframe-u.
