<?php
// i.php — Secure Image Proxy

// Make sure a file path was provided
if (!isset($_GET['p']) || empty($_GET['p'])) {
    http_response_code(400);
    die("Missing file path.");
}

$requestedPath = $_GET['p'];

// SECURITY: Block attempts to escape folders
if (strpos($requestedPath, '..') !== false || strpos($requestedPath, '//') !== false) {
    http_response_code(403);
    die("Invalid path.");
}

// Build absolute path
$baseDir = __DIR__ . '/'; // Root of your hosting account
$filePath = $baseDir . $requestedPath;

// Does the file exist?
if (!file_exists($filePath)) {
    http_response_code(404);
    die("File not found.");
}

// Detect file type
$imageInfo = @getimagesize($filePath);
if ($imageInfo === false) {
    http_response_code(415); // Unsupported media type
    die("Invalid image file.");
}

// Send correct header (image/jpeg, image/png, etc.)
header("Content-Type: " . $imageInfo['mime']);

// Output image raw data
readfile($filePath);
exit;
?>
