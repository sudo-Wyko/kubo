# Kubo Boarding House Management System

Welcome to the Kubo project! This is a JavaFX application integrated with a MySQL database. Follow the steps below to get your local environment set up exactly like the development build.

# Prerequisites
Before you start, make sure you have the following installed:

    - Java 17 or higher (OpenJDK recommended)

    - Maven (to handle dependencies)

    - MySQL Server (running locally)

    - VS Code with the Extension Pack for Java

# Getting Started
### 1. Clone the Repository

```bash
git clone https://github.com/sudo-Wyko/kubo
cd kubo
git checkout feature/login-screen
```

### 2. Database Setup

You must create the local database so the app has something to talk to.

Open schema.sql inside kubo/database.

Run the commands inside of the sql file in MySQL

### 3. Configure your Credentials

We use a config.properties file to handle local database connections. This file is ignored by Git for security.

Create a config.properties file inside the project root.

Open config.properties and enter your actual MySQL root password:

```properties
    db.url=jdbc:mysql://localhost:3306/kubo_db

    db.user=root

    db.password=YOUR_MYSQL_PASSWORD_HERE
```

# Building and Running

We use the JavaFX Maven plugin. You don't need to manually setup libraries; Maven handles it.

To run the app:
```bash

mvn clean javafx:run

```
Please create a new branch for every feature you work on:
```bash
git checkout -b feature/your-feature-name
```

For example, I did ```bash git checkout -b feature/login-screen``` to work on the login screen.