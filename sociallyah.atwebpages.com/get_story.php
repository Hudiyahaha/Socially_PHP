<?php
header("Content-Type: application/json");

try {
    require_once "conn.php";
} catch (Exception $e) {
    echo json_encode([
        "error" => true,
        "message" => $e->getMessage()
    ]);
    exit;
}

$userId = $_POST["userId"] ?? "";

if (empty($userId)) {
    echo json_encode([]);
    exit;
}

$timeLimit = time() - 86400; // last 24 hours

try {
    $sql = "SELECT s.id, s.userId, s.media, s.type, s.timestamp, u.username, u.dp
            FROM stories s
            JOIN users u ON u.uid = s.userId
            WHERE s.timestamp > ?
            AND (
                s.userId = ? 
                OR s.userId IN (
                    SELECT following_id 
                    FROM follows 
                    WHERE follower_id = ? AND status='accepted'
                )
            )
            ORDER BY s.timestamp DESC";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("iss", $timeLimit, $userId, $userId);

    $stmt->execute();
    $result = $stmt->get_result();

    $stories = [];

    while ($row = $result->fetch_assoc()) {

        // DO NOT add domain — return exactly the relative path
        $mediaPath = $row["media"];
        $dpPath = $row["dp"];

        $stories[] = [
            "id"        => $row['id'],
            "userId"    => $row['userId'],
            "media"     => $mediaPath, // Android will wrap in i.php
            "type"      => $row['type'],
            "timestamp" => (int)$row['timestamp'],
            "username"  => $row['username'],
            "dp"        => $dpPath   // also relative
        ];
    }

    echo json_encode($stories);

    $stmt->close();
    $conn->close();
} catch (Exception $e) {
    echo json_encode([
        "error" => true,
        "message" => "Database error: " . $e->getMessage()
    ]);
    exit;
}
?>
