<?php
header('Content-Type: application/json');
require_once "conn.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$userId = $_POST['userId'] ?? "";
$media = $_POST['media'] ?? "";
$type = $_POST['type'] ?? "";

if ($userId === "" || $media === "" || $type === "") {
    echo json_encode(["success" => false, "message" => "Missing fields"]);
    exit;
}

// force server timestamp
$timestamp = time();

// validate media_type
if ($type !== "image" && $type !== "video") {
    echo json_encode(["success" => false, "message" => "Invalid type"]);
    exit;
}

$mediaData = base64_decode($media);
if (!$mediaData) {
    echo json_encode(["success" => false, "message" => "Invalid base64"]);
    exit;
}

// make folder
$uploadDir = __DIR__ . "/uploads/$userId/";
if (!is_dir($uploadDir)) mkdir($uploadDir, 0777, true);

// save file
$extension = ($type === 'video') ? 'mp4' : 'jpg';
$postId = uniqid('p_', true);
$filename = "$postId.$extension";
$fullPath = $uploadDir . $filename;

file_put_contents($fullPath, $mediaData);

// DB path
$filePathForDB = "uploads/$userId/$filename";

// insert
$stmt = $conn->prepare(
   "INSERT INTO posts (post_id, user_id, media_type, file_path, `timestamp`)
     VALUES (?, ?, ?, ?, ?)"
);
$stmt->bind_param("ssssi", $postId, $userId, $type, $filePathForDB, $timestamp);
$stmt->execute();

echo json_encode(["success" => true, "message" => "Inserted"]);
