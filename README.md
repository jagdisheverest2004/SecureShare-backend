# SecureShare: A Secure File-Sharing Platform

SecureShare is a robust, full-stack application designed to provide a secure and efficient way for users to upload, manage, and share files. The backend is built using **Java with the Spring Boot framework**, ensuring a scalable and enterprise-grade architecture. The platform prioritizes data security through a hybrid encryption model and implements comprehensive authentication and authorization features.

***

## Key Features

* **User Authentication & Authorization**: A secure sign-up and sign-in process is implemented with **password hashing** using BCrypt. User login is a two-step process that includes **email-based OTP (One-Time Password) verification** for enhanced security.
* **Asymmetric Key Management**: Upon registration, each user is assigned a unique **RSA key pair (public and private keys)**. These keys are used to secure the file encryption process and are stored securely in the database.
* **Hybrid File Encryption**: The platform uses a **hybrid cryptographic approach** to handle file security.
    * Large file data (PDFs, images, etc.) is encrypted using a fast **AES-256 GCM** symmetric key.
    * This small AES key is then encrypted using the recipient's **RSA public key**.
    * This method ensures files can be encrypted and shared efficiently without compromising security.
* **Secure File Management**:
    * Users can upload files with custom metadata, including category and sensitivity.
    * Files are encrypted and stored in the database, with a unique ID for each file.
    * Only authorized users can download and decrypt their own files.
* **File Sharing with OTP**: The sharing feature includes an additional security layer for sensitive files. If a file is marked as sensitive, the sender must verify their identity via an OTP sent to their registered email before the file is shared. The file is then re-encrypted for the recipient, ensuring only they can access it.
* **Audit Logging**: The system maintains a detailed **audit log** of all significant user actions, such as file uploads, downloads, and sharing activities. This provides a transparent history of a user's interactions with the platform.
* **Password and Username Recovery**: Users can securely reset their password or retrieve their forgotten username by verifying their registered email.

***

## Tech Stack

* **Backend**: Java, Spring Boot
* **Security**: Spring Security, JSON Web Tokens (JWT), BCrypt, Java Cryptography Architecture (JCA)
* **Database**: MySQL
* **Data Access**: Spring Data JPA, Hibernate
* **Email Service**: JavaMailSender (SMTP)
