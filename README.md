# Kubo Property Management System

Kubo is a JavaFX-based boarding house and property management system. It is designed to help manage tenants, rooms, leases, and payments. It has two main sides: an Admin dashboard for the landlord/manager, and a Tenant portal for the renters.

## ✨ Features

### Admin Side
* **Manage Tenants:** Add new tenants or soft-delete them to keep records.
* **Manage Rooms:** Track room capacity, pricing, and see who is currently occupying them.
* **Leases:** See active, expired, and terminated leases.
* **Payments:** Accept and verify tenant payments.
* **Maintenance & Documents:** Check maintenance requests and view uploaded tenant documents.
* **Activity Logs:** Automatically tracks things like payments and lease updates using database triggers.

### Tenant Side
* **Dashboard:** See your current balance, lease details, and announcements.
* **Payments:** Submit payment records to the admin.
* **Maintenance:** Report issues (like a leaky faucet) for the admin to fix.
* **Rooms:** Check out available rooms and prices.

## 🛠️ Built With
* **Frontend:** JavaFX (FXML + CSS)
* **Backend:** Java
* **Database:** MySQL
* **Build Tool:** Maven

## 🚀 How to Run (Zero Setup!)

This project is designed to be "plug-and-play" so you do not need to manually run any SQL scripts to test it. The app handles the database creation automatically on its first run.

### Prerequisites
1. **Java JDK 17+**
2. **MySQL Server** running locally.
3. **Maven**

### 1. Configure the Database Connection
Open the `config.properties` file in the root folder and make sure your MySQL username and password are correct:

```properties
db.host=jdbc:mysql://localhost:3306
db.name=kubo_db
db.user=your_username_here
db.password=your_password_here
```

### 2. Run the App
**Important:** Make sure the `kubo_db` database does **not** exist yet. If you already have an old broken version, drop it in your MySQL console first (`DROP DATABASE kubo_db;`). 

Run the application through your IDE by running the main method in `App.java`, or use Maven in your terminal:

```bash
mvn clean compile
mvn javafx:run
```

On startup, the app will automatically create the `kubo_db` database, build all the tables, and set up the default accounts. 

### 3. Login
Once the app opens, you can log in immediately using the Super Admin account:
* **Username:** `admin`
* **Password:** `admin123`

New tenants can also create their own accounts right from the sign-up screen.
s