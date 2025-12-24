<?php
include 'conn.php';
header('Content-Type: text/plain');

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

$postId = trim($_POST['postId'] ?? '');
$userId = trim($_POST['userId'] ?? '');
$comment = trim($_POST['comment'] ?? '');

if(empty($postId) || empty($userId) || empty($comment)) {
    echo "error: missing postId, userId, or comment";
    exit;
}

// Optional: check if post exists
$stmt = $conn->prepare("SELECT id FROM posts WHERE post_id=?");
$stmt->bind_param("s", $postId);
$stmt->execute();
$result = $stmt->get_result();
if($result->num_rows === 0){
    echo "error: post does not exist";
    exit;
}

// Insert comment
$commentId = uniqid("c_", true); // unique comment id
$timestamp = time();
$stmt = $conn->prepare("INSERT INTO post_comments (comment_id, post_id, user_id, text, timestamp) VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("ssssi", $commentId, $postId, $userId, $comment, $timestamp);

if($stmt->execute()){
    echo "success";
} else {
    echo "error: could not insert comment";
}
?>
