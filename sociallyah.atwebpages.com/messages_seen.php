<?php
include "conn.php";

// Prevent caching
header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$chat = $_POST['chat_id'] ?? '';
$user = $_POST['user_id'] ?? '';

if (empty($chat) || empty($user)) {
    echo json_encode(["status" => 0, "message" => "Missing required parameters"]);
    exit;
}

// FIX: Update 'seen' to 1 for messages received by this user
// (Previously incorrectly updated 'deleted' field)
$stmt = $conn->prepare("
    UPDATE messages SET seen = 1 
    WHERE chat_id = ? 
    AND receiver_id = ?
    AND seen = 0
");
$stmt->bind_param("ss", $chat, $user);
$stmt->execute();

$updated = $stmt->affected_rows;

echo json_encode(["status" => 1, "updated" => $updated]);

$stmt->close();
$conn->close();
?>
