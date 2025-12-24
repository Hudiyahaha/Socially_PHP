<?php
include 'conn.php';
header('Content-Type: application/json');

$postId = trim($_POST['postId'] ?? '');

if(empty($postId)){
    echo json_encode(['error' => 'Missing postId']);
    exit;
}

// Fetch comments with username
$sql = "SELECT c.comment_id, c.user_id, c.text, c.timestamp, u.username
        FROM post_comments c
        LEFT JOIN users u ON c.user_id = u.uid
        WHERE c.post_id = ?
        ORDER BY c.timestamp ASC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $postId);
$stmt->execute();
$result = $stmt->get_result();

$comments = [];
while($row = $result->fetch_assoc()){
    $comments[] = [
        'comment_id' => $row['comment_id'],
        'user_id' => $row['user_id'],
        'username' => $row['username'],
        'text' => $row['text'],
        'timestamp' => (int)$row['timestamp']
    ];
}

echo json_encode($comments);
?>
