<?php
// Suppress default error output
$oldErrorReporting = error_reporting(E_ALL);
$oldDisplayErrors = ini_set('display_errors', 0);

// Enable mysqli exception mode to catch connection errors
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// Load environment variables from .env file
$envFile = __DIR__ . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        // Skip comments
        if (strpos(trim($line), '#') === 0) {
            continue;
        }
        // Parse KEY=VALUE format
        if (strpos($line, '=') !== false) {
            list($key, $value) = explode('=', $line, 2);
            $key = trim($key);
            $value = trim($value);
            if (!empty($key)) {
                $_ENV[$key] = $value;
                putenv("$key=$value");
            }
        }
    }
}

// Get database credentials from environment variables
$host = getenv('DB_HOST') ?: $_ENV['DB_HOST'] ?? 'localhost';
$dbname = getenv('DB_NAME') ?: $_ENV['DB_NAME'] ?? '';
$username = getenv('DB_USERNAME') ?: $_ENV['DB_USERNAME'] ?? '';
$password = getenv('DB_PASSWORD') ?: $_ENV['DB_PASSWORD'] ?? '';

try {
    // Suppress warnings/errors during connection attempt
    $conn = @new mysqli($host, $username, $password, $dbname);
    
    // Check for connection errors (even with @, we should check)
    if ($conn->connect_error) {
        $errorMsg = $conn->connect_error;
        // Restore error settings before throwing
        error_reporting($oldErrorReporting);
        ini_set('display_errors', $oldDisplayErrors);
        if (strpos($errorMsg, 'max_connections_per_hour') !== false) {
            throw new Exception("Database connection limit exceeded. Please try again later.");
        }
        throw new Exception("Database connection failed: " . $errorMsg);
    }
    
    // Restore error settings on success
    error_reporting($oldErrorReporting);
    ini_set('display_errors', $oldDisplayErrors);
} catch (mysqli_sql_exception $e) {
    // Restore error settings before throwing
    error_reporting($oldErrorReporting);
    ini_set('display_errors', $oldDisplayErrors);
    // Handle connection limit errors specifically
    $errorMsg = $e->getMessage();
    if (strpos($errorMsg, 'max_connections_per_hour') !== false) {
        throw new Exception("Database connection limit exceeded. Please try again later.");
    }
    throw new Exception("Database connection error: " . $errorMsg);
} catch (Exception $e) {
    // Restore error settings before throwing
    error_reporting($oldErrorReporting);
    ini_set('display_errors', $oldDisplayErrors);
    // Re-throw as-is
    throw $e;
} catch (Error $e) {
    // Restore error settings before throwing
    error_reporting($oldErrorReporting);
    ini_set('display_errors', $oldDisplayErrors);
    throw new Exception("Database connection fatal error: " . $e->getMessage());
}
?>
