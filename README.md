# Personal Finance Tracker

Applicazione web per la gestione delle finanze personali, sviluppata con **Spring Boot 3.2**, **Spring Security 6** e **Thymeleaf**. Consente di registrare entrate/uscite, filtrarle per periodo e categoria, visualizzare riepiloghi ed esportare i dati in formato Excel.

---

## Indice

1. [Scopo del progetto](#scopo-del-progetto)
2. [Stack tecnologico](#stack-tecnologico)
3. [Architettura](#architettura)
   - [Pattern MVC](#pattern-mvc)
   - [Struttura dei pacchetti](#struttura-dei-pacchetti)
   - [Flusso di autenticazione](#flusso-di-autenticazione)
   - [Flusso delle transazioni](#flusso-delle-transazioni)
4. [Modello dati](#modello-dati)
5. [Sicurezza](#sicurezza)
6. [Requisiti](#requisiti)
7. [Avvio rapido](#avvio-rapido)
8. [Credenziali predefinite](#credenziali-predefinite)

---

## Scopo del progetto

**Personal Finance Tracker** è un'applicazione web pensata per aiutare utenti privati a tenere traccia delle proprie entrate e uscite. L'obiettivo è fornire uno strumento semplice ma completo per:

- Registrare transazioni con importo, tipo (entrata/uscite), data, descrizione, categoria e metodo di pagamento
- Pianificare transazioni future con ricorrenza settimanale, mensile o annuale
- Filtrare lo storico per intervallo di date e per categoria
- Visualizzare un riepilogo immediato in dashboard con totali entrate, uscite e saldo
- Esportare i dati in Excel con raggruppamento mensile, statistiche per anno e riepilogo complessivo
- Gestire utenti con registrazione, autenticazione e ruoli (USER e ADMIN)

---

## Stack tecnologico

| Tecnologia | Versione | Ruolo |
|---|---|---|
| **Java** | 21 | Linguaggio |
| **Spring Boot** | 3.2.5 | Framework principale |
| **Spring MVC** | 6.1 | Pattern Model-View-Controller |
| **Spring Data JPA** | 3.2 | Accesso dati e repository |
| **Spring Security** | 6.1 | Autenticazione e autorizzazione |
| **Thymeleaf** | 3.1 | Template engine lato server |
| **SQLite** | 3.45.3.0 | Database embedded |
| **Hibernate** | 6.4 | ORM (Object-Relational Mapping) |
| **Apache POI** | 5.2.5 | Generazione file Excel (.xlsx) |
| **Lombok** | 1.18 | Riduzione boilerplate Java |
| **Bootstrap 5** | 5.3.3 | CSS framework (via CDN) |
| **Maven** | - | Build tool e gestione dipendenze |

### Dipendenze Maven (pom.xml)

Le dipendenze principali includono:

- `spring-boot-starter-web` — embedding Tomcat, MVC, REST
- `spring-boot-starter-data-jpa` — JPA + Hibernate
- `spring-boot-starter-security` — autenticazione e controllo accessi
- `spring-boot-starter-thymeleaf` — template engine lato server
- `spring-boot-starter-validation` — validazione Jakarta Bean Validation
- `thymeleaf-extras-springsecurity6` — integrazione Thymeleaf-Security
- `org.xerial:sqlite-jdbc` — driver JDBC per SQLite
- `org.hibernate.orm:hibernate-community-dialects` — dialect Hibernate per SQLite
- `org.apache.poi:poi-ooxml` — generazione file Excel
- `org.projectlombok:lombok` — annotazioni per ridurre codice boilerplate

---

## Architettura

### Pattern MVC

L'applicazione segue il pattern **Model-View-Controller** implementato da Spring MVC:

```
                 Richiesta HTTP
                      |
                      v
              DispatcherServlet
                      |
                      v
            HandlerMapping → Controller
                      |              |
                      v              v
                   Service        Model (DTO/Entity)
                      |              |
                      v              v
                 Repository       View (Thymeleaf)
                      |
                      v
                  Database
                      |
                      v
                Risposta HTML
```

| Livello | Componenti | Responsabilità |
|---|---|---|
| **Controller** | `AuthController`, `DashboardController`, `TransactionController`, `ExportController`, `AdminController` | Riceve le richieste HTTP, valida input, chiama i Service, popola il Model e restituisce la View |
| **Service** | `UserService`, `TransactionService`, `ExportService`, `CustomUserDetailsService` | Logica di business, validazione, orchestrazione delle operazioni |
| **Repository** | `UserRepository`, `TransactionRepository`, `CategoryRepository`, `RoleRepository`, `PaymentMethodRepository` | Interfacciamento con il database tramite Spring Data JPA |
| **Model** | `User`, `Role`, `Transaction`, `Category`, `PaymentMethod`, `TransactionType`, `RecurrenceType` | Entità JPA che mappano le tabelle del database |
| **DTO** | `UserRegistrationDto`, `TransactionFilterDto` | Oggetti per il trasporto dati tra View e Controller |
| **View** | Template Thymeleaf (`.html`) | Renderizzazione lato server con Bootstrap 5 |
| **Config** | `SecurityConfig`, `DataInitializer` | Configurazione sicurezza e dati iniziali |

### Struttura dei pacchetti

```
com.portfolio.financetracker/
├── config/              # Configurazioni (SecurityConfig, DataInitializer)
├── controller/          # Controller MVC (Auth, Dashboard, Transaction, Export, Admin)
├── dto/                 # Data Transfer Objects (UserRegistrationDto, TransactionFilterDto)
├── exception/           # Eccezioni custom e GlobalExceptionHandler
├── model/               # Entità JPA (User, Role, Transaction, Category, PaymentMethod, enums)
├── repository/          # Repository Spring Data JPA
├── service/             # Logica di business (UserService, TransactionService, ExportService, CustomUserDetailsService)
└── FinanceTrackerApplication.java  # Classe main
```

### Flusso di autenticazione

```
1. Utente → GET /login → AuthController → login.html (form)
2. Utente → POST /login (username, password) → Spring Security Filter Chain
3. SecurityContext ← CustomUserDetailsService.loadUserByUsername(email)
   → Recupera utente da DB → verifica password (BCrypt) → crea UserDetails con ruoli
4. Successo → redirect /dashboard
   Fallimento → redirect /login?error
5. Logout → POST /logout → sessione invalidata → redirect /login?logout
```

**Configurazione sicurezza (SecurityConfig.java):**

- `BCryptPasswordEncoder` per l'hashing delle password
- Rotte pubbliche: `/`, `/register`, `/login`, `/css/**`, `/js/**`, `/webjars/**`
- Rotte admin: `/admin/**` richiede ruolo `ROLE_ADMIN`
- Ogni altra rotta richiede autenticazione
- Protezione CSRF attiva su tutti i form POST
- Login form-based con pagina personalizzata

### Flusso delle transazioni

```
1. Utente autenticato → GET /transactions → TransactionController
2. TransactionService.findAll(currentUser, filter)
3. JPA Specification costruita dinamicamente:
   - WHERE user_id = currentUser.id (sempre)
   - AND date >= dateFrom (se specificato)
   - AND date <= dateTo (se specificato)
   - AND category_id = categoryId (se specificato)
4. Risultati ordinati per data DESC
5. TransactionController → model → transactions/list.html (Thymeleaf)
```

**Controllo proprietà:** ogni operazione su una transazione (lettura, modifica, cancellazione) verifica che la transazione appartenga all'utente corrente confrontando `transaction.user.id` con `currentUser.id`. Se non corrispondono, viene lanciata `ResourceNotFoundException`.

---

## Modello dati

### Entity Relationship Diagram (logico)

```
users ────< user_roles >──── roles
  │
  └───< transactions >─── category
              │
              └─── payment_method
```

### Dettaglio entità

**User**
| Campo | Tipo | Note |
|---|---|---|
| id | Long (PK) | Auto-generato |
| username | String (UNIQUE) | È l'email dell'utente |
| password | String (NOT NULL) | Hash BCrypt |
| enabled | Boolean | Default true |
| roles | Set<Role> | ManyToMany EAGER |

**Role**
| Campo | Tipo | Note |
|---|---|---|
| id | Long (PK) | Auto-generato |
| name | String (UNIQUE) | Es. "ROLE_USER", "ROLE_ADMIN" |

**Transaction**
| Campo | Tipo | Note |
|---|---|---|
| id | Long (PK) | Auto-generato |
| amount | BigDecimal (NOT NULL) | Importo |
| type | TransactionType (Enum) | INCOME / EXPENSE |
| date | LocalDate (NOT NULL) | Data dell'operazione |
| description | String (nullable) | Testo libero |
| isPlanned | Boolean | Default false |
| recurrence | RecurrenceType (Enum) | NONE / WEEKLY / MONTHLY / YEARLY |
| plannedDate | LocalDate (nullable) | Data pianificata |
| createdAt | LocalDateTime | Auto-impostato @PrePersist |
| updatedAt | LocalDateTime | Auto-aggiornato @PreUpdate |
| user | User (ManyToOne) | Proprietario della transazione |
| category | Category (ManyToOne) | Categoria (nullable) |
| paymentMethod | PaymentMethod (ManyToOne) | Metodo pagamento (nullable) |

**Category**
| Campo | Tipo | Note |
|---|---|---|
| id | Long (PK) | Auto-generato |
| name | String (NOT NULL) | Es. "Alimentari", "Stipendio" |
| icon | String | Classe icona Bootstrap |

**PaymentMethod**
| Campo | Tipo | Note |
|---|---|---|
| id | Long (PK) | Auto-generato |
| name | String (NOT NULL) | Es. "Carta di Credito", "PayPal" |

---

## Sicurezza

1. **Password**: hash BCrypt prima del salvataggio. Le password non vengono mai salvate in chiaro né restituite in nessuna risposta.
2. **CSRF**: token CSRF di Spring Security incluso in tutti i form tramite `th:action` e campo `_csrf`.
3. **Proprietà dati**: ogni query sulle transazioni filtra sempre per `user_id` dell'utente autenticato. Anche le operazioni di modifica/cancellazione verificano la proprietà.
4. **Ruoli**: le rotte `/admin/**` sono accessibili solo a utenti con `ROLE_ADMIN`. La registrazione assegna automaticamente `ROLE_USER`.
5. **Credenziali admin**: lette da `application.properties` tramite `@Value`, mai hardcoded nel codice.
6. **Sessione**: il logout invalida la sessione HTTP.

---

## Requisiti

- **JDK 21** (o superiore)
- **Apache Maven** 3.9+
- (Opzionale) Eclipse IDE con plugin:
  - **Lombok** (https://projectlombok.org/setup/eclipse)
  - **Spring Tools Suite** (STS)

---

## Avvio rapido

### Da terminale

```bash
# Clona il repository
git clone https://github.com/tuo-username/finance-tracker.git
cd finance-tracker

# Compila e avvia
mvn clean spring-boot:run
```

### Da Eclipse

1. **File → Import → Maven → Existing Maven Projects**
2. Seleziona la cartella del progetto
3. Assicurati che **Lombok sia installato** in Eclipse:
   - Scarica `lombok.jar` da https://projectlombok.org
   - Esegui `java -jar lombok.jar`
   - Seleziona il percorso di Eclipse.exe e installa
4. **Project → Clean** (per sicurezza)
5. Tasto destro sul progetto → **Run As → Maven build...**
6. In `Goals` inserisci: `clean spring-boot:run`
7. Apri il browser su `http://localhost:8081`

---

## Credenziali predefinite

Al primo avvio vengono creati automaticamente:

| Ruolo | Email | Password |
|---|---|---|
| ADMIN + USER | `admin@financetracker.local` | `AdminPassword123!` |

I nuovi utenti si registrano da `/register` e ottengono il ruolo `ROLE_USER`.

---

## Categorie e metodi di pagamento predefiniti

**Categorie:** Alimentari, Stipendio, Svago, Trasporti, Salute, Abbonamenti, Affitto, Bollette, Mutuo/Prestito, Altro

**Metodi di pagamento:** Contanti, Carta di Credito, Carta di Debito, Bonifico SEPA, PayPal

---

## Licenza

Progetto a scopo dimostrativo — realizzato per portfolio personale.
