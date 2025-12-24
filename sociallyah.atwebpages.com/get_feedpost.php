<?php
header('Content-Type: application/json');
require_once "conn.php";

$userId = $_POST['userId'] ?? "";
$limit  = isset($_POST['limit']) ? intval($_POST['limit']) : 50;
$offset = isset($_POST['offset']) ? intval($_POST['offset']) : 0;

if (empty($userId)) {
    echo json_encode([]);
    exit;
}

$BASE_URL = "http://sociallyah.atwebpages.com/i.php?p=";

// Select posts only from people the user is following
$sql = "SELECT p.post_id, p.user_id, p.media_type, p.file_path, p.timestamp,
               u.username, u.dp
        FROM posts p
        LEFT JOIN users u ON u.uid = p.user_id
        WHERE p.user_id IN (
            SELECT following_id
            FROM follows
            WHERE follower_id = ? AND status='accepted'
        )
        ORDER BY p.timestamp DESC
        LIMIT ? OFFSET ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("sii", $userId, $limit, $offset);
$stmt->execute();
$res = $stmt->get_result();

$rows = [];
while ($r = $res->fetch_assoc()) {
    $filePath = $r['file_path'];
    $mediaUrl = $filePath ? $BASE_URL . $filePath : "";
    $dpBase64 = $r['dp'] ?? "";

    $rows[] = [
        "post_id"    => $r['post_id'],
        "user_id"    => $r['user_id'],
        "media_type" => $r['media_type'],
        "media"      => $mediaUrl,
        "timestamp"  => (int)$r['timestamp'],
        "username"   => $r['username'] ?? "",
        "dp"         => $dpBase64
    ];
}

echo json_encode($rows);

$stmt->close();
$conn->close();
?>
