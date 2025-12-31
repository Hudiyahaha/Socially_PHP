# Security Notice - Secrets Removed

## Summary
This repository previously contained hardcoded secrets that have been removed and replaced with environment variable configuration.

## Secrets That Were Exposed

### 1. Database Credentials (in `sociallyah.atwebpages.com/conn.php`)
- **Host**: `fdb1031.runhosting.com`
- **Database**: `4707354_socially`
- **Username**: `4707354_socially`
- **Password**: `12345678ah`

### 2. Firebase Configuration (in `app/google-services.json`)
- **API Key**: `AIzaSyCQr7Nf4t8cO762KYDMX9Aq68T80-loH2M`
- **Project Number**: `318080792961`
- **Project ID**: `i-0761-23i-0765`
- **Firebase URL**: `https://i-0761-23i-0765-default-rtdb.firebaseio.com`
- **Mobile SDK App ID**: `1:318080792961:android:d6bfcb93f583752b831080`

## Actions Taken

1. ✅ Removed hardcoded database credentials from `conn.php`
2. ✅ Implemented environment variable loading using `.env` file
3. ✅ Replaced Firebase configuration with placeholder values
4. ✅ Added `.env` and `google-services.json` to `.gitignore`
5. ✅ Created example files (`.env.example` and `google-services.json.example`)
6. ✅ Updated README.md with setup instructions

## Required Actions by Repository Owner

⚠️ **CRITICAL**: The exposed secrets are still valid and present in git history. You MUST take the following actions immediately:

### Database Security
1. **Change the database password** for user `4707354_socially`
2. Consider rotating all database credentials
3. Review database access logs for any unauthorized access
4. Update the production `.env` file with new credentials

### Firebase Security
1. **Rotate the Firebase API key** in the Firebase Console
2. Go to Project Settings → General → Web API Key
3. Delete the old key and generate a new one
4. Download a fresh `google-services.json` from Firebase Console
5. Review Firebase usage logs for suspicious activity

### Git History
⚠️ **Note**: The secrets still exist in git history. To completely remove them:

#### Option 1: Using BFG Repo-Cleaner (Recommended)
```bash
# Download BFG Repo-Cleaner
wget https://repo1.maven.org/maven2/com/madgag/bfg/1.14.0/bfg-1.14.0.jar

# Clone a fresh copy of the repo
git clone --mirror https://github.com/Hudiyahaha/Socially_PHP.git

# Remove the secrets from history
java -jar bfg-1.14.0.jar --replace-text passwords.txt Socially_PHP.git

# Clean up and push
cd Socially_PHP.git
git reflog expire --expire=now --all && git gc --prune=now --aggressive
git push --force
```

#### Option 2: Using git-filter-repo
```bash
# Install git-filter-repo
pip3 install git-filter-repo

# Filter the repository
git filter-repo --replace-text passwords.txt

# Force push
git push --force --all
```

#### Option 3: Contact GitHub Support
If the repository is public, consider contacting GitHub support to permanently remove the commits containing secrets from their servers.

## Prevention Measures

The following safeguards are now in place:
- ✅ `.gitignore` updated to exclude `.env` and `google-services.json`
- ✅ Environment variable configuration implemented
- ✅ Example files provided for reference
- ✅ Documentation updated with security best practices

## Additional Recommendations

1. **Enable Git Hooks**: Use pre-commit hooks to scan for secrets
2. **Use Secret Scanning Tools**: Enable GitHub's secret scanning feature
3. **Regular Security Audits**: Periodically review code for hardcoded secrets
4. **Use Secret Management**: Consider using services like HashiCorp Vault or AWS Secrets Manager for production
5. **Implement 2FA**: Enable two-factor authentication on all services

## Contact

If you discover any additional security issues, please report them immediately to the repository owner.

---

**Last Updated**: 2025-12-31
