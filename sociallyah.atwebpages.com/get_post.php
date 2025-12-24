<?php
header('Content-Type: application/json');
require_once "conn.php";

$userId = $_POST['userId'] ?? "";

if (empty($userId)) {
    echo json_encode(["post_count" => 0, "posts" => []]);
    exit;
}

// optional: support pagination
$limit = isset($_POST['limit']) ? intval($_POST['limit']) : 50;
$offset = isset($_POST['offset']) ? intval($_POST['offset']) : 0;

$sql = "SELECT p.post_id, p.user_id, p.media_type, p.file_path, p.timestamp, 
               u.username, u.dp
        FROM posts p
        LEFT JOIN users u ON u.uid = p.user_id
        WHERE p.user_id = ?
        ORDER BY p.timestamp DESC
        LIMIT ? OFFSET ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("sii", $userId, $limit, $offset);
$stmt->execute();
$res = $stmt->get_result();

$rows = [];

while ($r = $res->fetch_assoc()) {
    $filePath = $r['file_path']; // relative path
    $dpPath = $r['dp'];           // Base64 profile picture

    $rows[] = [
        "post_id" => $r['post_id'],
        "user_id" => $r['user_id'],
        "media_type" => $r['media_type'],
        "file_path" => $filePath, // send relative path, not Base64
        "timestamp" => (int)$r['timestamp'],
        "username" => $r['username'] ?? "",
        "dp" => $dpPath ?? ""       // Base64 for profile
    ];
}

// Return both the posts and the total count
echo json_encode([
    "post_count" => count($rows),
    "posts" => $rows
]);

$stmt->close();
$conn->close();
?>
