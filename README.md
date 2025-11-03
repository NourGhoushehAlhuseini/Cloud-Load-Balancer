Cloud Based File Management and Load Balancing System (JavaFX)

A JavaFX based secure file management system with SQLite and MySQL databases, and a load balancer component.  
Developed as part of COMP20081 – Systems Software 2024/25 at Nottingham Trent University.

Overview
This system provides secure file handling, user authentication, role based access control, and database synchronisation between local and remote storage.  
It was designed to simulate real world cloud file storage with admin and standard user roles.

Key Features

1. User Management  
Login and registration with password hashing and salts.  
Role-based access control (Admin / Standard).  
Implemented in LoginController.java, RegisterController.java, and User.java.

2. Session Management  
Keeps users logged in throughout interactions.  
Implemented in SessionManager.java.

3. File Management  
Create, edit, delete, and view files.  
File metadata stored in SQLite and MySQL.  
Implemented in FileManager.java and FileData.java.

4. Encryption and Security  
Passwords and files securely encrypted.  
Implemented in LoginController.java and FileManager.java.

5. Databases  
SQLite (local) for session data.  
MySQL (remote) for user profiles and file metadata.  
Synchronisation handled in MySQLDB.java and DB.java.

6. Admin Panel  
Manage users, update passwords, and view logs.  
Implemented in AdminController.java and admin.fxml.

7. Logging  
Tracks all user actions and file operations.  
Implemented in LogManager.java.

8. Load Balancer 
LoadBalancer.java manages HTTP requests across containers.  


Interface  
Built using JavaFX with multiple FXML layouts:  
primary.fxml – Login view  
register.fxml – Registration view  
admin.fxml – Admin panel  
secondary.fxml – File management dashboard

Technologies  
Language: Java (JDK 17)  
Framework: JavaFX  
Databases: SQLite and MySQL  
Tools: Docker, JDBC, Maven, NetBeans  
Security: Hashed passwords with salts

To run the code, clone the repository: git clone https://github.com/NourGhoushehAlhuseini/Cloud-Load-Balancer.git
