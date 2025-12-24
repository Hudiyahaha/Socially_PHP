<?php
// Suppress default error output
$oldErrorReporting = error_reporting(E_ALL);
$oldDisplayErrors = ini_set('display_errors', 0);

// Enable mysqli exception mode to catch connection errors
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$host = 'fdb1031.runhosting.com';        // RunHosting usually uses localhost
$dbname = '4707354_socially';
$username = '4707354_socially';
$password = '12345678ah';

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
