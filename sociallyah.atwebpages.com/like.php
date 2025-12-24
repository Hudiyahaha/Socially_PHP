<?php
include 'conn.php';
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 1);

$postId = trim($_POST['postId'] ?? '');
$userId = trim($_POST['userId'] ?? '');

if(empty($postId) || empty($userId)){
    echo json_encode(['error' => 'Missing postId or userId']);
    exit;
}

// Check if post exists (use correct column, probably 'id')
$stmt = $conn->prepare("SELECT id FROM posts WHERE post_id=?");
$stmt->bind_param("s", $postId);
$stmt->execute();
$result = $stmt->get_result();
if($result->num_rows === 0){
    echo json_encode(['error' => 'Post does not exist']);
    exit;
}

// Check if user exists
$stmt = $conn->prepare("SELECT uid FROM users WHERE uid=?");
$stmt->bind_param("s", $userId);
$stmt->execute();
$result = $stmt->get_result();
if($result->num_rows === 0){
    echo json_encode(['error' => 'User does not exist']);
    exit;
}

// Toggle like
$stmt = $conn->prepare("SELECT * FROM post_likes WHERE post_id=? AND user_id=?");
$stmt->bind_param("ss", $postId, $userId);
$stmt->execute();
$result = $stmt->get_result();

if($result->num_rows > 0){
    // Unlike
    $stmt = $conn->prepare("DELETE FROM post_likes WHERE post_id=? AND user_id=?");
    $stmt->bind_param("ss", $postId, $userId);
    $stmt->execute();
    $status = 'unliked';
} else {
    // Like
    $stmt = $conn->prepare("INSERT INTO post_likes (post_id,user_id) VALUES (?, ?)");
    $stmt->bind_param("ss", $postId, $userId);
    $stmt->execute();
    $status = 'liked';
}

// Return updated like count
$stmt = $conn->prepare("SELECT COUNT(*) AS likeCount FROM post_likes WHERE post_id=?");
$stmt->bind_param("s", $postId);
$stmt->execute();
$result = $stmt->get_result();
$likeCount = (int)$result->fetch_assoc()['likeCount'];

// Send JSON response
echo json_encode([
    'status' => $status,
    'likeCount' => $likeCount
]);
?>
