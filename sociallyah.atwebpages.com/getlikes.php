<?php
include 'conn.php';
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 1);

$userId = trim($_POST['userId'] ?? '');
$postIds = $_POST['postIds'] ?? [];

if(empty($userId) || !is_array($postIds) || count($postIds) === 0){
    echo json_encode(['error' => 'Missing userId or postIds']);
    exit;
}

// Prepare placeholders for IN clause
$placeholders = implode(',', array_fill(0, count($postIds), '?'));
$types = str_repeat('s', count($postIds));

// Fetch likes count for the given posts
$sql = "SELECT p.post_id, 
               COUNT(pl.user_id) AS likeCount, 
               SUM(pl.user_id = ?) AS likedByUser
        FROM posts p
        LEFT JOIN post_likes pl ON p.post_id = pl.post_id
        WHERE p.post_id IN ($placeholders)
        GROUP BY p.post_id";

$stmt = $conn->prepare($sql);
$params = array_merge([$userId], $postIds);

// Bind parameters dynamically
$stmt->bind_param(str_repeat('s', count($params)), ...$params);
$stmt->execute();
$result = $stmt->get_result();

$likesData = [];
while($row = $result->fetch_assoc()){
    $likesData[] = [
        'postId' => $row['post_id'],
        'likeCount' => (int)$row['likeCount'],
        'likedByUser' => (bool)$row['likedByUser']
    ];
}

echo json_encode($likesData);
?>
