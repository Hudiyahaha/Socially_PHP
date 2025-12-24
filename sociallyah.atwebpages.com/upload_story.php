<?php
header("Content-Type: application/json");
require_once "conn.php"; // your MySQL connection

$userId = $_POST["userId"] ?? "";
$mediaBase64 = $_POST["media"] ?? "";
$type = $_POST["type"] ?? ""; // "image" or "video"
$timestamp = time(); // current UNIX timestamp

// Validate input
if (empty($userId) || empty($mediaBase64) || empty($type)) {
    echo json_encode(["status" => 0, "message" => "Missing fields"]);
    exit;
}

// Create user folder if not exists
$folder = "stories/$userId";
if (!file_exists($folder)) {
    mkdir($folder, 0777, true);
}

// Determine file extension
$ext = ($type === "video") ? ".mp4" : ".jpg";

// Final file path
$filename = "$folder/$timestamp$ext";

// Decode base64 media
$mediaData = base64_decode($mediaBase64);
if ($mediaData === false) {
    echo json_encode(["status" => 0, "message" => "Invalid Base64"]);
    exit;
}

// Save file to disk
if (file_put_contents($filename, $mediaData) === false) {
    echo json_encode(["status" => 0, "message" => "Failed to save file"]);
    exit;
}

// Insert story record in DB
$stmt = $conn->prepare(
    "INSERT INTO stories (userId, media, type, timestamp) VALUES (?, ?, ?, ?)"
);
$stmt->bind_param("sssi", $userId, $filename, $type, $timestamp);

if ($stmt->execute()) {
    echo json_encode(["status" => 1, "message" => "Story uploaded"]);
} else {
    echo json_encode(["status" => 0, "message" => "Database error: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>
