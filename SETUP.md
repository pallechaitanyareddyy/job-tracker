# Setup guide (Windows, PowerShell)

You need: Java 17, Maven, Node.js, PostgreSQL and Git. All of them are already installed on your machine.

## 1. Create the two databases (once)

`jobtracker` holds your real data. `jobtracker_test` is used only by automated tests, so tests can never damage your real data.

```powershell
psql -U postgres -c "CREATE DATABASE jobtracker;"
psql -U postgres -c "CREATE DATABASE jobtracker_test;"
```

psql asks for the password you set when installing PostgreSQL.
If Windows says `psql` is not recognised, open a new PowerShell window first. Your `psql --version` already worked, so a new window is normally enough.

## 2. Run the backend

The database password is read from an environment variable, so it never ends up in Git. Set it in the same PowerShell window you use to start the app (it is forgotten when you close the window):

```powershell
cd backend
$env:DB_PASSWORD = "your-postgres-password"
mvn spring-boot:run
```

The first run downloads dependencies, so it takes a few minutes. When it works, the log ends with `Started JobTrackerApplication` and Flyway reports that it migrated the schema to version 1.

Check it in a browser:

- http://localhost:8080/api/v1/health shows `{"status":"UP", ...}`
- http://localhost:8080/actuator/health shows `{"status":"UP"}`, which also proves the database connection works

Then open pgAdmin, go to `jobtracker` > Schemas > public > Tables. You should see `users`, `job_applications`, `status_history` and `flyway_schema_history`.

## 3. Run the frontend

Open a second PowerShell window:

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. You should see "Backend status: UP". The frontend does not talk to port 8080 directly; the Vite dev server forwards every `/api` request to it (see `frontend/vite.config.js`).

## 4. Run the tests

```powershell
cd backend
$env:DB_PASSWORD = "your-postgres-password"
mvn test
```

Expected: 2 tests pass. `HealthControllerTest` needs no database. `JobTrackerApplicationTests` starts the whole app against `jobtracker_test`, so it fails if the database or password is wrong.

## 5. Push to GitHub for the first time

Git is the tool on your computer. GitHub is the website that stores a copy. You first save a snapshot locally (a commit), then upload it (a push).

**One-time Git identity** (use the email on your GitHub account):

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

**Create the empty repository on GitHub:** sign in at github.com, click New repository, name it `job-tracker`, choose Public (it is a portfolio project) and do NOT tick "Add a README", .gitignore or licence, because this project already has them.

**Commit and push** from the project's root folder (the one containing `backend` and `frontend`):

```powershell
git init -b main
git add .
git status
```

Read the `git status` list before you continue. It must NOT list `node_modules`, `target`, `dist` or any `.env` file. If it does, stop and tell me. Windows may print "LF will be replaced by CRLF" warnings; they are harmless.

```powershell
git commit -m "Slice 1: project skeleton"
git remote add origin https://github.com/YOUR-USERNAME/job-tracker.git
git push -u origin main
```

Replace `YOUR-USERNAME` with your GitHub username. The first push opens a browser window to sign in to GitHub; approve it.

Refresh the repository page on GitHub. You should see `backend`, `frontend`, `docs` and the README.

## Common problems

| Symptom | Likely cause and fix |
| --- | --- |
| `Could not resolve placeholder 'DB_PASSWORD'` | The environment variable is not set in this PowerShell window. Run the `$env:DB_PASSWORD = ...` line again. |
| `password authentication failed for user "postgres"` | The password is wrong. Use the one you set in the PostgreSQL installer. |
| `database "jobtracker" does not exist` | Step 1 was skipped. |
| `Port 8080 was already in use` | Another program uses 8080. Close it, or start with `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` and change the target port in `frontend/vite.config.js` to match. |
| Frontend shows "Backend not reachable" | The backend is not running, or it is not on port 8080. |
| Flyway error mentioning an unsupported PostgreSQL version | Send me the full message. Your PostgreSQL 19 is newer than the versions the Flyway documentation lists as verified, so this is the first thing to check. |
