Socially Assignment 3 with php and MYSQL. hosted on http://sociallyah.atwebpages.com/signup.php

## Setup Instructions

### Database Configuration

1. Copy the `.env.example` file to `.env` in the `sociallyah.atwebpages.com` directory:
   ```bash
   cp sociallyah.atwebpages.com/.env.example sociallyah.atwebpages.com/.env
   ```

2. Edit the `.env` file and add your actual database credentials:
   ```
   DB_HOST=your_database_host
   DB_NAME=your_database_name
   DB_USERNAME=your_database_username
   DB_PASSWORD=your_database_password
   ```

### Firebase Configuration

1. Copy `app/google-services.json.example` to `app/google-services.json`:
   ```bash
   cp app/google-services.json.example app/google-services.json
   ```

2. Download your actual `google-services.json` file from Firebase Console and replace the placeholder file.

### Security Notes

- **NEVER** commit the `.env` file or `google-services.json` file to version control
- These files are already added to `.gitignore` to prevent accidental commits
- Always use the example files as templates
